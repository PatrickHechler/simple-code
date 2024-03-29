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

import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;

public record SimpleTypedef(String name, int flags, SimpleType type) implements SimpleExportable<SimpleTypedef> {
	
	public static final int FLAG_EXPORT = 0x1000;
	public static final int FLAG_FROM_ME_DEP = 0x2000;
	
	@Override
	public SimpleTypedef replace(SimpleTypedef other) {
		if ( ( other.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) == 0
			&& ( ( this.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) == 0 ) ) {
			return null;
		}
		if ( !other.type().equals(this.type()) ) {
			return null;
		}
		if ( ( other.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) != 0 ) {
			return this;
		}
		return other;
	}
	
}
