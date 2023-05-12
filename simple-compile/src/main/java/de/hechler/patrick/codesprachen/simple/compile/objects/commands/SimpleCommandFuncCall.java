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
package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

@SuppressWarnings("javadoc")
public class SimpleCommandFuncCall implements SimpleCommand {
	
	public final SimplePool  pool;
	public final String      firstName;
	public final String      secondName;
	public final SimpleValue function;
	
	public SimpleCommandFuncCall(SimplePool pool, String firstName, String secondName, SimpleValue function) {
		this.pool = pool;
		this.firstName = firstName;
		this.secondName = secondName;
		this.function = function;
	}
	
	public static SimpleCommandFuncCall create(SimplePool pool, String fn, String sn, SimpleValue val) {
		if ( !val.type().isFunc()) {
			throw new IllegalArgumentException("a function call needs a function structure as argument! (arg-type: " + val.type() + " arg: '" + val + "')");
		}
		return new SimpleCommandFuncCall(pool, fn, sn, val);
	}
	
	@Override
	public SimplePool pool() {
		return this.pool;
	}
	
}
