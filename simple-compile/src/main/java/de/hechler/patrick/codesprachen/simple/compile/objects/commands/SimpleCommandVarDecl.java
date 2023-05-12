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
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

public class SimpleCommandVarDecl extends SimpleFunctionVariable implements SimpleCommand {
	
	public final SimplePool pool;
	public SimpleValue      init = null;
	
	public SimpleCommandVarDecl(SimplePool pool, SimpleType type, String name) {
		super(type, name);
		this.pool = pool;
	}
	
	public static SimpleCommandVarDecl create(SimplePool pool, SimpleType t, String text) {
		return new SimpleCommandVarDecl(pool, t, text);
	}
	
	public void initValue(SimpleValue val) {
		assert init == null;
		init = val;
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	@Override
	public int hashCode() {
		return 128 ^ super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) return false;
		else return getClass() == obj.getClass();
	}
	
}
