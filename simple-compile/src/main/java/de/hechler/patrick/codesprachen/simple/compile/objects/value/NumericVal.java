package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;

public record NumericVal(NativeType type, long value, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(NativeType type, long value, ErrorContext ctx) {
		
		return new NumericVal(type, value, ctx);
	}
	
}
