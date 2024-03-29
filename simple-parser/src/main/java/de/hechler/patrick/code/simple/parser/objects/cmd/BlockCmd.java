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
import java.util.List;
import java.util.Set;

import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class BlockCmd extends SimpleCommand {
	
	private final List<SimpleCommand> cmds = new ArrayList<>();
	private boolean                   seal;
	
	public BlockCmd(SimpleScope parent) {
		super(parent);
	}
	
	public void addCmd(SimpleCommand cmd) {
		checkNotSealed();
		this.cmds.add(cmd);
	}
	
	public void seal() {
		checkNotSealed();
		this.seal = true;
	}
	
	private void checkNotSealed() {
		if ( this.seal ) throw new IllegalStateException("sealed");
	}
	
	private void checkSealed() {
		if ( !this.seal ) throw new IllegalStateException("not sealed");
	}
	
	@Override
	@SuppressWarnings("unused")
	public void directAvailableNames(Set<String> add) {}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	public int commandCount() {
		checkSealed();
		return this.cmds.size();
	}
	
	public SimpleCommand command(int i) {
		checkSealed();
		return this.cmds.get(i);
	}
	
	public SimpleScope currentAsParent() {
		return new SimpleScope() {
			
			private final int blkLen = BlockCmd.this.cmds.size();
			
			@Override
			public Set<String> availableNames() {
				return BlockCmd.this.availableNames(blkLen);
			}
			
			@Override
			public SimpleValue nameValueOrNull(String name, ErrorContext ctx) {
				return BlockCmd.this.nameValueOrNull(name, ctx, blkLen);
			}
			
			@Override
			public Object nameTypeOrDepOrFuncOrNull(String typedefName) {
				return BlockCmd.this.nameTypeOrDepOrFuncOrNull(typedefName);
			}
			
		};
	}
	
	protected Set<String> availableNames(int blockLength) {
		Set<String> result = super.parent.availableNames();
		for (int i = 0; i < blockLength; i++) {
			this.cmds.get(i).directAvailableNames(result);
		}
		return result;
	}
	
	private SimpleValue nameValueOrNull(String name, ErrorContext ctx, int blockLength) {
		for (int i = 0; i < blockLength; i++) {
			SimpleValue val = this.cmds.get(i).directNameValueOrNull(name, ctx);
			if ( val != null ) return val;
		}
		return super.parent.nameValueOrNull(name, ctx);
	}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleValue nameValueOrNull(String name, ErrorContext ctx) {
		throw new AssertionError("nameValue(String,ErrCtx) called on BlockCmd (there should be [BLOCK]:[NAME])!");
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		if ( this.cmds.isEmpty() ) {
			append.append("{ }");
			return;
		}
		append.append("{\n");
		indent.append("    ");
		for (SimpleCommand c : cmds) {
			append.append(indent);
			c.toString(append, indent);
			append.append('\n');
		}
		indent.replace(indent.length() - 4, indent.length(), "");
		append.append(indent).append('}');
	}
	
}
