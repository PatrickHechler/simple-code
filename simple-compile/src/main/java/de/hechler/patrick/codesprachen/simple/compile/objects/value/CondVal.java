package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record CondVal(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue, ErrorContext ctx)
	implements SimpleValue {
	
	public static SimpleValue create(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue,
		ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx, false);
		SimpleType ct = trueValue.type().commonType(falseValue.type(), ctx);
		trueValue  = CastVal.create(trueValue, ct, ctx);
		falseValue = CastVal.create(trueValue, ct, ctx);
		return new CondVal(condition, trueValue, falseValue, ctx);
	}
	
	@Override
	public SimpleType type() {
		return this.trueValue.type();
	}
	
	@Override
	public void checkAssignable(SimpleType type, ErrorContext ctx) {
		this.trueValue.checkAssignable(type, ctx);
		this.falseValue.checkAssignable(type, ctx);
	}
	
	@Override
	public SimpleValue simplify(UnaryOperator<SimpleValue> op) {
		SimpleValue c = op.apply(this.condition);
		if ( c instanceof ScalarNumericVal snv && snv.value() != 0L || c instanceof AddressOfVal
			|| c instanceof DataVal ) {
			return op.apply(this.trueValue);
		} else if ( c instanceof ScalarNumericVal ) {
			return op.apply(this.falseValue);
		}
		return create(c, op.apply(this.trueValue), op.apply(this.falseValue), this.ctx);
	}
	
	@Override
	public String toString() {
		return "(" + this.condition + " ? " + this.trueValue + " : " + this.falseValue + ")";
	}
	
}
