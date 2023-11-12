package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;

public record ScalarNumericVal(NativeType type, long value, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(NativeType type, long value, ErrorContext ctx) throws CompileError {
		switch ( type ) {
		case UNUM, NUM -> {}
		case UDWORD -> checkEq(type, value, Integer.toUnsignedLong((int) value), ctx);
		case UWORD -> checkEq(type, value, Short.toUnsignedLong((short) value), ctx);
		case UBYTE -> checkEq(type, value, Byte.toUnsignedLong((byte) value), ctx);
		case DWORD -> checkEq(type, value, (int) value, ctx);
		case WORD -> checkEq(type, value, (short) value, ctx);
		case BYTE -> checkEq(type, value, (byte) value, ctx);
		case FPDWORD, FPNUM -> throw new AssertionError();
		}
		return new ScalarNumericVal(type, value, ctx);
	}
	
	private static void checkEq(NativeType type, long a, long b, ErrorContext ctx) throws CompileError {
		if ( a != b ) {
			throw new CompileError(ctx,
				"the value " + a + " [UHEX-" + Long.toHexString(a) + "] can not be represented by the type: " + type);
		}
	}
	
	@Override
	public String toString() {
		return switch ( this.type ) {
		case UNUM -> Long.toUnsignedString(this.value);
		case NUM -> Long.toString(this.value) + "S";
		case UDWORD -> Integer.toUnsignedString((int) this.value) + "D";
		case DWORD -> Integer.toString((int) this.value) + "SD";
		case UWORD -> Integer.toUnsignedString((int) this.value) + "W";
		case WORD -> Short.toString((short) this.value) + "SW";
		case UBYTE -> Integer.toUnsignedString((int) this.value) + "B";
		case BYTE -> Byte.toString((byte) this.value) + "SB";
		case FPDWORD, FPNUM -> throw new AssertionError();
		};
	}
	
}
