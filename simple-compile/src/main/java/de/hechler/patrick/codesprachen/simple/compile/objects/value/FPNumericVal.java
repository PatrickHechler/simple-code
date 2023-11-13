package de.hechler.patrick.codesprachen.simple.compile.objects.value;

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
	public String toString() {
		if ( type == NativeType.FPNUM ) return Double.toString(this.value);
		return Float.toString((float) this.value) + "D";
	}
	
}
