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
package de.hechler.patrick.code.simple.parser.objects.simplefile;

import de.hechler.patrick.code.simple.parser.objects.cmd.BlockCmd;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;

public record SimpleFunction(SimpleDependency dep, String name, FuncType type, BlockCmd block)
	implements SimpleExportable<SimpleFunction> {
	
	public SimpleFunction(SimpleDependency dep, String name, FuncType type) {
		this(dep, name, type, null);
	}
	
	public SimpleFunction {
		if ( ( type.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new AssertionError();
		}
	}
	
	@Override
	public SimpleFunction replace(SimpleFunction other) {
		if ( !this.type.equals(other.type) ) {
			return null;
		}
		if ( ( this.type.flags() & ~( FLAG_EXPORT | FLAG_FROM_ME_DEP | FuncType.FLAG_INIT | FuncType.FLAG_MAIN ) )//
			!= ( other.type.flags() & ~( FLAG_EXPORT | FLAG_FROM_ME_DEP | FuncType.FLAG_INIT | FuncType.FLAG_MAIN ) ) ) {
			return null;
		}
		if ( this.block != null && other.block != null ) {
			return null;
		}
		if ( !this.type.equalsIgnoreNonStructuralFlags(other) ) {
			return null;
		}
		if ( this.block != null ) {
			return this;
		}
		return other;
	}
	
}
