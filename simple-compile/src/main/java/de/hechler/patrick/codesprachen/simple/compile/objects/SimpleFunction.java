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
package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Arrays;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleFuncPool;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunctionSymbol;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;

public class SimpleFunction extends SimpleFunctionSymbol {
	
	public final boolean            main;
	public final SimpleCommandBlock body;
	public final SimpleFuncPool     pool;
	
	public SimpleFunction(boolean export, boolean main, String name, SimpleCommandBlock cmd, SimpleFuncPool pool) {
		this(-1L, null, export, main, name, type(pool), cmd, pool);
	}
	
	public SimpleFunction(long address, Object relative, String name, SimpleFuncType type) {
		this(address, relative, true, false, name, type, null, null);
	}
	
	private SimpleFunction(long address, Object relative, boolean export, boolean main, String name, SimpleFuncType type, SimpleCommandBlock cmd,
		SimpleFuncPool pool) {
		super(address, relative, export, name, type);
		this.main = main;
		this.body = cmd;
		this.pool = pool;
	}
	
	private static SimpleFuncType type(SimpleFuncPool pool) {
		return new SimpleFuncType(Arrays.asList(convert(pool.myargs)), Arrays.asList(convert(pool.myresults)));
	}
	
	private static SimpleOffsetVariable[] convert(SimpleFunctionVariable[] arr) {
		SimpleOffsetVariable[] res = new SimpleOffsetVariable[arr.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = new SimpleOffsetVariable(arr[i].type, arr[i].name);
		}
		return res;
	}
	
	@Override
	public boolean isExport() {
		return export;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public String toExportString() {
		if (!export) {
			throw new IllegalStateException("this is not marked as export!");
		}
		StringBuilder b = new StringBuilder();
		b.append(FUNC);
		b.append(Long.toHexString(super.address()).toUpperCase());
		b.append(FUNC);
		b.append(this.name);
		type.appendToExportStr(b);
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return type.hashCode();
	}
	
	@Override
	public SimpleExportable changeRelative(Object relative) {
		if (super.address() == -1) { throw new AssertionError("address is not initilized!"); }
		if (super.relative() == null) { throw new AssertionError("relative is not initilized!"); }
		return new SimpleFunction(super.address(), relative, super.export, this.main, super.name, super.type, this.body, this.pool);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SimpleFunction other = (SimpleFunction) obj;
		return type.equals(other.type);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("func ");
		if (export) {
			b.append("exp ");
		}
		return b.append(name).append(type).append(' ').append(this.body).toString();
	}
	
}
