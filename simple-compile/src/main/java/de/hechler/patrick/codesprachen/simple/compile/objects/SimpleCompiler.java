package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.IllegalSelectorException;
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
import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandAssign;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandFuncCall;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandIf;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandWhile;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypeArray;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePrimitive;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;
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
	
	private static final int X00                   = PrimAsmConstants.X_ADD;
	private static final int X01                   = PrimAsmConstants.X_ADD + 1;
	private static final int X02                   = PrimAsmConstants.X_ADD + 2;
	private static final int X03                   = PrimAsmConstants.X_ADD + 3;
	private static final int MAX_INT_REG           = X03;
	private static final int METHOD_STRUCT_REG     = X00 + 4;
	private static final int VARIABLE_POINTER_REG  = X00 + 5;
	private static final int MIN_REG_VARIABLE_REG  = X00 + 6;
	private static final int MAX_REG_VARIABLE_REG  = X00 + 0x9F;
	/**
	 * the compiler won't use any registers above this value for critical stuff
	 */
	public static final int  MAX_COMPILER_REGISTER = MAX_REG_VARIABLE_REG;
	
	private static class UsedData {
		
		int  regs        = X00 + 5;
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
			initVariableMemory(target, sf);
			sf.addrVars = true;
		} else {
			sf.addrVars = false;
		}
		sf.regVars = used.maxregs;
		for (SimpleCommand cmd : sf.body.cmds) {
			compileCommand(target, sf, regs, cmd);
		}
		if (used.maxaddr > 0L) {
			Param p1;
			ParamBuilder b = new ParamBuilder();
			b.art = A_NUM;
			b.v1 = PrimAsmPreDefines.INT_MEMORY_FREE;
			p1 = b.build();
			Command c = new Command(Commands.CMD_INT, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		Command c = new Command(Commands.CMD_RET, null, null);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	private void initVariableMemory(CompileTarget target, SimpleFunction sf) {
		Param p1, p2;
		ParamBuilder b = new ParamBuilder();
		b.art = A_NUM;
		b.v1 = PrimAsmPreDefines.INT_MEMORY_ALLOC;
		p1 = b.build();
		Command c = new Command(Commands.CMD_INT, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		b.v1 = -1L;
		p2 = b.build();
		b.art = A_SR;
		b.v1 = X00;
		p1 = b.build();
		c = new Command(Commands.CMD_CMP, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		b.art = A_NUM;
		assert target.outOfMemAddr != -1L;
		b.v1 = target.pos - target.outOfMemAddr;
		p1 = b.build();
		c = new Command(Commands.CMD_JMP, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		b.art = A_SR;
		b.v1 = VARIABLE_POINTER_REG;
		p1 = b.build();
		b.v1 = X00;
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
			compileFuncCall(target, sf, regs, funcCallCmd);
		} else if (cmd instanceof SimpleCommandAssign) {
			SimpleCommandAssign assignCmd = (SimpleCommandAssign) cmd;
			compileAssignCommand(target, sf, regs, assignCmd);
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
			assignVariable(target, sf, regs, varDeclCmd, varDeclCmd.init);
		} else {
			throw new InternalError("unknown command type: " + cmd.getClass().getName() + " (of command: '" + cmd + "')");
		}
	}
	
	private void compileFuncCall(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandFuncCall funcCallCmd) {
		validateFuncCall(funcCallCmd);
		push(target, sf);
		call(target, sf, funcCallCmd, regs);
		pop(target, sf);
	}
	
	private void validateFuncCall(SimpleCommandFuncCall funcCallCmd) {
		SimpleFunction func;
		if (funcCallCmd.secondName == null) {
			func = funcCallCmd.pool.getFunction(funcCallCmd.firstName);
		} else {
			SimpleDependency dep = funcCallCmd.pool.getDependency(funcCallCmd.firstName);
			SimpleExportable se = dep.imps.get(funcCallCmd.secondName);
			if (se == null || ! (se instanceof SimpleFunction)) {
				throw new IllegalStateException("function call needs a function! dependency: " + dep.path + " > '" + dep.depend + "' (not) function name: " + funcCallCmd.secondName + " > " + se);
			}
			func = (SimpleFunction) se;
			assert dep.path.addr != -1L;
			assert func.address != -1L;
		}
		if ( !func.type.equals(funcCallCmd.function.type())) {
			throw new IllegalStateException(
				"the function call structure is diffrent to the type needed by the called function! (function: " + (funcCallCmd.firstName) + (funcCallCmd.secondName == null ? "" : (":" + funcCallCmd.secondName))
					+ " given func-struct type: '" + funcCallCmd.function.type() + "' needded func-struct type: '" + func.type + "' given func-struct: '" + funcCallCmd.function + "')");
		}
	}
	
	private void compileAssignCommand(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandAssign assignCmd) {
		if (assignCmd.target.type().isPrimitive())
			if (assignCmd.target instanceof SimpleVariableValue) {
				assignVariable(target, sf, regs, ((SimpleVariableValue) assignCmd.target).sv, assignCmd.value);
			} else {
				int r1 = register(regs, MAX_COMPILER_REGISTER + 1);
				boolean o1 = makeRegUsable(regs, r1);
				SimpleValue nt = assignCmd.target.addExpUnary(assignCmd.pool, SimpleValue.EXP_UNARY_AND);
				target.pos = nt.loadValue(r1, regs, sf.cmds, target.pos);
				int r2 = register(regs, MAX_COMPILER_REGISTER + 2);
				boolean o2 = makeRegUsable(regs, r2);
				target.pos = assignCmd.value.loadValue(r2, regs, sf.cmds, target.pos);
				Param p1, p2;
				ParamBuilder b = new ParamBuilder();
				b.art = A_SR | B_REG;
				b.v1 = r1;
				p1 = b.build();
				b.art = A_SR;
				b.v1 = r2;
				p2 = b.build();
				Command c = new Command(Commands.CMD_MOV, p1, p2);
				target.pos += c.length();
				sf.cmds.add(c);
				regs[r2] = o2;
				regs[r1] = o1;
			}
	}
	
	private void call(CompileTarget target, SimpleFunction sf, SimpleCommandFuncCall funcCallCmd, boolean[] regs) {
		assert regs[METHOD_STRUCT_REG];
		regs[METHOD_STRUCT_REG] = false;
		target.pos = funcCallCmd.function.loadValue(METHOD_STRUCT_REG, regs, sf.cmds, target.pos);
		regs[METHOD_STRUCT_REG] = true;
		if (funcCallCmd.secondName == null) {
			Param p1;
			Command c;
			p1 = Param.createLabel(funcCallCmd.firstName);
			c = new Command(Commands.CMD_CALL, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		} else {
			SimpleDependency dep = funcCallCmd.pool.getDependency(funcCallCmd.firstName);
			SimpleFunction func = (SimpleFunction) dep.imps.get(funcCallCmd.secondName);
			Param p1, p2;
			ParamBuilder b = new ParamBuilder();
			b.art = A_SR;
			b.v1 = X00;
			p1 = b.build();
			b.art = A_NUM;
			b.v1 = dep.path.addr - target.pos;
			p2 = b.build();
			Command c = new Command(Commands.CMD_LEA, p1, p2);
			target.pos += c.length();
			sf.cmds.add(c);
			b.v1 = PrimAsmPreDefines.INT_GET_FILE;
			p1 = b.build();
			c = new Command(Commands.CMD_INT, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
			// TODO
		}
	}
	
	private void pop(CompileTarget target, SimpleFunction sf) {
		Param p1;
		ParamBuilder b = new ParamBuilder();
		b.art = A_SR;
		Command c;
		for (int i = sf.regVars - 1; i >= 0; i -- ) {
			b.v1 = MIN_REG_VARIABLE_REG + i;
			p1 = b.build();
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		if (sf.addrVars) {
			b.v1 = VARIABLE_POINTER_REG;
			p1 = b.build();
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		b.v1 = METHOD_STRUCT_REG;
		p1 = b.build();
		c = new Command(Commands.CMD_PUSH, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	private void push(CompileTarget target, SimpleFunction sf) {
		Param p1;
		ParamBuilder b = new ParamBuilder();
		b.art = A_SR;
		Command c;
		b.v1 = METHOD_STRUCT_REG;
		p1 = b.build();
		c = new Command(Commands.CMD_PUSH, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		if (sf.addrVars) {
			b.v1 = VARIABLE_POINTER_REG;
			p1 = b.build();
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		for (int i = 0; i < sf.regVars; i ++ ) {
			b.v1 = MIN_REG_VARIABLE_REG + i;
			p1 = b.build();
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
	}
	
	private void assignVariable(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleVariable vari, SimpleValue value) {
		assert vari.reg != -1;
		assert vari.type.isPrimitive() || vari.type.isPointer();
		if (vari.addr != -1L) {
			int reg = register(regs, MAX_COMPILER_REGISTER + 1);
			boolean old = makeRegUsable(regs, reg);
			target.pos = value.loadValue(reg, regs, sf.cmds, target.pos);
			Param p1, p2;
			ParamBuilder b = new ParamBuilder();
			b.art = A_SR;
			b.v1 = reg;
			p2 = b.build();
			b.art = A_SR | B_NUM;
			b.v1 = vari.reg;
			b.v2 = vari.addr;
			p1 = b.build();
			Commands mov;
			switch (vari.type.byteCount()) {
			case 8:
				mov = Commands.CMD_MOV;
				break;
			case 4:
				mov = Commands.CMD_MVDW;
				break;
			case 2:
				mov = Commands.CMD_MVW;
				break;
			case 1:
				mov = Commands.CMD_MVB;
				break;
			default:
				throw new InternalError("unknown byte count of variable!");
			}
			Command c = new Command(mov, p1, p2);
			target.pos += c.length();
			sf.cmds.add(c);
			regs[reg] = old;
		} else {
			regs[vari.reg] = false;
			target.pos = value.loadValue(vari.reg, regs, sf.cmds, target.pos);
			assert regs[vari.reg];
		}
	}
	
	private boolean makeRegUsable(boolean[] regs, int reg) {
		boolean old = regs[reg];
		regs[reg] = false;
		return old;
	}
	
	private int register(boolean[] regs, int fallback) {
		for (int i = fallback; i < 256; i ++ ) {
			if ( !regs[i]) return i;
		}
		for (int i = fallback - 1; i > 0; i -- ) {
			if ( !regs[i]) return i;
		}
		return fallback;
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

