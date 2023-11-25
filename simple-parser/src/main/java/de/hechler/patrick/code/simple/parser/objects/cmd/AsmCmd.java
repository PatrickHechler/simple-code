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

import java.util.List;

import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class AsmCmd extends SimpleCommand {
	
	public final List<AsmParam>  params;
	public final String          asm;
	public final List<AsmResult> results;
	
	private AsmCmd(SimpleScope parent, List<AsmParam> params, String asm, List<AsmResult> results) {
		super(parent);
		this.params = List.copyOf(params);
		this.asm = asm;
		this.results = List.copyOf(results);
	}
	
	public static AsmCmd create(SimpleScope parent, List<AsmParam> params, String asm, List<AsmResult> results,
		@SuppressWarnings( "unused" ) ErrorContext ctx) {
		return new AsmCmd(parent, params, asm, results);
	}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, @SuppressWarnings("unused") StringBuilder indent) {
		append.append("asm");
		for (AsmParam p : this.params) {
			append.append(" \"").append(p.target).append("\" <-- ").append(p.value);
		}
		append.append(":::").append(asm).append(">>>");
		for (AsmResult r : this.results) {
			if ( r.value == null ) append.append(' ').append(r.target).append(" <-- ?");
			else append.append(' ').append(r.target).append(" <-- \"").append(r.value).append('"');
		}
		append.append(';');
	}
	
	public record AsmParam(String target, SimpleValue value) {
		
		public static AsmParam create(String target, SimpleValue value, ErrorContext ctx) {
			value.type().checkCastable(null, ctx, false);
			return new AsmParam(target, value);
		}
		
	}
	
	public record AsmResult(SimpleValue target, String value) {
		
		public static AsmResult create(SimpleValue value, String target, ErrorContext ctx) {
			value.type().checkCastable(null, ctx, false);
			value.checkAssignable(value.type(), ctx); // the special meaning of null in only guaranteed in checkCastable
			return new AsmResult(value, target);
		}
		
	}
	
}
