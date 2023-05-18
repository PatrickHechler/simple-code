// This file is part of the Simple Code Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

@SuppressWarnings("javadoc")
public class SimpleCommandWhile implements SimpleCommand {
	
	public final SimplePool    pool;
	public final SimpleValue   condition;
	public final SimpleCommandBlock whileCmd;
	
	public SimpleCommandWhile(SimplePool pool, SimpleValue condition, SimpleCommandBlock whileCmd) {
		this.pool      = pool;
		this.condition = condition;
		this.whileCmd  = whileCmd;
	}
	
	public static SimpleCommandWhile create(SimplePool pool, SimpleValue val, SimpleCommandBlock whileCmd) {
		return new SimpleCommandWhile(pool, val, whileCmd);
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString(b, "");
		return b.toString();
	}
	
	@Override
	public void toString(StringBuilder b, String indention) { 
		b.append("while (").append(this.condition).append(") ");
		this.whileCmd.toString(b, indention.concat("\t"));
	 }
	
}
