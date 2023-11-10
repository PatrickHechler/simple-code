package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record CondVal(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue, ErrorContext ctx) implements SimpleValue {
	
	public CondVal {
		condition.type().checkCastable(NativeType.UNUM, ctx);
		trueValue.type().commonType(falseValue.type(), ctx);
	}
	
	@Override
	public SimpleType type() {
		return this.trueValue.type().commonType(this.falseValue.type(), this.ctx);
	}
	
}
