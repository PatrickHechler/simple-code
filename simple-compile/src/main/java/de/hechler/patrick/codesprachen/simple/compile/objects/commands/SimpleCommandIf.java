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

public class SimpleCommandIf implements SimpleCommand {
	
	public final SimplePool    pool;
	public final SimpleValue   condition;
	public final SimpleCommand ifCmd;
	public final SimpleCommand elseCmd;
	
	public SimpleCommandIf(SimplePool pool, SimpleValue condition, SimpleCommand ifCmd, SimpleCommand elseCmd) {
		this.pool      = pool;
		this.condition = condition;
		this.ifCmd     = ifCmd;
		this.elseCmd   = elseCmd;
	}
	
	public static SimpleCommandIf create(SimplePool pool, SimpleValue val, SimpleCommand cmd, SimpleCommand elseCmd) {
		return new SimpleCommandIf(pool, val, cmd, elseCmd);
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
		String newIndention = indention.concat("\t");
		b.append("if (").append(this.condition).append(") ");
		this.ifCmd.toString(b, newIndention);
		if (this.elseCmd != null) {
			b.append('\n').append(indention).append("else ");
			this.elseCmd.toString(b, newIndention);
		}
	}
	
}
