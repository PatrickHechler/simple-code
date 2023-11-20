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
package de.hechler.patrick.codesprachen.simple.parser.objects.value;

import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.BinaryOpVal.BinaryOp;

public class UnaryOpVal {
	
	// no need to give them an error context
	private static final SimpleValue MINUS_ONE = ScalarNumericVal.create(NativeType.BYTE, -1L, ErrorContext.NO_CONTEXT);
	private static final SimpleValue ONE       = ScalarNumericVal.create(NativeType.UBYTE, 1L, ErrorContext.NO_CONTEXT);
	private static final SimpleValue ZERO      = ScalarNumericVal.create(NativeType.UBYTE, 0L, ErrorContext.NO_CONTEXT);
	private static final SimpleValue ZERO_8B   = ScalarNumericVal.create(NativeType.UNUM, 0L, ErrorContext.NO_CONTEXT);
	
	public static SimpleValue create(UnaryOp op, SimpleValue a, ErrorContext ctx) {
		switch ( op ) {
		case MINUS:
			return BinaryOpVal.create(a, BinaryOp.MATH_MUL, MINUS_ONE, ctx);
		case PLUS:
			return BinaryOpVal.create(a, BinaryOp.MATH_MUL, ONE, ctx);
		case ADDRESS_OF:
			return AddressOfVal.create(a, ctx);
		case BIT_NOT:
			return BinaryOpVal.create(a, BinaryOp.BIT_XOR, MINUS_ONE, ctx);
		case BOOL_NOT:
			if ( a.type() instanceof PointerType ) {
				return BinaryOpVal.create(a, BinaryOp.CMP_NEQ, ZERO_8B, ctx);
			}
			return BinaryOpVal.create(a, BinaryOp.CMP_NEQ, ZERO, ctx);
		case DEREF_PNTR:
			return BinaryOpVal.create(a, BinaryOp.ARR_PNTR_INDEX, ZERO, ctx);
		default:
			throw new AssertionError("unknown Unary Operator: " + op.name());
		}
	}
	
	public enum UnaryOp {
		PLUS, MINUS, ADDRESS_OF, BIT_NOT, BOOL_NOT, DEREF_PNTR,
	}
	
}
