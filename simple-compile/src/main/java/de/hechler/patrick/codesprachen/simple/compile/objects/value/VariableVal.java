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
	
	@Override
	public void checkAssignable(SimpleType type, ErrorContext ctx) {
		type.checkCastable(sv.type(), ctx, false);
	}
	
	@Override
	public boolean isConstant() { return (sv.flags() & SimpleVariable.FLAG_CONSTANT) != 0; }
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public String toString() {
		return sv.name();
	}
	
}
