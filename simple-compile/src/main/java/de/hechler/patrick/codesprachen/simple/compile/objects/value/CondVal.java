package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record CondVal(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue, ErrorContext ctx)
	implements SimpleValue {
	
	public static SimpleValue create(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue,
		ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx);
		SimpleType ct = trueValue.type().commonType(falseValue.type(), ctx);
		trueValue  = CastVal.create(trueValue, ct, ctx);
		falseValue = CastVal.create(trueValue, ct, ctx);
		return new CondVal(condition, trueValue, falseValue, ctx);
	}
	
	@Override
	public SimpleType type() {
		return this.trueValue.type();
	}
	
}