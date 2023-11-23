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
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;

public class IfCmd extends SimpleCommand {
	
	public final SimpleValue   condition;
	public final SimpleCommand trueCmd;
	public final SimpleCommand falseCmd;
	
	private IfCmd(SimpleScope parent, SimpleValue condition, SimpleCommand trueCmd, SimpleCommand falseCmd) {
		super(parent);
		this.condition = condition;
		this.trueCmd = trueCmd;
		this.falseCmd = falseCmd;
	}
	
	public static IfCmd create(SimpleScope parent, SimpleValue condition, SimpleCommand trueCmd, SimpleCommand falseCmd,
		ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx, false);
		return new IfCmd(parent, condition, trueCmd, falseCmd);
	}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("if (").append(this.condition).append(") ");
		this.trueCmd.toString(append, indent);
		if ( this.falseCmd != null ) {
			append.append('\n').append(indent);
			this.falseCmd.toString(append, indent);
		}
	}
	
}
