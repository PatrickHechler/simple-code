package de.hechler.patrick.codesprachen.simple.interpreter.java;

import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;

public sealed interface ConstantValue {
	
	SimpleType type();
	
	record DataValue(SimpleType type, long address) implements ConstantValue {}
	
	record ScalarValue(SimpleType type, long value) implements ConstantValue {
		
		public static ScalarValue ONE = new ScalarValue(NativeType.UBYTE, 1L);
		public static ScalarValue ZERO = new ScalarValue(NativeType.UBYTE, 0L);
		
		public ScalarValue(SimpleType type, long value) {
			this.type = type;
			switch ( type ) {
			case PointerType _pt -> this.value = value; // NOSONAR
			case NativeType.NUM, NativeType.UNUM -> this.value = value;
			case NativeType.DWORD -> this.value = (int) value;
			case NativeType.UDWORD -> this.value = 0xFFFFFFFFL & value;
			case NativeType.WORD -> this.value = (short) value;
			case NativeType.UWORD -> this.value = 0xFFFFL & value;
			case NativeType.BYTE -> this.value = (byte) value;
			case NativeType.UBYTE -> this.value = 0xFFL & value;
			default -> throw new AssertionError("unknown/illegal type class: " + type.getClass());
			}
		}
		
	}
	
	record FPValue(SimpleType type, double value) implements ConstantValue {
		
		public FPValue(SimpleType type, double value) {
			if ( type == NativeType.FPNUM ) {
				this.type = type;
				this.value = value;
				return;
			}
			if ( type == NativeType.FPDWORD ) {
				this.type = type;
				this.value = (float) value;
				return;
			}
			throw new IllegalArgumentException("illegal type: " + type);
		}
		
	}
	
}
