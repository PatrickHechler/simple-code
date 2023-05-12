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

import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueConst;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;

public class SimpleConstants {
	
	private SimpleConstants() { }
	
	public static SimpleConstant create(String name, SimpleValue val, boolean export) {
		if ( !val.isConstant()) {
			throw new IllegalArgumentException("the constant '" + name + "' has no constant value: (" + val + ")");
		}
		SimpleValueConst cv = (SimpleValueConst) val;
		if ( !cv.implicitNumber()) {
			throw new IllegalArgumentException("the constant '" + name + "' has no constant (implicit) number value: (" + cv + ")");
		}
		return new SimpleConstant(name, cv.getNumber(), export);
	}
	
}
