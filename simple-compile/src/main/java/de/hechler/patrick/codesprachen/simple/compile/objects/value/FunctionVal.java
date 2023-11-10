package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record FunctionVal(SimpleFunction func, SimpleType type, ErrorContext ctx) implements SimpleValue { 

	public static SimpleValue create(SimpleFunction func, ErrorContext ctx) {
		return new FunctionVal(func, PointerType.create(func.type(), ctx), ctx);
	}

}
