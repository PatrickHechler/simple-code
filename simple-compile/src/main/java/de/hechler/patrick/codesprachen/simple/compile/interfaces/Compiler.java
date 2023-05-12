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
package de.hechler.patrick.codesprachen.simple.compile.interfaces;

import java.io.IOException;
import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.File;

/**
 * a compiler can compile/translate some source files to target files.
 * <p>
 * <ol>
 * <li>at first all source/target files are added with the {@link #addTranslationUnit(Path, File)} method.</li>
 * <li>then the {@link #compile()} method is invoked, there the compiler should execute it's translation process</li>
 * </ol>
 * 
 * @author pat
 */
public interface Compiler {
	
	/**
	 * registers the given source file and target file.
	 * <p>
	 * the target file will not be closed after this call, the compile has to close the target file, after it is no
	 * longer needed
	 * <p>
	 * the compiler should not compile the source file directly in this method. (even if it can)
	 * 
	 * @param source the source file
	 * @param target the target file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void addTranslationUnit(Path source, File target) throws IOException;
	
	/**
	 * compile all added files to the given target files.
	 * <p>
	 * this method should close all target files.
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void compile() throws IOException;
	
}
