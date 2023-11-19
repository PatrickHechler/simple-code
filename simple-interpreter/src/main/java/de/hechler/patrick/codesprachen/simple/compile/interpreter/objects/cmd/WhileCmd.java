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
package de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.interpreter.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.value.SimpleValue;

public class WhileCmd extends SimpleCommand {
	
	public final SimpleValue condition;
	public final SimpleCommand loop;
	
	private WhileCmd(SimpleScope parent, SimpleValue condition, SimpleCommand loop) {
		super(parent);
		this.condition = condition;
		this.loop = loop;
	}
	
	public static WhileCmd create(SimpleScope parent, SimpleValue condition, SimpleCommand loop, ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx, false);
		return new WhileCmd(parent, condition, loop);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("while (").append(this.condition).append(") ");
		loop.toString(append, indent);
	}
	
}
