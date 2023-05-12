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
package de.hechler.patrick.codesprachen.simple.compile.enums;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;

public enum FileType {
	
	/**
	 * compile the file as a simple code file.<br>
	 * mark the output file as executable if it has a main function
	 * <p>
	 * by default every file with *.sc is {@link #simpleCode}
	 */
	simpleCode,
	/**
	 * compile the file as a simple code file.<br>
	 * never mark the output file as executable
	 * <p>
	 * by default no file is {@link #simpleCodeNonExecutable}
	 */
	simpleCodeNonExecutable,
	/**
	 * assemble the file with the {@link PrimitiveAssembler}<br>
	 * mark the output file as executable
	 * <p>
	 * by default every file with *.psc is {@link #primitiveCode}
	 */
	primitiveCode,
	/**
	 * assemble the file with the {@link PrimitiveAssembler}<br>
	 * do not mark the output file as executable
	 * <p>
	 * by default no file is {@link #primitiveCodeExecutable}
	 */
	primitiveCodeExecutable,
	/**
	 * copy the file<br>
	 * do not mark the copied file as executable
	 * <p>
	 * by default every file which is not used anywhere else is {@link #copy}
	 */
	copy,
	/**
	 * copy the file<br>
	 * mark the copied file as executable
	 * <p>
	 * by default no file is {@link #copyExecutable}
	 */
	copyExecutable,
	/**
	 * ignore the file<br>
	 * do not create an output file for this input file
	 * <p>
	 * by default no file is {@link #ignore}
	 */
	ignore,
	
}
