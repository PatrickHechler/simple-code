package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DefMultiCompiler extends MultiCompiler {
	
	public static final String SIMPLE_SOURCE_CODE_END    = "ssc";
	public static final String PRIMITIVE_SOURCE_CODE_END = "psc";
	public static final String SIMPLE_SYMBOL_FILE_END    = "ssf";
	public static final String PRIMITIVE_SYMBOL_FILE_END = "psf";
	
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final boolean DEFAULT_FORCE   = false;
	
	private final Charset cs;
	private final boolean force;
	
	private final Compiler other,
												  simpleSym,
												  simpleSrc,
												  primSym,
												  primSrc;
	
	public DefMultiCompiler(Charset cs, boolean force, Compiler other, Compiler simpleSym, Compiler simpleSrc, Compiler primSym, Compiler primSrc) {
		this.cs = cs;
		this.force = force;
		this.other = other;
		this.simpleSym = simpleSrc;
		this.simpleSrc = simpleSrc;
		this.primSym = primSym;
		this.primSrc = primSrc;
	}
	
	public DefMultiCompiler(Charset cs, boolean force, Compiler other, Compiler simple, Compiler prim, Compile symbol) {
		this(cs, force, other, symbol, simple, symbol, prim);
	}
	public DefMultiCompiler(Charset cs, boolean force, Compiler other, Compiler simple, Compiler prim) {
		this(cs, force, other, simple, prim, new IgnoreCompiler());
	}
	
	public DefMultiCompiler(Charset cs, boolean force, Compiler other, Path srcRoot, Path... lockups) {
		this(cs, force, other, new SimpleCompiler(cs, srcRoot, lockups), new PrimitiveCompiler(cs));
	}
	public DefMultiCompiler(Charset cs, boolean force, Compiler other) {
		this(cs, force, other, Paths.get("."), Paths.get("/s.sym/"));
	}
	
	public DefMultiCompiler(Charset cs, boolean force) {
		this(cs, force, new CopyCompiler());
	}
	
	public DefMultiCompiler() {
		this(DEFAULT_CHARSET, DEFAULT_FORCE);
	}
	
	@Override
	protected Compiler findCompiler(Path source) {
		String name = source.getFileName().toString();
		int start = name.lastIndexOf('.') + 1;
		String end = name.substring(start, name.length() - start);
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
