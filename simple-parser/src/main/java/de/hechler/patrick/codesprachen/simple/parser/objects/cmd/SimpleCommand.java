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
package de.hechler.patrick.codesprachen.simple.parser.objects.cmd;

import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;

public abstract class SimpleCommand implements SimpleScope {
	
	protected final SimpleScope parent;
	
	public SimpleCommand(SimpleScope parent) {
		this.parent = parent;
	}
	
	public abstract SimpleValue directNameValueOrNull(String name, ErrorContext ctx);
	
	@Override
	public SimpleValue nameValueOrNull(String name, ErrorContext ctx) {
		SimpleValue value = directNameValueOrNull(name, ctx);
		if ( value != null ) return value;
		return parent.nameValueOrNull(name, ctx);
	}
	
	@Override
	public Object nameTypeOrDepOrFuncOrNull(String typedefName, ErrorContext ctx) {
		return parent.nameTypeOrDepOrFuncOrNull(typedefName, ctx);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new StringBuilder());
		return sb.toString();
	}
	
	public abstract void toString(StringBuilder append, StringBuilder indent);
	
}
