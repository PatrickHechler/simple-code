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
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.VariableVal;

public class VarDeclCmd extends SimpleCommand {
	
	public final SimpleVariable sv;
	
	private VarDeclCmd(SimpleScope parent, SimpleVariable sv) {
		super(parent);
		this.sv = sv;
	}
	
	public static VarDeclCmd create(SimpleScope parent, SimpleVariable sv, ErrorContext ctx) {
		if ( ( sv.flags() & SimpleVariable.FLAG_EXPORT ) != 0 ) {
			throw new AssertionError(ctx);
		}
		return new VarDeclCmd(parent, sv);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		if ( sv.name().equals(name) ) {
			return VariableVal.create(sv, ctx);
		}
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, @SuppressWarnings( "unused" ) StringBuilder indent) {
		if ( ( sv.flags() & SimpleVariable.FLAG_CONSTANT ) != 0 ) {
			append.append("const ");
		}
		if ( ( sv.flags() & SimpleVariable.FLAG_EXPORT ) != 0 ) {
			append.append("exp ");
		}
		append.append(sv.type()).append(' ').append(sv.name());
		if ( sv.initialValue() != null ) {
			append.append(" <-- ").append(sv.initialValue());
		}
		append.append(';');
	}
	
}
