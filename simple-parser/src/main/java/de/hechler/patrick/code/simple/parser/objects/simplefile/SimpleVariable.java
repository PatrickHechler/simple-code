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

import java.util.Objects;

import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public record SimpleVariable(SimpleType type, String name, SimpleValue initialValue, int flags)
	implements SimpleExportable<SimpleVariable> {
	
	public static final int FLAG_CONSTANT = 0x0004;
	public static final int FLAG_GLOBAL = 0x0008;
	public static final int ALL_FLAGS = FLAG_CONSTANT | FLAG_EXPORT | FLAG_GLOBAL | FLAG_FROM_ME_DEP;
	
	public SimpleVariable(SimpleType type, String name, SimpleValue initialValue, int flags) {
		this.type = Objects.requireNonNull(type, "type");
		this.name = Objects.requireNonNull(name, "name");
		this.flags = flags & ALL_FLAGS;
		this.initialValue = initialValue;
		if ( this.flags != flags ) {
			throw new IllegalArgumentException("invalid flags: " + Integer.toHexString(flags));
		}
	}
	
	@Override
	public SimpleVariable replace(SimpleVariable other) {
		if ( !this.type.equals(other.type) ) {
			return null;
		}
		if ( ( this.flags & ~( FLAG_EXPORT | FLAG_FROM_ME_DEP ) ) != ( other.flags & ~( FLAG_EXPORT | FLAG_FROM_ME_DEP ) ) ) {
			return null;
		}
		if ( this.initialValue != null && other.initialValue != null ) {
			if ( ( this.flags & FLAG_CONSTANT ) != 0 || !this.initialValue.equals(other.initialValue) ) {
				return null;
			}
		} else if ( this.initialValue != null ) {
			return this;
		}
		return other;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( ( this.flags & FLAG_CONSTANT ) != 0 ) {
			sb.append("const ");
		}
		if ( ( this.flags & FLAG_EXPORT ) != 0 ) {
			sb.append("exp ");
		}
		sb.append(this.type).append(' ').append(this.name);
		if ( this.initialValue != null ) {
			sb.append(" <-- ").append(this.initialValue);
		}
		return sb.append(';').toString();
	}
	
}
