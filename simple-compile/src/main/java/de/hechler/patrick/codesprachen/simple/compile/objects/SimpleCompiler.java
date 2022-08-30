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
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.enums.CompilerCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.CompilerCommandCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.ConstantPoolCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;
import de.hechler.patrick.codesprachen.primitive.core.objects.PrimitiveConstant;
import de.hechler.patrick.codesprachen.primitive.core.utils.Convert;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser;
import de.hechler.patrick.codesprachen.simple.compile.enums.LogMode;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.TriFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAsm;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAsm.AsmParam;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAssign;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandFuncCall;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandIf;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandWhile;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueNoConst;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleVariableValue;
import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class SimpleCompiler implements TriFunction <String, String, String, SimpleDependency> {
	
	private static final long NO_LOCK = PatrFileSysConstants.NO_LOCK;
	
	private final Path    srcRoot;
	private final Path[]  lockups;
	private final Charset cs;
	private final LogMode lm;
	
	private final Map <String, CompileTarget> targets = new HashMap <>();
	
	public SimpleCompiler(Path srcRoot, Path[] lockups, Charset cs, LogMode lm) {
		this.srcRoot = srcRoot.toAbsolutePath().normalize();
		this.lockups = new Path[lockups.length];
		for (int i = 0; i < lockups.length; i ++ ) {
			this.lockups[i] = lockups[i].toAbsolutePath().normalize();
		}
		this.cs = cs;
		this.lm = lm;
	}
	
	public synchronized void addFile(PatrFile outFile, Path currentFile, Path exports, boolean neverExecutable) {
		lm.log(LogMode.files, "register simple source file file: ", currentFile.toString(), "");
		CompileTarget target = new CompileTarget(currentFile, exports, outFile, neverExecutable);
		String relPath = srcRoot.relativize(currentFile.toAbsolutePath().normalize()).toString();
		CompileTarget old = targets.put(relPath, target);
		assert old == null;
	}
	
	public void compile() {
		if (targets.isEmpty()) {
			return;
		}
		try {
			targets.values().iterator().next().binary.withLock(() -> {
				precompileQueue();
				lm.log(LogMode.compileSteps, "parsed ", "all files", "");
				// ensures that all variables have an address (also those from source dependencies)
				for (CompileTarget target : targets.values()) {
					makeFileStart(target);
				}
				lm.log(LogMode.compileSteps, "compile now the ", "simple files", "");
				// compile all before (link) and assemble
				for (CompileTarget target : targets.values()) {
					compile(target);
				}
				lm.log(LogMode.compileSteps, "assemble now the ", "simple files", "");
				// (link) and assemble
				for (CompileTarget target : targets.values()) {
					assemble(target);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	private void precompileQueue() {
		for (CompileTarget pc : targets.values()) {
			try {
				lm.log(LogMode.files, "start parsing ", pc.source.toString(), "");
				try (Reader r = Files.newBufferedReader(pc.source, cs)) {
					ANTLRInputStream in = new ANTLRInputStream();
					in.load(r, 1024, 1024);
					SimpleGrammarLexer lexer = new SimpleGrammarLexer(in);
					CommonTokenStream toks = new CommonTokenStream(lexer);
					SimpleGrammarParser parser = new SimpleGrammarParser(toks);
					parser.setErrorHandler(new BailErrorStrategy());
					pc.file = new SimpleFile(this);
					parser.simpleFile(pc.file);
				}
				lm.log(LogMode.files, "finished parsing ", pc.source.toString(), "");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public SimpleDependency apply(String name, String depend, String runtime) {
		switch (depend.substring(depend.lastIndexOf('.') + 1)) {
		case MultiCompiler.SIMPLE_SOURCE_CODE_END:
			return sourceDependency(name, depend);
		case MultiCompiler.SIMPLE_SYMBOL_FILE_END:
		default:
			return exportedDependency(name, depend, runtime);
		case MultiCompiler.PRIMITIVE_SYMBOL_FILE_END:
			return primitiveDependency(name, depend);
		}
	}
	
	private SimpleDependency primitiveDependency(String name, String depend) {
		Path p = findDependencyFile(depend);
		try (Scanner sc = new Scanner(Files.newBufferedReader(p))) {
			Map <String, PrimitiveConstant> primConsts = new HashMap <>();
			PrimitiveAssembler.readSymbols(null, primConsts, sc, p);
			Map <String, SimpleConstant> consts = new HashMap <>();
			for (PrimitiveConstant pc : primConsts.values()) {
				consts.put(pc.name, new SimpleConstant(name, pc.value, true));
			}
			return new SimpleDependency(name, depend) {
				
				@Override
				public SimpleExportable get(String name) {
					SimpleConstant sc = consts.get(name);
					if (sc == null) {
						throw new NoSuchElementException("this dependency (" + name + " > '" + p + "') has no constant with the name: " + name);
					}
					return sc;
				}
				
			};
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private SimpleDependency sourceDependency(String name, String depend) {
		CompileTarget dep = targets.get(srcRoot.relativize(srcRoot.getFileSystem().getPath(depend).normalize()).toString());
		if (dep == null) {
			throw new IllegalArgumentException("dependency could not be found! (dependnecy: '" + depend + "'");
		}
		final SimpleFile file = dep.file;
		return new SimpleDependency(name, depend) {
			
			@Override
			public SimpleExportable get(String name) {
				return file.getExport(name);
			}
			
		};
	}
	
	private static final int SSF_LEN = 3;
	private static final int PSF_LEN = 4;
	
	static {
		if (SSF_LEN != MultiCompiler.SIMPLE_SYMBOL_FILE.length()) {
			throw new AssertionError();
		}
		if (PSF_LEN != MultiCompiler.PRIMITIVE_SYMBOL_FILE.length()) {
			throw new AssertionError();
		}
	}
	
	private SimpleDependency exportedDependency(String name, String depend, String runtime) {
		Path p = findDependencyFile(depend);
		try {
			final Map <String, SimpleExportable> imps;
			try (Reader in = Files.newBufferedReader(p, cs)) {
				imps = SimpleExportable.readExports(in);
			}
			if (runtime == null) {
				if (depend.endsWith(MultiCompiler.SIMPLE_SYMBOL_FILE)) {
					runtime = depend.substring(0, depend.length() - SSF_LEN);
				} else if (depend.endsWith(MultiCompiler.PRIMITIVE_SYMBOL_FILE)) {
					runtime = depend.substring(0, depend.length() - PSF_LEN);
				} else {
					runtime = depend;
				}
			}
			return new SimpleDependency(name, runtime) {
				
				@Override
				public SimpleExportable get(String name) {
					SimpleExportable se = imps.get(name);
					if (se == null) {
						throw new NoSuchElementException(name);
					}
					return se;
				}
				
			};
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	private Path findDependencyFile(String depend) {
		for (Path p : lockups) {
			Path resolved = p.resolve(depend);
			if (Files.exists(resolved)) {
				return resolved;
			}
		}
		throw new IllegalArgumentException("dependency could not be found! (dependnecy: '" + depend + "', lockups: " + Arrays.toString(lockups) + ")");
		
	}
	
	private void assemble(CompileTarget target) throws IOException, ElementLockedException {
		try (OutputStream out = target.binary.openOutput(true, NO_LOCK)) {
			PrimitiveAssembler asm = new PrimitiveAssembler(out, null, lockups, true, false) {
				
				@Override
				protected Command replaceUnknownCommand(Command cmd) throws InternalError {
					if (cmd instanceof FuncCallCmd) {
						return replaceDepCall(cmd);
					} else {
						return super.replaceUnknownCommand(cmd);
					}
				}
				
			};
			Collection <SimpleFunction> funcs = target.file.functions();
			Map <String, Long> funcAddrs = new HashMap <>();
			for (SimpleFunction func : funcs) {
				funcAddrs.put(func.name, func.address);
			}
			List <Command> cmds = FileCommandsList.create(target.file, lm);
			asm.assemble(cmds, funcAddrs);
		}
	}
	
	private Command replaceDepCall(Command cmd) {
		FuncCallCmd fcc = (FuncCallCmd) cmd;
		assert fcc.func.address != -1L;
		Param p1 = build(A_SR, REG_DEP_FUNC_DEPENDENCY_FILE_REGISTER);
		Param p2 = build(A_NUM, fcc.func.address);
		Command c = new Command(Commands.CMD_CALO, p1, p2);
		return c;
	}
	
	private void makeFileStart(CompileTarget target) throws IOException {
		lm.log(LogMode.files, "make the start for file: ", target.source.toString(), " (data, global variables and executable start)");
		assert target.pos == -1L;
		SimpleFunction main = target.neverExe ? null : target.file.mainFunction();
		target.pos = 0L;
		if (target.exports != null) {
			target.expout = Files.newBufferedWriter(target.exports, cs);
		} else {
			target.expout = new OutputStreamWriter(OutputStream.nullOutputStream());
		}
		if (main != null) {
			executableStart(target);
		}
		fillData(target);
		lm.log(LogMode.files, "made the start of file: ", target.source.toString(), "");
	}
	
	private void compile(CompileTarget target) throws IOException {
		SimpleFunction main = target.neverExe ? null : target.file.mainFunction();
		if (main != null) {
			correctMainAddress(target);
			compileFunction(target, main);
		}
		for (SimpleFunction sf : target.file.functions()) {
			if (sf == main) continue;
			compileFunction(target, sf);
		}
	}
	
	private static final long MY_INT_OUT_OF_MEM_ERROR = PrimAsmPreDefines.INTERRUPT_COUNT;
	private static final long MY_INT_DEP_LOAD_ERROR   = MY_INT_OUT_OF_MEM_ERROR + 1L;
	private static final long MY_INT_CNT              = MY_INT_DEP_LOAD_ERROR + 1L;
	
	public static final Map <String, SimpleConstant> DEFAULT_CONSTANTS;
	
	static {
		Map <String, SimpleConstant> defConsts = new LinkedHashMap <>();
		for (PrimitiveConstant pc : PrimAsmConstants.START_CONSTANTS.values()) {
			defConsts.put(pc.name, new SimpleConstant(pc.name, pc.value, false));
		}
		defConsts.put("INT_OUT_OF_MEM_ERROR", new SimpleConstant("INT_OUT_OF_MEM_ERROR", MY_INT_CNT, false));
		defConsts.put("INT_DEP_LOAD_ERROR", new SimpleConstant("INT_DEP_LOAD_ERROR", MY_INT_DEP_LOAD_ERROR, false));
		defConsts.put("INTERRUPT_COUNT", new SimpleConstant("INTERRUPT_COUNT", MY_INT_CNT, false));
		DEFAULT_CONSTANTS = Collections.unmodifiableMap(defConsts);
	}
	
	private static final String MY_EXPORT_FILE                      = "/de/hechler/patrick/codesprachen/simple/compile/executableStart.psf";
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
					throw new AssertionError(pc + " expected: " + MY_INT_CNT);
				}
				pc = map.get(INT_OUT_OF_MEM_ERR_SYMBOL);
				if (pc.value != MY_INT_OUT_OF_MEM_ERROR) {
					throw new AssertionError(pc + " expected: " + MY_INT_OUT_OF_MEM_ERROR);
				}
				pc = map.get(INT_DEP_LOAD_ERR_SYMBOL);
				if (pc.value != MY_INT_DEP_LOAD_ERROR) {
					throw new AssertionError(pc + " expected: " + MY_INT_DEP_LOAD_ERROR);
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
		try (OutputStream out = target.binary.openOutput(true, NO_LOCK)) {
			target.outOfMemErrorAddr = target.pos;
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
		assert (target.pos & 7) == 0;
		byte[] zeros = new byte[8];
		for (SimpleValueDataPointer dv : target.file.dataValues()) {
			dv.addr = target.pos;
			target.binary.appendContent(dv.data, 0, dv.data.length, NO_LOCK);
			target.pos += dv.data.length;
			fill8Zeros(zeros);
			align(target, zeros);
		}
		for (SimpleVariable sv : target.file.vars()) {
			int len = sv.type.byteCount();
			if (zeros.length < len) {
				zeros = new byte[len];
			}
			sv.addr = target.pos;
			target.binary.appendContent(zeros, 0, len, NO_LOCK);
			target.pos += len;
			fill8Zeros(zeros);
			align(target, zeros);
		}
		assert target.pos == target.binary.length(NO_LOCK);
	}
	
	private void fill8Zeros(byte[] zeros) {
		zeros[0] = zeros[1] = zeros[2] = zeros[3] = zeros[4] = zeros[5] = zeros[6] = zeros[7] = 0;
	}
	
	private void align(CompileTarget target, byte[] zeros) throws IOException, ElementLockedException {
		if ( (target.pos & 7) != 0) {
			int len = (int) (8 - (target.pos & 7));
			target.binary.appendContent(zeros, 0, len, NO_LOCK);
			target.pos += len;
		}
	}
	
	private void executableStart(CompileTarget target) throws IOException {
		assert target.pos == 0L;
		try (InputStream in = getClass().getResourceAsStream(EXECUTABLE_START_FILE)) {
			try (OutputStream out = target.binary.openOutput(true, NO_LOCK)) {
				byte[] buf = new byte[512];
				while (true) {
					int r = in.read(buf, 0, 512);
					if (r == -1) {
						break;
					}
					out.write(buf, 0, r);
					target.pos += r;
				}
			}
		}
		lm.log(LogMode.files, "made the file executable: ", target.source.toString(), "");
	}
	
	public static final int X00                                   = PrimAsmConstants.X_ADD;
	public static final int REG_DEP_FUNC_DEPENDENCY_FILE_REGISTER = X00;
	// public static final int X01 = PrimAsmConstants.X_ADD + 1;
	// public static final int X02 = PrimAsmConstants.X_ADD + 2;
	// public static final int X03 = PrimAsmConstants.X_ADD + 3;
	// public static final int REG_MAX_INT = X03;
	/**
	 * this register is used to store the current methods results
	 */
	public static final int REG_METHOD_STRUCT     = X00 + 4;
	/**
	 * this method is reserved for array and structure variables<br>
	 * and also used by pointer/primitive variables when there are not enough registers)
	 */
	public static final int REG_VARIABLE_POINTER  = X00 + 5;
	public static final int REG_MIN_VARIABLE      = X00 + 6;
	public static final int REG_MAX_VARIABLE      = X00 + 0x9F;
	/**
	 * the compiler won't use any registers above this value for critical stuff
	 */
	public static final int MAX_COMPILER_REGISTER = REG_MAX_VARIABLE;
	
	public static class UsedData implements Cloneable {
		
		private int  regs        = REG_MIN_VARIABLE;
		private int  maxregs     = -1;
		private long currentaddr = 0L;
		private long maxaddr     = -1L;
		
		@Override
		protected UsedData clone() {
			try {
				return (UsedData) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
		}
		
	}
	
	private void compileFunction(CompileTarget target, SimpleFunction sf) throws IOException {
		lm.log(LogMode.functions, "compile function ", sf.name, " of file: " + target.source);
		sf.address = target.pos;
		if (sf.export && target.expout != null) {
			target.expout.append(sf.toExportString()).append('\n');
		}
		sf.cmds = new CommandList();
		UsedData used = sf.pool.used();
		used.currentaddr = used.maxaddr;
		used.regs = used.maxregs;
		UsedData funcArgs = used.clone();
		count(used, sf.body.commands);
		boolean[] regs = new boolean[256];
		regs[REG_METHOD_STRUCT] = true;
		regs[REG_VARIABLE_POINTER] = true;
		for (int i = REG_MIN_VARIABLE; i < used.regs; i ++ ) {
			regs[i] = true;
		}
		if (used.maxaddr > 0L) {
			initVariableMemory(target, sf);
			sf.addrVars = true;
		} else {
			sf.addrVars = false;
		}
		copyArgs(target, sf, funcArgs, sf.pool.myargs);
		sf.regVars = used.maxregs;
		for (SimpleCommand cmd : sf.body.commands) {
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
		lm.log(LogMode.functions, "comped function ", sf.name, " of file: " + target.source);
	}
	
	private void copyArgs(CompileTarget target, SimpleFunction sf, UsedData funcArgs, SimpleVariable[] myargs) {
		for (int i = 0; i < sf.type.arguments.length; i ++ ) {
			SimpleVariable myarg = myargs[i];
			SimpleVariable arg = sf.type.arguments[i];
			assert arg.reg == REG_METHOD_STRUCT;
			assert arg.addr != -1L;
			Param p1, p2;
			if (myarg.addr == -1L) {
				assert myarg.reg >= REG_MIN_VARIABLE && myarg.reg <= REG_MAX_VARIABLE : myarg.reg;
				p1 = build(A_SR, myarg.reg);
			} else if (myarg.addr == 0L) {
				assert myarg.reg == REG_VARIABLE_POINTER : myarg.name + " : " + myarg.reg + " : " + sf;
				p1 = build(A_SR | B_REG, REG_VARIABLE_POINTER);
			} else {
				assert myarg.reg == REG_VARIABLE_POINTER : myarg.name + " : " + myarg.reg + " : " + sf;
				p1 = build(A_SR | B_NUM, REG_VARIABLE_POINTER, myarg.addr);
			}
			if (arg.addr == 0L) {
				assert arg.reg == REG_METHOD_STRUCT : arg.name + " : " + arg.reg + " : " + sf;
				p2 = build(A_SR | B_REG, REG_METHOD_STRUCT);
			} else {
				assert arg.addr != -1L : arg.name + " : " + sf;
				assert arg.reg == REG_METHOD_STRUCT : arg.name + " : " + arg.reg + " : " + sf;
				p2 = build(A_SR | B_NUM, REG_METHOD_STRUCT, arg.addr);
			}
			target.pos = SimpleValueNoConst.addMovCmd(arg.type, sf.cmds, target.pos, p1, p2);
		}
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
	
	private void compileCommand(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommand cmd)
		throws InternalError {
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
			for (SimpleCommand childCmd : blockCmd.commands) {
				compileCommand(target, sf, regs, childCmd);
			}
		} else if (cmd instanceof SimpleCommandVarDecl) {
			SimpleCommandVarDecl varDeclCmd = (SimpleCommandVarDecl) cmd;
			if (varDeclCmd.init == null) {
				return;
			}
			assignVariable(target, sf, regs, varDeclCmd, varDeclCmd.init);
		} else if (cmd instanceof SimpleCommandAsm) {
			SimpleCommandAsm asm = (SimpleCommandAsm) cmd;
			compileAsm(target, sf, regs, asm);
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
	
	private void compileAsm(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandAsm asmCmd) {
		final int arglen = asmCmd.asmArguments.length;
		final int reslen = asmCmd.asmResults.length;
		boolean[] argsolds = new boolean[arglen];
		for (int i = 0; i < arglen; i ++ ) {
			AsmParam arg = asmCmd.asmArguments[i];
			freeAsmReg(target, sf, regs, argsolds, i, arg);
			target.pos = arg.value.loadValue(arg.register, regs, sf.cmds, target.pos);
		}
		addAsmCommands(target, sf, asmCmd);
		for (int i = 0; i < reslen; i ++ ) {
			AsmParam res = asmCmd.asmResults[i];
			SimpleValue val = res.value.addExpUnary(asmCmd.pool, SimpleValue.EXP_UNARY_AND);
			int reg = register(regs, res.register + 1 >= 256 ? MAX_COMPILER_REGISTER + 1 : res.register + 1);
			boolean old = makeRegUsable(regs, reg, sf.cmds, target);
			target.pos = val.loadValue(reg, regs, sf.cmds, target.pos);
			Param p1, p2;
			p1 = build(A_SR | B_REG, reg);
			p2 = build(A_SR, res.register);
			Commands mv = Commands.CMD_MOV;
			if (val.type().isPrimitive()) {
				switch (val.type().byteCount()) {
				case 8:
					break;
				case 4:
					mv = Commands.CMD_MVDW;
					break;
				case 2:
					mv = Commands.CMD_MVW;
					break;
				case 1:
					mv = Commands.CMD_MVB;
					break;
				default:
					throw new InternalError("primitive type with unknown byte count: " + val.type().byteCount() + " (" + val.type() + ")");
				}
			}
			Command mov = new Command(mv, p1, p2);
			target.pos += mov.length();
			sf.cmds.add(mov);
			releseReg(regs, reg, old, sf.cmds, target);
		}
		for (int i = 0; i < arglen; i ++ ) {
			AsmParam arg = asmCmd.asmArguments[i];
			assert regs[arg.register];
			if (argsolds[i]) {
				singlePushOrPop(target, sf, arg, Commands.CMD_POP);
			} else {
				regs[arg.register] = false;
			}
		}
	}
	
	/*
	 * private ParseContext parse(String asmCode, Map <String, SimpleConstant> consts) { asmCode = asmCode.substring(2, asmCode.length() - 2); PrimitiveAssembler asm = new
	 * PrimitiveAssembler(OutputStream.nullOutputStream(), null, new Path[0], false, true); try {
	 * 
	 * @SuppressWarnings("unchecked") ParseContext context = return context; } catch (IOException | AssembleError e) { throw new RuntimeException(e); } }
	 */
	
	@SuppressWarnings("unchecked")
	private void addAsmCommands(CompileTarget target, SimpleFunction sf, SimpleCommandAsm asmCmd) {
		try {
			final ConstantPoolCommand cpc = new ConstantPoolCommand();
			PrimitiveAssembler asm = new PrimitiveAssembler(new OutputStream() {
				
				@Override
				public void write(int b) throws IOException {
					cpc.addBytes(new byte[] {(byte) b });
					target.pos ++ ;
				}
				
				@Override
				public void write(byte[] b) throws IOException {
					cpc.addBytes(b);
					target.pos += b.length;
				}
				
				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					cpc.addBytes(Arrays.copyOfRange(b, off, off + len));
					target.pos += len;
				}
				
			}, null, lockups, false, true);
			asm.assemble(Paths.get("[INVALID]"), new ANTLRInputStream(asmCmd.asmCode), (Map <String, PrimitiveConstant>) (Map <String, ? extends PrimitiveConstant>) asmCmd.pool.getConstants());
			sf.cmds.add(cpc);
			if ( (target.pos & 7) != 0) {
				byte[] bytes = new byte[ (8 - ((int) target.pos & 7))];
				Param p1 = build(A_NUM, JMP_LEN + bytes.length);
				Command jmp = new Command(Commands.CMD_JMP, p1, null);
				target.pos += jmp.length() + bytes.length;
				sf.cmds.add(jmp);
				ConstantPoolCommand ocpc = new ConstantPoolCommand();
				ocpc.addBytes(bytes);
				sf.cmds.add(ocpc);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void freeAsmReg(CompileTarget target, SimpleFunction sf, boolean[] regs, boolean[] olds, int i, AsmParam arg) {
		olds[i] = regs[arg.register];
		if (olds[i]) {
			singlePushOrPop(target, sf, arg, Commands.CMD_PUSH);
		}
	}
	
	private void singlePushOrPop(CompileTarget target, SimpleFunction sf, AsmParam arg, Commands pushOrPop) {
		Param p1 = build(A_SR, arg.register);
		Command push = new Command(pushOrPop, p1, null);
		target.pos += push.length();
		sf.cmds.add(push);
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
	
	private void compileElse(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleCommandIf ifCmd,
		List <Command> cmds) throws InternalError {
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
	
	private void compileAssignCommand(CompileTarget target, SimpleFunction sf, boolean[] regs,
		SimpleCommandAssign assignCmd) {
		if (assignCmd.target.type().isPrimitive())
			if (assignCmd.target instanceof SimpleVariableValue) {
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
	
	private void compileFuncCall(CompileTarget target, SimpleFunction sf, boolean[] regs,
		SimpleCommandFuncCall funcCallCmd) {
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
			SimpleExportable se = dep.get(funcCallCmd.secondName);
			if (se == null || ! (se instanceof SimpleFunction)) {
				throw new IllegalStateException("function call needs a function! dependency: " + dep.path + " > '"
					+ dep.depend + "' (not) function name: " + funcCallCmd.secondName + " > " + se);
			}
			func = (SimpleFunction) se;
			assert dep.path.addr != -1L;
		}
		if ( !func.type.equals(funcCallCmd.function.type())) {
			throw new IllegalStateException(
				"the function call structure is diffrent to the type needed by the called function! (function: "
					+ (funcCallCmd.firstName)
					+ (funcCallCmd.secondName == null ? "" : (":" + funcCallCmd.secondName))
					+ " given func-struct type: '" + funcCallCmd.function.type()
					+ "' needded func-struct type: '" + func.type + "' given func-struct: '"
					+ funcCallCmd.function + "')");
		}
	}
	
	private void call(CompileTarget target, SimpleFunction sf, SimpleCommandFuncCall funcCallCmd, boolean[] regs) {
		assert regs[REG_METHOD_STRUCT];
		regs[REG_METHOD_STRUCT] = false;
		target.pos = funcCallCmd.function.loadValue(REG_METHOD_STRUCT, regs, sf.cmds, target.pos);
		assert regs[REG_METHOD_STRUCT];
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
		SimpleFunction func = (SimpleFunction) dep.get(funcCallCmd.secondName);
		Param p1, p2;
		// load dependency
		p1 = build(A_SR, REG_DEP_FUNC_DEPENDENCY_FILE_REGISTER);
		p2 = build(A_NUM, dep.path.addr - target.pos);
		Command c = new Command(Commands.CMD_LEA, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_NUM, INT_GET_FILE);
		c = new Command(Commands.CMD_INT, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		// check if error on load
		p1 = build(A_SR, REG_DEP_FUNC_DEPENDENCY_FILE_REGISTER);
		p2 = build(A_NUM, -1L);
		c = new Command(Commands.CMD_CMP, p1, p2);
		target.pos += c.length();
		sf.cmds.add(c);
		p1 = build(A_NUM, target.dependencyLoadErrorAddr - target.pos);
		c = new Command(Commands.CMD_JMPEQ, p1, null);
		target.pos += c.length();
		sf.cmds.add(c);
		// add FuncCallCmd for the link method
		c = new FuncCallCmd(func);
		target.pos += c.length();
		sf.cmds.add(c);
	}
	
	/**
	 * a {@link FuncCallCmd} is a {@link Command} which is only for Compiler internal use<br>
	 * this command is used when a function outside of the current file is called and replaced by a non internal command in the {@link SimpleCompiler#link(CompileTarget)} method
	 * 
	 * @author pat
	 */
	private static class FuncCallCmd extends Command {
		
		private static final long CALO_REG_CONST_LENGTH = 16L;
		
		static {
			if (CALO_REG_CONST_LENGTH != new Command(Commands.CMD_CALO, build(A_SR, X00), build(A_NUM, 0L)).length()) {
				throw new AssertionError();
			}
		}
		
		private final SimpleFunction func;
		
		public FuncCallCmd(SimpleFunction func) {
			super(null, null, null);
			this.func = func;
		}
		
		@Override
		public long length() {
			return CALO_REG_CONST_LENGTH;
		}
		
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
	
	private void assignVariable(CompileTarget target, SimpleFunction sf, boolean[] regs, SimpleVariable vari,
		SimpleValue value) {
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
			regs[reg] = false;
		}
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
			if ( !regs[i]) {
				return i;
			}
		}
		for (int i = fallback - 1; i > 0; i -- ) {
			if ( !regs[i]) {
				return i;
			}
		}
		return fallback;
	}
	
	public static void count(UsedData used, Iterable <?> countTarget) {
		final int startRegs = used.regs;
		final long startAddr = used.currentaddr;
		for (Object obj : countTarget) {
			if (obj instanceof SimpleCommandBlock) {
				count(used, ((SimpleCommandBlock) obj).commands);
			} else if (obj instanceof SimpleVariable) {
				SimpleVariable sv = (SimpleVariable) obj;
				assert sv.addr == -1L;
				assert sv.reg == -1;
				if ( (sv.type.isPrimitive() || sv.type.isPointer()) && used.regs < MAX_COMPILER_REGISTER) {
					sv.reg = used.regs;
					used.regs ++ ;
				} else {
					int bc = sv.type.byteCount();
					used.currentaddr = align(used.currentaddr, bc);
					sv.addr = used.currentaddr;
					sv.reg = REG_VARIABLE_POINTER;
					used.currentaddr += bc;
				}
			} else if (obj instanceof SimpleCommand) {
				// do nothing
			} else {
				throw new InternalError("unknown class: '" + obj.getClass().getName() + "' of object: '" + obj + "'");
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
	
	public static long align(long pos, int bc) {
		int high = 8;
		if (Integer.bitCount(bc) == 1 && bc < 8) {
			high = bc;
		}
		int and = high - 1;
		if ( (pos & and) != 0) {
			pos += high - (pos & and);
		}
		return pos;
	}
	
	private static class CompileTarget {
		
		private final Path     source;
		private final Path     exports;
		private final PatrFile binary;
		private final boolean  neverExe;
		
		private volatile SimpleFile file;
		
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
	
	private static class FileCommandsList extends AbstractList <Command> implements List <Command> {
		
		private final SimpleFunction[] funcs;
		private final LogMode          lm;
		
		private FileCommandsList(SimpleFunction[] funcs, LogMode lm) {
			this.funcs = funcs;
			this.lm = lm;
		}
		
		public static List <Command> create(SimpleFile file, LogMode lm) {
			Collection <SimpleFunction> functions = file.functions();
			SimpleFunction[] funcs = functions.toArray(new SimpleFunction[functions.size()]);
			try {
				assert false;
			} catch (AssertionError ae) {
				SimpleFunction[] sorted = funcs.clone();
				Arrays.sort(sorted, (a, b) -> Long.compare(a.address, b.address));
				assert Arrays.equals(funcs, sorted);
			}
			return new FileCommandsList(funcs, lm);
		}
		
		@Override
		public Iterator <Command> iterator() {
			return new Iterator <Command>() {
				
				private int                i   = 0;
				private Iterator <Command> sub = null;
				
				@Override
				public boolean hasNext() {
					return i < funcs.length || (sub != null && sub.hasNext());
				}
				
				@Override
				public Command next() {
					if (sub != null && sub.hasNext()) {
						return sub.next();
					}
					if (i >= funcs.length) {
						throw new NoSuchElementException();
					}
					sub = funcs[i].cmds.iterator();
					lm.log(LogMode.functions, "assemble function: ", funcs[i].toString(), "");
					CompilerCommandCommand ccc;
					if (i != 0) {
						ccc = new CompilerCommandCommand(CompilerCommand.assertPos, funcs[i].address);
					} else {
						ccc = new CompilerCommandCommand(CompilerCommand.setPos, funcs[i].address);
					}
					i ++ ;
					return ccc;
				}
				
			};
		}
		
		@Override
		public Command get(int index) {
			long i;
			int fsi;
			for (i = 1, fsi = 0; fsi < funcs.length; i ++ , fsi ++ ) {
				if (i > index) {
					if (i == 0) {
						return new CompilerCommandCommand(CompilerCommand.setPos, funcs[fsi].address);
					} else {
						return new CompilerCommandCommand(CompilerCommand.assertPos, funcs[fsi].address);
					}
				}
				long newI = i + funcs[fsi].cmds.size();
				if (newI > index) {
					return funcs[fsi].cmds.get((int) (index - i));
				}
				i = newI;
			}
			throw new IndexOutOfBoundsException();
		}
		
		@Override
		public int size() {
			long s;
			int i;
			for (s = 0L, i = 0; i < funcs.length; i ++ , s ++ ) {
				s += funcs[i].cmds.size();
			}
			return (int) Math.min(Integer.MAX_VALUE, s);
		}
		
	}
	
}
