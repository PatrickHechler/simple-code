package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record ScalarNumericVal(SimpleType type, long value, ErrorContext ctx) implements SimpleValue {
	
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
