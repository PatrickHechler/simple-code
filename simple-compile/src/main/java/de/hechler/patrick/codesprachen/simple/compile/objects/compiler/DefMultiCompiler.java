package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

public class DefMultiCompiler extends MultiCompiler {
	
	public static final String SIMPLE_SOURCE_CODE_END    = "ssc";
	public static final String PRIMITIVE_SOURCE_CODE_END = "psc";
	public static final String SIMPLE_SYMBOL_FILE_END    = "ssf";
	public static final String PRIMITIVE_SYMBOL_FILE_END = "psf";
	
	public static final int FILE_END_LEN = 3;
	
	public static final String SIMPLE_SOURCE_CODE    = "." + SIMPLE_SOURCE_CODE_END;
	public static final String PRIMITIVE_SOURCE_CODE = "." + PRIMITIVE_SOURCE_CODE_END;
	public static final String SIMPLE_SYMBOL_FILE    = "." + SIMPLE_SYMBOL_FILE_END;
	public static final String PRIMITIVE_SYMBOL_FILE = "." + PRIMITIVE_SYMBOL_FILE_END;
	
	public static final int FILE_LEN = FILE_END_LEN + 1;
	
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	
	private final Compiler other;
	private final Compiler simpleSym;
	private final Compiler simpleSrc;
	private final Compiler primSym;
	private final Compiler primSrc;
	
	public DefMultiCompiler(Compiler other, Compiler simpleSym, Compiler simpleSrc, Compiler primSym,
			Compiler primSrc) {
		this.other = other;
		this.simpleSym = simpleSym;
		this.simpleSrc = simpleSrc;
		this.primSym = primSym;
		this.primSrc = primSrc;
	}
	
	public DefMultiCompiler(Compiler other, Compiler simple, Compiler prim, Compiler symbol) {
		this(other, symbol, simple, symbol, prim);
	}
	
	public DefMultiCompiler(Compiler other, Compiler simple, Compiler prim) {
		this(other, simple, prim, new IgnoreCompiler());
	}
	
	public DefMultiCompiler(Charset cs, Compiler other, Path srcRoot, Path... lockups) {
		this(other, new SimpleCompiler(cs, srcRoot, lockups), new PrimitiveCompiler(cs, lockups));
	}
	
	public DefMultiCompiler(Charset cs, Path srcRoot, Path... lockups) {
		this(cs, new CopyCompiler(), srcRoot, lockups);
	}
	
	public DefMultiCompiler(Charset cs, Compiler other) {
		this(cs, other, Paths.get("."), Paths.get("/patr-symbols/"), Paths.get("~/patr-symbols/"));
	}
	
	public DefMultiCompiler(Charset cs) {
		this(cs, new CopyCompiler());
	}
	
	public DefMultiCompiler() {
		this(DEFAULT_CHARSET);
	}
	
	@Override
	protected Compiler findCompiler(Path source) {
		String name  = source.getFileName().toString();
		int    start = name.lastIndexOf('.') + 1;
		String end   = name.substring(start, name.length() - start);
		switch (end) {
		case PRIMITIVE_SOURCE_CODE_END:
			return primSrc;
		case SIMPLE_SOURCE_CODE_END:
			return simpleSrc;
		case PRIMITIVE_SYMBOL_FILE_END:
			return primSym;
		case SIMPLE_SYMBOL_FILE_END:
			return simpleSym;
		default:
			return other;
		}
	}
	
}
