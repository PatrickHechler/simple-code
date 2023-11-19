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
package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;

public record FPNumericVal(NativeType type, double value, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(NativeType type, double value, ErrorContext ctx) {
		if ( type != NativeType.FPNUM && type != NativeType.FPDWORD ) throw new AssertionError("illegal type: " + type);
		if ( type == NativeType.FPDWORD ) value = (float) value;
		return new FPNumericVal(type, value, ctx);
	}
	
	@Override
	public boolean isConstant() { return true; }
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public SimpleValue simplify(@SuppressWarnings("unused") UnaryOperator<SimpleValue> op) { return this; }

	@Override
	public String toString() {
		if ( this.type == NativeType.FPNUM ) return Double.toString(this.value);
		return Float.toString((float) this.value) + "D";
	}

}
