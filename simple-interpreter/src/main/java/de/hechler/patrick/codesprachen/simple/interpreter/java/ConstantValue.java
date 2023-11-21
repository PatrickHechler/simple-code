package de.hechler.patrick.codesprachen.simple.interpreter.java;

import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;

public sealed interface ConstantValue {
	
	SimpleType type();
	
	record DataValue(SimpleType type, long address) implements ConstantValue {}
	
	record ScalarValue(SimpleType type, long value) implements ConstantValue {
		
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
	
	record FP64Value(double value) implements ConstantValue {
		
		@Override
		public SimpleType type() {
			return NativeType.FPNUM;
		}
		
	}
	
	record FP32Value(float value) implements ConstantValue {
		@Override
		public SimpleType type() {
			return NativeType.FPDWORD;
		}
	}
	
}
