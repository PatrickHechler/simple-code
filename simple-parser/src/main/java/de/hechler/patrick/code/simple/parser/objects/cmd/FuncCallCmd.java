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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.value.CastVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class FuncCallCmd extends SimpleCommand {
	
	public final SimpleValue       func;
	public final List<SimpleValue> results;
	public final List<SimpleValue> arguments;
	
	private FuncCallCmd(SimpleScope parent, SimpleValue func, List<SimpleValue> results, List<SimpleValue> arguments) {
		super(parent);
		this.func = func;
		this.results = Collections.unmodifiableList(results);
		this.arguments = Collections.unmodifiableList(arguments);
	}
	
	public static FuncCallCmd create(SimpleScope parent, SimpleValue func, List<SimpleValue> results, List<SimpleValue> arguments,
		ErrorContext ctx) {
		SimpleType type = func.type();
		if ( !( type instanceof FuncType ft ) || ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new CompileError(ctx, "I need something of type function address to call it (got: " + type + ")!");
		}
		List<SimpleValue> res = convert(results == null ? List.of() : results, ft.resMembers(), ctx);
		List<SimpleValue> args = convert(arguments, ft.argMembers(), ctx);
		return new FuncCallCmd(parent, func, res, args);
	}
	
	private static List<SimpleValue> convert(List<SimpleValue> results, List<SimpleVariable> resMembers, ErrorContext ctx) {
		List<SimpleValue> res = new ArrayList<>(resMembers.size());
		if ( resMembers.size() != results.size() ) {
			throw new CompileError(ctx, "wrong number of function result assignments!");
		}
		for (int i = 0; i < resMembers.size(); i++) {
			if ( results.get(i) == null ) {
				res.add(null);
				continue;
			}
			SimpleType targetType = resMembers.get(i).type();
			results.get(i).type().checkCastable(targetType, ctx, false);
			res.add(CastVal.create(results.get(i), targetType, ctx));
		}
		return res;
	}
	
	@Override
	@SuppressWarnings("unused")
	public void directAvailableNames(Set<String> add) {}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, @SuppressWarnings("unused") StringBuilder indent) {
		append.append(this.func).append(" <");
		append(append, this.results);
		append.append("> <-- (");
		append(append, this.arguments);
		append.append(");");
	}
	
	private static void append(StringBuilder append, List<SimpleValue> list) {
		if ( list.isEmpty() ) return;
		if ( list.get(0) == null ) append.append('?');
		else append.append(list.get(0));
		for (int i = 1, s = list.size(); i < s; i++) {
			SimpleValue e = list.get(i);
			if ( e != null ) {
				append.append(", ").append(e);
			} else {
				append.append(", ?");
			}
		}
	}
	
}
