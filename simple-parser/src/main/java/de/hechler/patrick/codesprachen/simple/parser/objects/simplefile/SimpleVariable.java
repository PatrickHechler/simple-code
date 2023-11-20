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
package de.hechler.patrick.codesprachen.simple.parser.objects.simplefile;

import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;

public record SimpleVariable(SimpleType type, String name, SimpleValue initialValue, int flags) {
	
	public static final int FLAG_CONSTANT = 0x0001;
	public static final int FLAG_EXPORT = 0x0002;
	public static final int ALL_FLAGS = FLAG_CONSTANT | FLAG_EXPORT;
	
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
