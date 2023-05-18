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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleSubPool;
import de.hechler.patrick.codesprachen.simple.compile.utils.ListStart;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;

@SuppressWarnings("javadoc")
public class SimpleCommandBlock implements SimpleCommand {
	
	public final SimplePool           pool;
	private final List<SimpleCommand> cmds;
	public final List<SimpleCommand>  commands;
	private boolean                   sealed;
	private int                       regMax = -1;
	
	public SimpleCommandBlock(SimplePool pool) {
		this.pool     = pool;
		this.cmds     = new LinkedList<>();
		this.commands = Collections.unmodifiableList(this.cmds);
	}
	
	public SimpleCommandBlock(SimplePool pool, SimpleCommandBlock orig) {
		this.pool     = pool;
		this.cmds     = null;
		this.commands = new ListStart<>(orig.commands, orig.commands.size());
		this.sealed   = true;
	}
	
	@Override
	public SimplePool pool() {
		return this.pool;
	}
	
	public void addCmd(SimpleCommand cmd) {
		if (this.sealed) { throw new IllegalStateException("already sealed (me: " + this + ")"); }
		this.cmds.add(cmd);
	}
	
	public void seal() {
		if (this.sealed) { throw new IllegalStateException("already sealed (me: " + this + ")"); }
		this.sealed = true;
	}
	
	public SimpleCommandBlock snapshot(SimpleSubPool snapshotPool) {
		return new SimpleCommandBlock(snapshotPool, this);
	}
	
	public void initRegMax(int regMax) {
		if (!this.sealed) throw new IllegalStateException("this block still can be modified");
		if (this.regMax != -1) throw new IllegalStateException("this block has its regMax already initilized");
		if (regMax == -1) throw new AssertionError("regMax is -1");
		this.regMax = regMax;
	}
	
	public int regMax() {
		int s = this.regMax;
		if (s == -1) throw new IllegalStateException("this block has its regMax not yet set");
		return s;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString(b, "");
		return b.toString();
	}
	
	@Override
	public void toString(StringBuilder b, String indention) {
		b.append('{');
		if (!this.commands.isEmpty()) {
			String newIndention = indention.concat("\t");
			b.append('\n');
			for (SimpleCommand sc : this.commands) {
				b.append(newIndention);
				sc.toString(b, newIndention);
				b.append('\n');
			}
			b.append(indention);
		}
		b.append('}');
	}
	
}
