package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_REG;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines.INT_EXIT;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines.INT_GET_FILE;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines.INT_MEMORY_ALLOC;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines.INT_MEMORY_FREE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
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
	
	private static final long MY_INT_OUT_OF_MEM_ERROR = PrimAsmPreDefines.INTERRUPT_COUNT;
	private static final long MY_INT_DEP_LOAD_ERROR   = MY_INT_OUT_OF_MEM_ERROR + 1L;
	private static final long MY_INT_CNT              = MY_INT_DEP_LOAD_ERROR + 1L;
	
	// TODO make these files
	private static final String MY_EXPORT_FILE                      = "/de/hechler/patrick/codesprachen/simple/compile/mySymbols.psf";
	private static final String EXECUTABLE_START_FILE               = "/de/hechler/patrick/codesprachen/simple/compile/executableStart.pmc";
	private static final String MAIN_ADDRESS_EXPORT_SYMBOL          = "MAIN_ADDRESS";
	private static final String MAIN_ADDRESS_RELATIVE_EXPORT_SYMBOL = "MAIN_ADDRESS_REL_POS";
	private static final String INTERRUPT_COUNT_SYMBOL              = "INTERRUPT_COUNT";
	private static final String INT_OUT_OF_MEM_ERR_SYMBOL           = "INT_OUT_OF_MEM_ERR";
	private static final String INT_DEP_LOAD_ERR_SYMBOL             = "INT_DEP_LOAD_ERR";
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
				pc = map.get(INTERRUPT_COUNT_SYMBOL);
				if (pc.value != MY_INT_CNT) {
					throw new AssertionError();
				}
				pc = map.get(INT_OUT_OF_MEM_ERR_SYMBOL);
				if (pc.value != MY_INT_OUT_OF_MEM_ERROR) {
					throw new AssertionError();
				}
				pc = map.get(INT_DEP_LOAD_ERR_SYMBOL);
				if (pc.value != MY_INT_DEP_LOAD_ERROR) {
					throw new AssertionError();
				}
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
		assert target.pos == target.binary.length(NO_LOCK);
		target.outOfMemErrorAddr = target.pos;
		try (OutputStream out = target.binary.openOutput(true, NO_LOCK)) {
			PrimitiveAssembler asm = new PrimitiveAssembler(out, null, new Path[0], true, false);
			Command outOfMem = new Command(Commands.CMD_INT, build(A_NUM, MY_INT_OUT_OF_MEM_ERROR), null);
			Command mov1 = new Command(Commands.CMD_MOV, build(A_SR, X00), build(A_NUM, 1L));
			Command iex = new Command(Commands.CMD_INT, build(A_NUM, INT_EXIT), null);
			Command depLoad = new Command(Commands.CMD_INT, build(A_NUM, MY_INT_DEP_LOAD_ERROR), null);
			long exitLen = mov1.length() + iex.length();
			target.pos += outOfMem.length() + exitLen;
			target.dependencyLoadErrorAddr = target.pos;
			target.pos += depLoad.length() + exitLen;
			asm.assemble(Arrays.asList(outOfMem, mov1, iex, depLoad, mov1, iex), Collections.emptyMap());
		}
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
	
	private static final int X00 = PrimAsmConstants.X_ADD;
	// private static final int X01 = PrimAsmConstants.X_ADD + 1;
	// private static final int X02 = PrimAsmConstants.X_ADD + 2;
	// private static final int X03 = PrimAsmConstants.X_ADD + 3;
	// private static final int REG_MAX_INT = X03;
	private static final int REG_METHOD_STRUCT     = X00 + 4;
	private static final int REG_VARIABLE_POINTER  = X00 + 5;
	private static final int REG_MIN_VARIABLE      = X00 + 6;
	private static final int REG_MAX_VARIABLE      = X00 + 0x9F;
	/**
	 * the compiler won't use any registers above this value for critical stuff
	 */
	public static final int  MAX_COMPILER_REGISTER = REG_MAX_VARIABLE;
	
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
			p1 = build(A_NUM, INT_MEMORY_FREE);
			Command c = new Command(Commands.CMD_INT, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		Command c = new Command(Commands.CMD_RET, null, null);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	private void initVariableMemory(CompileTarget target, SimpleFunction sf) {
		assert target.outOfMemErrorAddr != -1L;
		Param p1, p2;
		p1 = build(A_NUM, INT_MEMORY_ALLOC);
		Command c = new Command(Commands.CMD_INT, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_SR, X00);
		p2 = build(A_NUM, -1L);
		c = new Command(Commands.CMD_CMP, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_NUM, target.pos - target.outOfMemErrorAddr);
		c = new Command(Commands.CMD_JMPEQ, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_SR, REG_VARIABLE_POINTER);
		p2 = build(A_SR, X00);
		c = new Command(Commands.CMD_MOV, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	private void compileCommand(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommand cmd) throws InternalError {
		if (cmd instanceof SimpleCommandIf) {
			SimpleCommandIf ifCmd = (SimpleCommandIf) cmd;
			compileIf(target, sf, regs, ifCmd);
		} else if (cmd instanceof SimpleCommandWhile) {
			SimpleCommandWhile whileCmd = (SimpleCommandWhile) cmd;
			compileWhile(target, sf, regs, whileCmd);
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
	
	private static final int JMP_LEN   = 16;
	private static final int JMPEQ_LEN = JMP_LEN;
	
	static {
		if (new Command(Commands.CMD_JMP, build(A_NUM, -1L), null).length() != JMP_LEN) {
			throw new AssertionError();
		}
		if (new Command(Commands.CMD_JMPEQ, build(A_NUM, -1L), null).length() != JMPEQ_LEN) {
			throw new AssertionError();
		}
	}
	
	private void compileIf(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandIf ifCmd) {
		int reg = register(regs, MAX_COMPILER_REGISTER + 2);
		boolean old = makeRegUsable(regs, reg, sf.cmds, target);
		target.pos = ifCmd.condition.loadValue(reg, regs, sf.cmds, target.pos);
		Param p1, p2;
		p1 = build(A_SR, reg);
		p2 = build(A_NUM, 0L);
		Command c = new Command(Commands.CMD_CMP, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		long startIfAddr = target.pos;
		target.pos += JMPEQ_LEN;
		List <Command> cmds = sf.cmds;
		List <Command> sub = new LinkedList <>();
		sf.cmds = sub;
		compileCommand(target, sf, regs, ifCmd.ifCmd);
		p1 = build(A_NUM, target.pos - startIfAddr);
		assert sf.cmds == sub;
		sf.cmds = cmds;
		c = new Command(Commands.CMD_JMPEQ, p1, null);
		sf.cmds.add(c);
		sf.cmds.addAll(sub);
		if (ifCmd.elseCmd != null) {
			compileElse(target, sf, regs, ifCmd, cmds);
		}
		releseReg(regs, reg, old, sf.cmds, target);
	}
	
	private void compileElse(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandIf ifCmd, List <Command> cmds) throws InternalError {
		Param p1;
		Command c;
		List <Command> sub;
		long ifEndAddr = target.pos;
		target.pos += JMP_LEN;
		sub = new LinkedList <>();
		sf.cmds = sub;
		compileCommand(target, sf, regs, ifCmd.elseCmd);
		assert sf.cmds == sub;
		sf.cmds = cmds;
		p1 = build(A_NUM, target.pos - ifEndAddr);
		c = new Command(Commands.CMD_JMP, p1, null);
		sf.cmds.add(c);
		sf.cmds.addAll(sub);
	}
	
	private void compileWhile(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandWhile whileCmd) {
		int reg = register(regs, MAX_COMPILER_REGISTER + 1);
		boolean old = makeRegUsable(regs, reg, sf.cmds, target);
		long loopStart = target.pos;
		target.pos = whileCmd.condition.loadValue(reg, regs, sf.cmds, target.pos);
		Param p1, p2;
		p1 = build(A_SR, reg);
		p2 = build(A_NUM, 0L);
		Command c = new Command(Commands.CMD_CMP, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		target.pos += JMPEQ_LEN;
		List <Command> cmds = sf.cmds;
		List <Command> sub = new LinkedList <>();
		sf.cmds = sub;
		compileCommand(target, sf, regs, whileCmd);
		assert sub == sf.cmds;
		sf.cmds = cmds;
		p1 = build(A_NUM, loopStart - target.pos);
		c = new Command(Commands.CMD_JMPEQ, p1, null);
		sf.cmds.add(c);
		sf.cmds.addAll(sub);
		releseReg(regs, reg, old, sf.cmds, target);
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
				throw new IllegalStateException(
					"function call needs a function! dependency: " + dep.path + " > '" + dep.depend + "' (not) function name: " + funcCallCmd.secondName + " > " + se);
			}
			func = (SimpleFunction) se;
			assert dep.path.addr != -1L;
			assert func.address != -1L;
		}
		if ( !func.type.equals(funcCallCmd.function.type())) {
			throw new IllegalStateException("the function call structure is diffrent to the type needed by the called function! (function: " + (funcCallCmd.firstName)
				+ (funcCallCmd.secondName == null ? "" : (":" + funcCallCmd.secondName)) + " given func-struct type: '" + funcCallCmd.function.type()
				+ "' needded func-struct type: '" + func.type + "' given func-struct: '" + funcCallCmd.function + "')");
		}
	}
	
	private void compileAssignCommand(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandAssign assignCmd) {
		if (assignCmd.target.type().isPrimitive()) if (assignCmd.target instanceof SimpleVariableValue) {
			assignVariable(target, sf, regs, ((SimpleVariableValue) assignCmd.target).sv, assignCmd.value);
		} else {
			int r1 = register(regs, MAX_COMPILER_REGISTER + 1);
			boolean o1 = makeRegUsable(regs, r1, sf.cmds, target);
			SimpleValue nt = assignCmd.target.addExpUnary(assignCmd.pool, SimpleValue.EXP_UNARY_AND);
			target.pos = nt.loadValue(r1, regs, sf.cmds, target.pos);
			int r2 = register(regs, MAX_COMPILER_REGISTER + 2);
			boolean o2 = makeRegUsable(regs, r2, sf.cmds, target);
			target.pos = assignCmd.value.loadValue(r2, regs, sf.cmds, target.pos);
			Param p1, p2;
			p1 = build(A_SR | B_REG, r1);
			p2 = build(A_SR, r2);
			Command c = new Command(Commands.CMD_MOV, p1, p2);
			target.pos += c.length();
			sf.cmds.add(c);
			releseReg(regs, r2, o2, sf.cmds, target);
			releseReg(regs, r1, o1, sf.cmds, target);
		}
	}
	
	private void call(CompileTarget target, SimpleFunction sf, SimpleCommandFuncCall funcCallCmd, boolean[] regs) {
		assert regs[REG_METHOD_STRUCT];
		regs[REG_METHOD_STRUCT] = false;
		target.pos = funcCallCmd.function.loadValue(REG_METHOD_STRUCT, regs, sf.cmds, target.pos);
		regs[REG_METHOD_STRUCT] = true;
		if (funcCallCmd.secondName == null) {
			Param p1;
			Command c;
			p1 = Param.createLabel(funcCallCmd.firstName);
			c = new Command(Commands.CMD_CALL, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		} else {
			dependencyCall(target, sf, funcCallCmd);
		}
	}
	
	private void dependencyCall(CompileTarget target, SimpleFunction sf, SimpleCommandFuncCall funcCallCmd) {
		SimpleDependency dep = funcCallCmd.pool.getDependency(funcCallCmd.firstName);
		SimpleFunction func = (SimpleFunction) dep.imps.get(funcCallCmd.secondName);
		Param p1, p2;
		p1 = build(A_SR, X00);
		p2 = build(A_NUM, dep.path.addr - target.pos);
		Command c = new Command(Commands.CMD_LEA, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_NUM, INT_GET_FILE);
		c = new Command(Commands.CMD_INT, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_SR, X00);
		p2 = build(A_NUM, -1L);
		c = new Command(Commands.CMD_CMP, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_NUM, target.dependencyLoadErrorAddr - target.pos);
		c = new Command(Commands.CMD_JMPEQ, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_SR, X00);
		p2 = build(A_NUM, func.address);
		c = new Command(Commands.CMD_CALO, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	private void pop(CompileTarget target, SimpleFunction sf) {
		Param p1;
		Command c;
		for (int i = sf.regVars - 1; i >= 0; i -- ) {
			p1 = build(A_SR, REG_MIN_VARIABLE + i);
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		if (sf.addrVars) {
			p1 = build(A_SR, REG_VARIABLE_POINTER);
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		p1 = build(A_SR, REG_METHOD_STRUCT);
		c = new Command(Commands.CMD_PUSH, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	private void push(CompileTarget target, SimpleFunction sf) {
		Param p1;
		Command c;
		p1 = build(A_SR, REG_METHOD_STRUCT);
		c = new Command(Commands.CMD_PUSH, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		if (sf.addrVars) {
			p1 = build(A_SR, REG_VARIABLE_POINTER);
			c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			sf.cmds.add(c);
		}
		for (int i = 0; i < sf.regVars; i ++ ) {
			p1 = build(A_SR, REG_MIN_VARIABLE + i);
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
			boolean old = makeRegUsable(regs, reg, sf.cmds, target);
			target.pos = value.loadValue(reg, regs, sf.cmds, target.pos);
			Param p1, p2;
			p2 = build(A_SR, reg);
			p1 = build(A_SR | B_NUM, vari.reg, vari.addr);
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
			releseReg(regs, reg, old, sf.cmds, target);
		} else {
			regs[vari.reg] = false;
			target.pos = value.loadValue(vari.reg, regs, sf.cmds, target.pos);
			assert regs[vari.reg];
		}
	}
	
	private boolean makeRegUsable(boolean[] regs, int reg, List <Command> cmds, CompileTarget target) {
		boolean old = regs[reg];
		if (old) {
			Param p1 = build(A_SR, reg);
			Command c = new Command(Commands.CMD_PUSH, p1, null);
			target.pos += c.length();
			cmds.add(c);
		}
		regs[reg] = false;
		return old;
	}
	
	private void releseReg(boolean[] regs, int reg, boolean old, List <Command> cmds, CompileTarget target) {
		if (old) {
			Param p1 = build(A_SR, reg);
			Command c = new Command(Commands.CMD_POP, p1, null);
			target.pos += c.length();
			cmds.add(c);
		}
		regs[reg] = old;
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
		
		// TODO make those two
		private long   outOfMemErrorAddr       = -1L;
		private long   dependencyLoadErrorAddr = -1L;
		private long   pos                     = -1L;
		private Writer expout                  = null;
		
		public CompileTarget(Path src, Path exp, PatrFile bin, boolean neverExe) {
			this.source = src;
			this.exports = exp;
			this.binary = bin;
			this.neverExe = neverExe;
		}
		
	}
	
}

