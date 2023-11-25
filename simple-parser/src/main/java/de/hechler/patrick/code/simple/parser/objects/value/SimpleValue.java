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
import de.hechler.patrick.code.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;

public interface SimpleValue {
	
	SimpleType type();
	
	default SimpleValue simplify() {
		return simplify(simplyfyOperator());
	}
	
	static UnaryOperator<SimpleValue> simplyfyOperator() {
		return v -> v.simplify(simplyfyOperator());
	}
	
	SimpleValue simplify(UnaryOperator<SimpleValue> simpifier);
	
	default SimpleValue superSimplify() {
		SimpleValue res = simplify(simplyfyOperator());
		if ( res instanceof DataVal dv && dv.deref() ) {
			return switch ( type() ) {// no need to extend the 64-bit unum to a 64-bit value
			case NativeType.NUM, NativeType.UNUM -> _SSH.toScalar(this, dv, 8, false);
			case NativeType.FPNUM -> _SSH.toFP(this, dv, 8);
			case NativeType.FPDWORD -> _SSH.toFP(this, dv, 4);
			case NativeType.DWORD -> _SSH.toScalar(this, dv, 4, false);
			case NativeType.UDWORD -> _SSH.toScalar(this, dv, 4, true);
			case NativeType.WORD -> _SSH.toScalar(this, dv, 2, false);
			case NativeType.UWORD -> _SSH.toScalar(this, dv, 2, true);
			case NativeType.BYTE -> _SSH.toScalar(this, dv, 2, false);
			case NativeType.UBYTE -> _SSH.toScalar(this, dv, 2, true);
			case /* NOSONAR */ PointerType _a -> _SSH.toScalar(this, dv, 8, false);
			case /* NOSONAR */ ArrayType _a -> _SSH.toScalar(this, dv, 8, false);
			case /* NOSONAR */ SimpleType _a -> res;
			};
		}
		return res;
	}
	
	static class _SSH {// NOSONAR
		
		private _SSH() {}
		
		private static SimpleValue toScalar(SimpleValue orig, DataVal dv, int len, boolean signExtend) {
			if ( invalid(orig, dv, len) ) {
				return dv;
			}
			long value = toLong(dv, len, signExtend);
			return ScalarNumericVal.create(orig.type(), value, ErrorContext.NO_CONTEXT);
		}
		
		private static SimpleValue toFP(SimpleValue orig, DataVal dv, int len) {
			if ( invalid(orig, dv, len) ) {
				return dv;
			}
			long   value = toLong(dv, len, false);
			double fpVal = len == 4 ? Float.intBitsToFloat((int) value) : Double.longBitsToDouble(value);
			return FPNumericVal.create((NativeType) orig.type(), fpVal, ErrorContext.NO_CONTEXT);
		}
		
		private static long toLong(DataVal dv, int len, boolean signExtend) {
			long   value = 0;
			byte[] data  = dv.orig().value();
			int    off   = (int) dv.off();
			for (int i = 0; i < len; i++) {
				value |= ( 0xFFL & data[off + i] ) << ( i << 3 );
			}
			if ( signExtend ) {// value & bit(( len * 8 ) - 1)
				int highestBit = 1 << ( ( len << 3 ) - 1 );
				if ( ( value & highestBit ) != 0 ) {
					value |= ~( highestBit - 1 );
				}
			}
			return value;
		}
		
		private static boolean invalid(SimpleValue orig, DataVal dv, int len) {
			if ( dv.off() < 0 || dv.orig().value().length - len <= dv.off() ) {
				System.err.println("[WARNING]: attempt to dereference an array outside of its bounds: " + orig.ctx());
				System.err.println("           value: " + orig);
				System.err.println("           simplified-value: " + dv);
				System.err.println("           offset: " + dv.off());
				System.err.println("           array-length (in bytes): " + dv.orig().value().length);
				System.err.println("           deref-block-length (in bytes): " + len);
				System.err.println("           deref-block (in bytes): " + dv.off() + " .. " + ( dv.off() + len ));
				return true;
			}
			return false;
		}
		
	}
	
	ErrorContext ctx();
	
	default void checkAssignable(@SuppressWarnings("unused") SimpleType type, ErrorContext ctx) throws CompileError {
		throw new CompileError(ctx, "this value (" + this + ") is not assignable");
	}
	
	// false positives are better than false negatives
	// false negatives lead to non optimized values
	// false positives can lead to some wasted calculation power (but the same result)
	// if it is hard to calculate if this is a constant and it would be again needed when the value should be optimized,
	// just return true
	// also some values are in only some ways constant, then also return true (for example a string has no known runtime
	// address but known content)
	default boolean isConstant() {
		return false;
	}
	
}
