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
package de.hechler.patrick.code.simple.parser.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;

public record ScalarNumericVal(SimpleType type, long value, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue createAllowTruncate(SimpleType type, long value, ErrorContext ctx) throws CompileError {
		switch ( type ) {
		case NativeType.UNUM, NativeType.NUM -> {}
		case NativeType.UDWORD -> value = Integer.toUnsignedLong((int) value);
		case NativeType.UWORD -> value = Short.toUnsignedLong((short) value);
		case NativeType.UBYTE -> value = Byte.toUnsignedLong((byte) value);
		case NativeType.DWORD -> value = (int) value;
		case NativeType.WORD -> value = (short) value;
		case NativeType.BYTE -> value = (byte) value;
		case NativeType.FPDWORD, NativeType.FPNUM -> throw new AssertionError();
		case PointerType pt -> {}
		case SimpleType st -> System.err.println("[WARN]: there is some crazy number type: " + st);
		}
		return new ScalarNumericVal(type, value, ctx);
	}
	
	public static SimpleValue create(SimpleType type, long value, ErrorContext ctx) throws CompileError {
		switch ( type ) {
		case NativeType.UNUM, NativeType.NUM -> {}
		case NativeType.UDWORD -> checkEq(type, value, Integer.toUnsignedLong((int) value), ctx);
		case NativeType.UWORD -> checkEq(type, value, Short.toUnsignedLong((short) value), ctx);
		case NativeType.UBYTE -> checkEq(type, value, Byte.toUnsignedLong((byte) value), ctx);
		case NativeType.DWORD -> checkEq(type, value, (int) value, ctx);
		case NativeType.WORD -> checkEq(type, value, (short) value, ctx);
		case NativeType.BYTE -> checkEq(type, value, (byte) value, ctx);
		case NativeType.FPDWORD, NativeType.FPNUM -> throw new AssertionError();
		case PointerType pt -> {}
		case SimpleType st -> System.err.println("[WARN]: there is some crazy number type: " + st);
		}
		return new ScalarNumericVal(type, value, ctx);
	}
	
	private static void checkEq(SimpleType type, long a, long b, ErrorContext ctx) throws CompileError {
		if ( a != b ) {
			throw new CompileError(ctx,
				"the value " + a + " [UHEX-" + Long.toHexString(a) + "] can not be represented by the type: " + type);
		}
	}
	
	@Override
	public boolean isConstant() { return true; }
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public SimpleValue simplify(@SuppressWarnings("unused") UnaryOperator<SimpleValue> op) { return this; }

	@Override
	public String toString() {
		return switch ( this.type ) {
		case NativeType.UNUM -> Long.toUnsignedString(this.value);
		case NativeType.NUM -> Long.toString(this.value) + "S";
		case NativeType.UDWORD -> Integer.toUnsignedString((int) this.value) + "D";
		case NativeType.DWORD -> Integer.toString((int) this.value) + "SD";
		case NativeType.UWORD -> Integer.toUnsignedString((int) this.value) + "W";
		case NativeType.WORD -> Short.toString((short) this.value) + "SW";
		case NativeType.UBYTE -> Integer.toUnsignedString((int) this.value) + "B";
		case NativeType.BYTE -> Byte.toString((byte) this.value) + "SB";
		case PointerType pt -> "( (" + pt + ") UHEX-" + Long.toHexString(value) + ")";
		case SimpleType st -> "( (" + st + ") " + value + ")";
		};
	}
	
}
