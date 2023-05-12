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
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

@SuppressWarnings("javadoc")
public class SimpleCommandAssign implements SimpleCommand {
	
	public final SimplePool  pool;
	public final SimpleValue target;
	public final SimpleValue value;
	
	public SimpleCommandAssign(SimplePool pool, SimpleValue target, SimpleValue value) {
		this.pool   = pool;
		this.target = target;
		this.value  = value;
	}
	
	public static SimpleCommandAssign create(SimplePool pool, SimpleValue target, SimpleValue value) {
		SimpleType t = target.type();
		if (!t.equals(value.type())) {
			throw new IllegalStateException(
				"target and value type are different! target.type: " + t + " value.type: " + value.type() + " target: '" + target + "' value: '" + value + "'");
		}
		if (!t.isPrimitive() && !t.isPointer()) {
			throw new IllegalStateException("can not assign with array/structure values! (target: " + target + " value: " + value + ")");
		}
		return new SimpleCommandAssign(pool, target, value);
	}
	
	@Override
	public SimplePool pool() {
		return this.pool;
	}
	
}
