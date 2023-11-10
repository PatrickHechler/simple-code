package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record VariableVal(SimpleVariable sv, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleVariable sv, ErrorContext ctx) {
		return new VariableVal(sv, ctx);
	}
	
	@Override
	public SimpleType type() {
		return this.sv.type();
	}
	
}
