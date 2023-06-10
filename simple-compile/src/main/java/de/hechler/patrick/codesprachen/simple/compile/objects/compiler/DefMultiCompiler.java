//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SCompiler;

@SuppressWarnings("javadoc")
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
	
	private final SCompiler other;
	private final SCompiler simpleSym;
	private final SCompiler simpleSrc;
	private final SCompiler primSym;
	private final SCompiler primSrc;
	
	public DefMultiCompiler(SCompiler other, SCompiler simpleSym, SCompiler simpleSrc, SCompiler primSym,
			SCompiler primSrc) {
		this.other = other;
		this.simpleSym = simpleSym;
		this.simpleSrc = simpleSrc;
		this.primSym = primSym;
		this.primSrc = primSrc;
	}
	
	public DefMultiCompiler(SCompiler other, SCompiler simple, SCompiler prim, SCompiler symbol) {
		this(other, symbol, simple, symbol, prim);
	}
	
	public DefMultiCompiler(SCompiler other, SCompiler simple, SCompiler prim) {
		this(other, simple, prim, new IgnoreCompiler());
	}
	
	public DefMultiCompiler(Charset cs, SCompiler other, Path srcRoot, Path... lockups) {
		this(other, new SimpleCompiler(cs, srcRoot, lockups), new PrimitiveCompiler(cs, lockups));
	}
	
	public DefMultiCompiler(Charset cs, Path srcRoot, Path... lockups) {
		this(cs, new CopyCompiler(), srcRoot, lockups);
	}
	
	public DefMultiCompiler(Charset cs, SCompiler other) {
		this(cs, other, Paths.get("."), Paths.get("/patr-symbols/"), Paths.get("~/patr-symbols/"));
	}
	
	public DefMultiCompiler(Charset cs) {
		this(cs, new CopyCompiler());
	}
	
	public DefMultiCompiler() {
		this(DEFAULT_CHARSET);
	}
	
	@Override
	protected SCompiler findCompiler(Path source) {
		String name  = source.getFileName().toString();
		int    start = name.lastIndexOf('.') + 1;
		String end   = name.substring(start, name.length());
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
