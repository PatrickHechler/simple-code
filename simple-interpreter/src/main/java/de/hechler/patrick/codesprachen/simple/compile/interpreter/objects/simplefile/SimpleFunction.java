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
package de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.simplefile;

import de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.cmd.BlockCmd;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.types.FuncType;

public record SimpleFunction(String name, FuncType type, BlockCmd block) {
	
	public SimpleFunction(String name, FuncType type) {
		this(name, type, null);
	}
	
	public SimpleFunction {
		if ( ( type.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new AssertionError();
		}
	}
	
}
