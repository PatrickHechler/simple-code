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
package de.hechler.patrick.code.simple.parser.objects.cmd;

import java.util.Set;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class StructFuncCallCmd extends SimpleCommand {
	
	public final SimpleValue func;
	public final SimpleValue fstruct;
	
	private StructFuncCallCmd(SimpleScope parent, SimpleValue func, SimpleValue fstruct) {
		super(parent);
		this.func = func;
		this.fstruct = fstruct;
	}
	
	public static StructFuncCallCmd create(SimpleScope parent, SimpleValue func, SimpleValue fstruct,
		ErrorContext ctx) {
		if ( !( func.type() instanceof FuncType ft ) || ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new CompileError(ctx, "I need something of type function address to call it!");
		}
		if ( !ft.isInvokableBy(fstruct.type()) ) {
			throw new CompileError(ctx,
				"the type of the function structure does not match the type of the function: function type: " + ft
					+ " function structure type: " + fstruct.type());
		}
		return new StructFuncCallCmd(parent, func, fstruct);
	}
	
	@Override
	@SuppressWarnings("unused")
	public void directAvailableNames(Set<String> add) {}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("call ").append(this.func).append(' ').append(this.fstruct).append(';');
	}
	
}
