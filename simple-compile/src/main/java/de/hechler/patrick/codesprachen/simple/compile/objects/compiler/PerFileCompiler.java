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

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SCompiler;
import de.hechler.patrick.zeugs.pfs.interfaces.File;


public abstract class PerFileCompiler implements SCompiler {
	
	private final List<TwoFiles> paths = new LinkedList<>();
	
	public void addTranslationUnit(Path source, File target) throws IOException {
		paths.add(new TwoFiles(source, target));
	}
	
	public void compile() throws IOException {
		try {
			for (TwoFiles tp : paths) {
				compile(tp.source, tp.target);
			}
		} finally {
			for (TwoFiles tp : paths) {
				tp.target.close();
			}
		}
	}
	
	/**
	 * compile the given source file to the target file.
	 * <p>
	 * after all files are compiled they are closed automatically, therre is no need to close the target file
	 * 
	 * @param source the source file
	 * @param target the target file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	protected abstract void compile(Path source, File target) throws IOException;
	
	private static class TwoFiles {
		
		private final Path source;
		private final File target;
		
		private TwoFiles(Path source, File target) {
			this.source = source;
			this.target = target;
		}
		
	}
	
}
