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

public class SimpleCommandWhile implements SimpleCommand {
	
	public final SimplePool    pool;
	public final SimpleValue   condition;
	public final SimpleCommand whileCmd;
	
	public SimpleCommandWhile(SimplePool pool, SimpleValue condition, SimpleCommand whileCmd) {
		this.pool = pool;
		this.condition = condition;
		this.whileCmd = whileCmd;
	}
	
	public static SimpleCommandWhile create(SimplePool pool, SimpleValue val, SimpleCommand whileCmd) {
		return new SimpleCommandWhile(pool, val, whileCmd);
	}

	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
