package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record AddressOfVal(SimpleValue a, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleValue a, ErrorContext ctx) {
		if ( a instanceof ScalarNumericVal ) {
			throw new CompileError(ctx, "can not calculate the runtime address of a numeric constant: " + a);
		}
		return new AddressOfVal(a, PointerType.create(a.type(), ctx), ctx);
	}
	
	@Override
	public String toString() {
		return "( &" + this.a + " )";
	}
	
}
