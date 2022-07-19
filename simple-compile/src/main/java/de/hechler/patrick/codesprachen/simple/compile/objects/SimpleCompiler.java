package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;
import de.hechler.patrick.codesprachen.primitive.core.objects.PrimitiveConstant;
import de.hechler.patrick.codesprachen.primitive.core.utils.Convert;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandAssign;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandFuncCall;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandIf;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandWhile;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleVariableValue;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class SimpleCompiler {
	
	private static final long NO_LOCK = PatrFileSysConstants.NO_LOCK;
	
	private final Path    srcRoot;
	private final Path[]  lockups;
	private final Charset cs;
	
	private final Map <String, CompileTarget> targets = new HashMap <>();
	private final Queue <CompileTarget>       prework = new ConcurrentLinkedQueue <>();
	
	private volatile boolean working;
	private volatile boolean stop;
	
	public SimpleCompiler(Path srcRoot, Path[] lockups, Charset cs) {
		this.srcRoot = srcRoot.normalize();
		this.lockups = new Path[lockups.length];
		for (int i = 0; i < lockups.length; i ++ ) {
			this.lockups[i] = lockups[i].normalize();
		}
		this.cs = cs;
	}
	
	public synchronized void addFile(PatrFile outFile, Path currentFile, Path exports, boolean neverExecutable) {
		assert !stop;
		CompileTarget target = new CompileTarget(currentFile, exports, outFile, neverExecutable);
		String relPath = srcRoot.relativize(currentFile.normalize()).toString();
		CompileTarget old = targets.put(relPath, target);
		prework.add(target);
		if ( !working) {
			working = true;
			Thread worker = new Thread(() -> {
				try {
					precompileQueue();
				} finally {
					working = false;
					synchronized (SimpleCompiler.this) {
						notify();
					}
				}
			}, "simple precompiler");
			worker.setPriority(Math.min(worker.getPriority() + 2, Thread.MAX_PRIORITY));
			worker.start();
		}
		assert old == null;
	}
	
	private void precompileQueue() {
		while (true) {
			CompileTarget pc = prework.poll();
			if (pc == null) return;
			try {
				try (Reader r = Files.newBufferedReader(pc.source, cs)) {
					ANTLRInputStream in = new ANTLRInputStream();
					in.load(r, 1024, 1024);
					SimpleGrammarLexer lexer = new SimpleGrammarLexer(in);
					CommonTokenStream toks = new CommonTokenStream(lexer);
					SimpleGrammarParser parser = new SimpleGrammarParser(toks);
					pc.file = new SimpleFile(lockups, cs);
					parser.simpleFile(pc.file);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void compile() {
		synchronized (this) {
			stop = true;
			while (working) {
				try {
					wait(1000L);
				} catch (InterruptedException e) {
					System.err.println("unexpected interrupt!");
					e.printStackTrace();
				}
			}
		}
		precompileQueue();
		for (CompileTarget target : targets.values()) {
			try {
				target.binary.withLock(() -> compile(target));
			} catch (IOException e) {
				throw new RuntimeException(e.toString(), e);
			}
		}
	}
	
	private void compile(CompileTarget target) throws IOException {
		assert target.pos == -1L;
		SimpleFunction main = null;
		target.pos = 0L;
		target.expout = Files.newBufferedWriter(target.exports, cs);
		if ( !target.neverExe) {
			main = target.file.mainFunction();
			if (main != null) {
				executableStart(target);
				fillData(target);
				correctMainAddress(target);
				compileFunction(target, main);
			}
		} else {
			fillData(target);
		}
		for (SimpleFunction sf : target.file.functions()) {
			if (sf == main) continue;
			compileFunction(target, sf);
		}
	}
	
	// TODO make these files
	private static final String MY_EXPORT_FILE                      = "/de/hechler/patrick/codesprachen/simple/compile/mySymbols.psf";
	private static final String EXECUTABLE_START_FILE               = "/de/hechler/patrick/codesprachen/simple/compile/executableStart.pmc";
	private static final String MAIN_ADDRESS_EXPORT_SYMBOL          = "MAIN_ADDRESS";
	private static final String MAIN_ADDRESS_RELATIVE_EXPORT_SYMBOL = "MAIN_ADDRESS_REL_POS";
	private static final long   MAIN_ADDRESS;
	private static final long   MAIN_ADDRESS_RELATIVE_POSITION;
	
	static {
		try (InputStream in = SimpleCompiler.class.getResourceAsStream(MY_EXPORT_FILE)) {
			try (Scanner sc = new Scanner(in, "UTF-8")) {
				Map <String, PrimitiveConstant> map = new HashMap <>();
				PrimitiveAssembler.readSymbols(null, map, sc, Paths.get(MY_EXPORT_FILE));
				PrimitiveConstant pc = map.get(MAIN_ADDRESS_EXPORT_SYMBOL);
				MAIN_ADDRESS = pc.value;
				pc = map.get(MAIN_ADDRESS_RELATIVE_EXPORT_SYMBOL);
				MAIN_ADDRESS_RELATIVE_POSITION = pc.value;
			}
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	private void correctMainAddress(CompileTarget target) throws IOException {
		byte[] bytes = new byte[8];
		Convert.convertLongToByteArr(bytes, 0, target.pos - MAIN_ADDRESS_RELATIVE_POSITION);
		target.binary.setContent(bytes, MAIN_ADDRESS, 0, 8, NO_LOCK);
	}
	
	private void fillData(CompileTarget target) throws IOException {
		for (SimpleValueDataPointer dataVal : target.file.dataValues()) {
			dataVal.addr = target.pos;
			target.binary.appendContent(dataVal.data, 0, dataVal.data.length, NO_LOCK);
			target.pos += dataVal.data.length;
			if ( (target.pos & 7) != 0) {
				byte[] bytes = new byte[(int) (8 - (target.pos & 7))];
				target.binary.appendContent(bytes, 0, bytes.length, NO_LOCK);
				target.pos += bytes.length;
			}
		}
		assert target.pos == target.binary.length(NO_LOCK);
	}
	
	private void executableStart(CompileTarget target) throws IOException {
		assert target.pos == 0L;
		try (InputStream in = getClass().getResourceAsStream(EXECUTABLE_START_FILE)) {
			try (OutputStream out = target.binary.openOutput(true, NO_LOCK)) {
				byte[] buf = new byte[512];
				while (true) {
					int r = in.read(buf, 0, 512);
					if (r == -1) break;
					out.write(buf, 0, r);
				}
			}
		}
	}
	
	private static final long MY_INT_ERROR = PrimAsmPreDefines.INTERRUPT_COUNT;
	
	private static final int MAX_INT_REG           = PrimAsmConstants.X_ADD + 3;
	private static final int METHOD_STRUCT_REG     = PrimAsmConstants.X_ADD + 4;
	private static final int VARIABLE_POINTER_REG  = PrimAsmConstants.X_ADD + 5;
	private static final int MIN_REG_VARIABLE_REG  = PrimAsmConstants.X_ADD + 6;
	private static final int MAX_REG_VARIABLE_REG  = PrimAsmConstants.X_ADD + 0x9F;
	/**
	 * the compiler won't use any registers above this value for critical stuff
	 */
	public static final int  MAX_COMPILER_REGISTER = MAX_REG_VARIABLE_REG;
	
	private static class UsedData {
		
		int  regs        = PrimAsmConstants.X_ADD + 5;
		int  maxregs     = -1;
		long currentaddr = 0L;
		long maxaddr     = -1L;
		
	}
	
	private void compileFunction(CompileTarget target, SimpleFunction sf) throws IOException {
		sf.address = target.pos;
		if (sf.export && target.expout != null) {
			target.expout.append(sf.toExportString()).append('\n');
		}
		sf.cmds = new LinkedList <>();
		SimplePool pool = sf.body.pool;
		boolean[] regs = new boolean[256];
		// X00..X03 is for interrupts
		// X04 is current method struct
		// X05 is the backup pointer for variables if there is nowhere else place
		// X06..X9F is for variables
		// XA0..XF9 is used by expressions and similar
		UsedData used = new UsedData();
		count(used, regs, sf.body);
		for (int i = 0; i < used.regs; i ++ ) {
			regs[i] = true;
		}
		if (used.maxaddr > 0L) {
			Param p1, p2;
			ParamBuilder b = new ParamBuilder();
			b.art = A_NUM;
			b.v1 = PrimAsmPreDefines.INT_MEMORY_ALLOC;
			p1 = b.build();
			Command c1 = new Command(Commands.CMD_INT, p1, null);
			target.pos += c1.length();
			sf.cmds.add(c1);
			Command c2;
		}
		for (SimpleCommand cmd : sf.body.cmds) {
			compileCommand(target, sf, regs, cmd);
		}
	}
	
	private void compileCommand(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommand cmd) throws InternalError {
		if (cmd instanceof SimpleCommandIf) {
			SimpleCommandIf ifCmd = (SimpleCommandIf) cmd;
			
			// TODO
		} else if (cmd instanceof SimpleCommandWhile) {
			SimpleCommandWhile whileCmd = (SimpleCommandWhile) cmd;
			
			// TODO
		} else if (cmd instanceof SimpleCommandFuncCall) {
			SimpleCommandFuncCall funcCallCmd = (SimpleCommandFuncCall) cmd;
			
			// TODO
		} else if (cmd instanceof SimpleCommandAssign) {
			SimpleCommandAssign assignCmd = (SimpleCommandAssign) cmd;
			if (assignCmd.target instanceof SimpleVariableValue) {
				
			} else {
				
			}
			// TODO
		} else if (cmd instanceof SimpleCommandBlock) {
			SimpleCommandBlock blockCmd = (SimpleCommandBlock) cmd;
			for (SimpleCommand childCmd : blockCmd.cmds) {
				compileCommand(target, sf, regs, childCmd);
			}
		} else if (cmd instanceof SimpleCommandVarDecl) {
			SimpleCommandVarDecl varDeclCmd = (SimpleCommandVarDecl) cmd;
			if (varDeclCmd.init == null) {
				return;
			}
			if (varDeclCmd.addr != -1L) {
				int reg = register(regs);
				boolean old = makeRegUsable(regs, reg);
				target.pos = varDeclCmd.init.loadValue(reg, regs, sf.cmds, target.pos);
				Param p1, p2;
				ParamBuilder b = new ParamBuilder();
				b.art = A_SR;
				b.v1 = reg;
				p2 = b.build();
				b.art = A_SR | B_NUM;
				b.v1 = varDeclCmd.reg;
				b.v2 = varDeclCmd.addr;
				p1 = b.build();
				Command c = new Command(Commands.CMD_MOV, p1, p2);
				target.pos += c.length();
				sf.cmds.add(c);
				regs[reg] = old;
			} else {
				regs[varDeclCmd.reg] = false;
				target.pos = varDeclCmd.init.loadValue(varDeclCmd.reg, regs, sf.cmds, target.pos);
				assert regs[varDeclCmd.reg];
			}
		} else {
			throw new InternalError("unknown command type: " + cmd.getClass().getName() + " (of command: '" + cmd + "')");
		}
	}
	
	private boolean makeRegUsable(boolean[] regs, int reg) {
		boolean old = regs[reg];
		regs[reg] = false;
		return old;
	}
	
	private int register(boolean[] regs) {
		for (int i = MAX_COMPILER_REGISTER + 1; i < 256; i ++ ) {
			if ( !regs[i]) return i;
		}
		for (int i = MAX_COMPILER_REGISTER; i > 0; i -- ) {
			if ( !regs[i]) return i;
		}
		return MAX_COMPILER_REGISTER + 1;
	}
	
	boolean alignMemory = true;
	
	private void count(UsedData used, boolean[] regs, SimpleCommandBlock body) {
		final int startRegs = used.regs;
		final long startAddr = used.currentaddr;
		for (SimpleCommand cmd : body.cmds) {
			if (cmd instanceof SimpleCommandBlock) {
				count(used, regs, (SimpleCommandBlock) cmd);
			} else if (cmd instanceof SimpleCommandVarDecl) {
				SimpleCommandVarDecl vd = (SimpleCommandVarDecl) cmd;
				if ( (vd.type.isPrimitive() || vd.type.isPointer()) && used.regs < MAX_COMPILER_REGISTER) {
					vd.addr = -used.regs;
					used.regs ++ ;
				} else {
					int bc = vd.type.byteCount();
					if (alignMemory) {
						int high = 8;
						if (Integer.bitCount(bc) == 1) {
							high = Math.max(8, bc);
						}
						int and = high - 1;
						if ( (used.currentaddr & and) != 0) {
							used.currentaddr += high - (used.currentaddr & and);
						}
					}
					vd.addr = used.currentaddr;
					used.currentaddr += bc;
				}
			}
		}
		if (used.currentaddr > used.maxaddr) {
			used.maxaddr = used.currentaddr;
		}
		if (used.regs > used.maxregs) {
			used.maxregs = used.regs;
		}
		used.regs = startRegs;
		used.currentaddr = startAddr;
	}
	
	private static class CompileTarget {
		
		private final Path     source;
		private final Path     exports;
		private final PatrFile binary;
		private final boolean  neverExe;
		
		private volatile SimpleFile file;
		
		private long   outOfMemAddr = -1L;
		private long   pos          = -1L;
		private Writer expout       = null;
		
		public CompileTarget(Path src, Path exp, PatrFile bin, boolean neverExe) {
			this.source = src;
			this.exports = exp;
			this.binary = bin;
			this.neverExe = neverExe;
		}
		
	}
	
}
