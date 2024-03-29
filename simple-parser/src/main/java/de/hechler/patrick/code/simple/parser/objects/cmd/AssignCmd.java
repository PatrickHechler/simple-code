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

import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class AssignCmd extends SimpleCommand {
	
	public final SimpleValue target;
	public final SimpleValue value;
	
	private AssignCmd(SimpleScope parent, SimpleValue target, SimpleValue value) {
		super(parent);
		this.target = target;
		this.value = value;
	}
	
	public static AssignCmd create(SimpleScope parent, SimpleValue target, SimpleValue value, ErrorContext ctx) {
		target.checkAssignable(value.type(), ctx);
		return new AssignCmd(parent, target, value);
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
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append(this.target).append(" <-- ").append(this.value).append(';');
	}
	
}
