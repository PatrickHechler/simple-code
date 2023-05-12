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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.FileTypes;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;
import de.hechler.patrick.zeugs.pfs.interfaces.File;


public class PrimitiveCompiler extends PerFileCompiler {
	
	private final Charset cs;
	private final Path[]  lockups;
	
	public PrimitiveCompiler(Charset cs, Path... lockups) {
		this.cs = cs;
		this.lockups = lockups;
	}
	
	public void compile(Path source, File target) throws IOException {
		Path sym = symFile(source);
		try (OutputStream out = target.openAppend().asOutputStream()) {
			try (BufferedReader reader = Files.newBufferedReader(source, cs)) {
				PrimitiveAssembler asm;
				try (OutputStream eo = Files.newOutputStream(sym)) {
					try (PrintStream eop = new PrintStream(eo, false, cs)) {
						asm = new PrimitiveAssembler(out, eop, lockups, false, true);
						asm.assemble(Path.of(source.toString()), reader);
						eop.flush();
					}
				}
				if (Files.size(sym) == 0) {
					Files.delete(sym);
				}
			}
		}
	}
	
	private static Path symFile(Path src) {
		String name = src.getFileName().toString();
		String end  = FileTypes.PRIMITIVE_SOURCE_CODE.getExtensionWithDot();
		if (name.endsWith(end)) {
			name = name.substring(0, name.length() - end.length());
		}
		name += FileTypes.PRIMITIVE_SYMBOL_FILE.getExtensionWithDot();
		return src.resolveSibling(name);
	}
	
}
