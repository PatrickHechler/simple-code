package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants.X_ADD;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;

import de.hechler.patrick.codesprachen.primitive.core.objects.PrimitiveConstant;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser.SimpleFileContext;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.TriFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueConst;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleNameable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypeArray;
import de.hechler.patrick.zeugs.pfs.interfaces.File;

public class SimpleCompiler extends StepCompiler<SimpleCompiler.SimpleTU> {
	
	// X00 .. X0F are reserved for interrupts and asm blocks
	// X10 .. X1F are reserved for special compiler registers
	// X20 .. XCF are reserved for variables
	// XD0 .. XF9 are reserved for temporary values
	public static final int MIN_COMPILER_REGISTER = X_ADD + 0x10;
	public static final int REG_METHOD_STRUCT     = MIN_COMPILER_REGISTER;
	public static final int REG_VAR_PNTR          = X_ADD + 0x11;
	public static final int MIN_VAR_REGISTER      = X_ADD + 0x20;
	public static final int MAX_VAR_REGISTER      = X_ADD + 0xCF;
	public static final int MIN_TMP_VAL_REG       = MAX_VAR_REGISTER + 1;
	
	public static final Map<String, SimpleConstant> DEFAULT_CONSTANTS;
	
	public static final long INT_ERR_OUT_OF_MEM = PrimAsmPreDefines.INTERRUPT_COUNT;
	public static final long INTERRUPT_COUNT    = INT_ERR_OUT_OF_MEM + 1;
	
	static {
		Map<String, SimpleConstant> defConsts = new HashMap<>();
		for (PrimitiveConstant cnst : PrimAsmConstants.START_CONSTANTS.values()) {
			defConsts.put(cnst.name(), new SimpleConstant(cnst.name(), cnst.value(), false));
		}
		defConsts.put("INT_ERR_OUT_OF_MEM", new SimpleConstant("INT_ERR_OUT_OF_MEM", INT_ERR_OUT_OF_MEM, false));
		defConsts.put("INTERRUPT_COUNT", new SimpleConstant("INTERRUPT_COUNT", INTERRUPT_COUNT, false));
		DEFAULT_CONSTANTS = Collections.unmodifiableMap(defConsts);
	}
	
	private final Charset cs;
	private final Path    srcRoot;
	private final Path[]  lockups;
	
	public SimpleCompiler(Charset cs, Path srcRoot, Path[] lockups) {
		this.cs = cs;
		this.srcRoot = srcRoot;
		this.lockups = lockups.clone();
	}
	
	@Override
	protected SimpleTU createTU(Path source, File target) {
		return new SimpleTU(source, target);
	}
	
	@Override
	protected void init(SimpleTU tu) throws IOException {
		try (Reader r = Files.newBufferedReader(tu.source, cs)) {
			ANTLRInputStream in = new ANTLRInputStream();
			in.load(r, 1024, 1024);
			SimpleGrammarLexer  lexer  = new SimpleGrammarLexer(in);
			CommonTokenStream   toks   = new CommonTokenStream(lexer);
			SimpleGrammarParser parser = new SimpleGrammarParser(toks);
			parser.setErrorHandler(new BailErrorStrategy());
			tu.sf = new SimpleFile(tu);
			tu.context = parser.simpleFile(tu.sf);
		}
	}
	
	@Override
	protected void precompile(SimpleTU tu) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void compile(SimpleTU tu) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void finish(SimpleTU tu) throws IOException {
		// TODO Auto-generated method stub
	}
	
	public static long align(long addr, int bc) {
		return SimpleCompiler.align(addr, bc);
	}
	
	private class SimpleSourceDependency extends SimpleDependency {
		
		private final Path dependency;
		
		public SimpleSourceDependency(String name, String runtimeDepend, Path dependency) {
			super(name, runtimeDepend);
			this.dependency = dependency;
		}
		
		@Override
		public SimpleExportable get(String name) {
			SimpleTU stu = SimpleCompiler.super.tus.get(dependency);
			if (stu == null) {
				throw new NoSuchElementException(
						"the dependency " + depend + ": '" + dependency + "' could not be found");
			}
			SimpleNameable named = stu.named.get(name);
			if (!(named instanceof SimpleExportable se) || !se.isExport()) {
				throw new NoSuchElementException(
						"the dependency " + depend + ": does not export anything with the given " + name);
			}
			return se;
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
	}
	
	public static class SimpleSymbolDependency extends SimpleDependency {
		
		private final Map<String, SimpleExportable> imported;
		
		public SimpleSymbolDependency(String name, String runtimeDepend, Map<String, SimpleExportable> imported) {
			super(name, runtimeDepend);
			this.imported = imported;
		}
		
		private static SimpleSymbolDependency create(String name, String run, Path path, Charset cs) {
			try (BufferedReader r = Files.newBufferedReader(path, cs)) {
				Map<String, SimpleExportable> is = SimpleExportable.readExports(r);
				return new SimpleSymbolDependency(name, run, is);
			} catch (IOException e) {
				throw new IOError(e);
			}
			
		}
		
		@Override
		public SimpleExportable get(String name) {
			SimpleExportable se = imported.get(name);
			if (se == null) { throw new NoSuchElementException(name); }
			return se;
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
	}
	
	public class SimpleTU extends TranslationUnit implements TriFunction<String, String, String, SimpleDependency> {
		
		public SimpleFileContext context;
		
		private final Map<String, SimpleNameable> named = new HashMap<>();
		
		public SimpleFile sf;
		
		public SimpleTU(Path source, File target) {
			super(source, target);
		}
		
		@Override
		public SimpleDependency apply(String name, String compileDepend, String runtimeDepend) {
			Path   path = srcRoot.resolve(compileDepend);
			String dep;
			if (runtimeDepend != null) {
				dep = runtimeDepend;
			} else {
				dep = SimpleDependency.runtimeName(compileDepend);
			}
			SimpleDependency res = null;
			if (Files.exists(path)) {
				res = new SimpleSourceDependency(name, dep, path.normalize());
			} else {
				for (Path p : lockups) {
					if (Files.exists(p)) {
						res = SimpleSymbolDependency.create(name, dep, path, cs);
						break;
					}
				}
				if (res == null) {
					throw new NoSuchElementException(
							"could not find the dependency " + name + "; '" + compileDepend + "'");
				}
			}
			if (named.put(name, res) != null) {
				throw new IllegalStateException("the name " + name + " is already used!");
			}
			return res;
		}
		
	}
	
	// this is used by antlr, can't be done in the SimpleConstant class because the SimpleValue class is not known there
	/**
	 * creates a new {@link SimpleConstant}
	 * 
	 * @param name   the name of the constant
	 * @param val    the value of the constant
	 * @param export the export flag of the constnat
	 * 
	 * @return the newly created constant
	 */
	public static SimpleConstant createConstant(String name, SimpleValue val, boolean export) {
		if (!(val instanceof SimpleValueConst c)) {
			throw new IllegalArgumentException("a constant needs a constant value (val: " + val + ')');
		}
		return new SimpleConstant(name, c.getNumber(), export);
	}
	
	public static SimpleType createArray(SimpleType t, SimpleValue val) {
		if (val == null) {
			return new SimpleTypeArray(t, -1L);
		}
		if (!(val instanceof SimpleValueConst c)) {
			throw new IllegalArgumentException("the length of an array needs to be constant (val: " + val + ')');
		}
		return new SimpleTypeArray(t, c.getNumber());
	}
	
}
