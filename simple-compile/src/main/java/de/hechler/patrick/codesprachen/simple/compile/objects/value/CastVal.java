package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record CastVal(SimpleValue value, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleValue value, SimpleType type, ErrorContext ctx) {
		if ( value.type() == type ) {
			return value;
		}
		value.type().checkCastable(type, ctx, true);
		return new CastVal(value, type, ctx);
	}
	
	@Override
	public boolean isConstant() { return this.value.isConstant(); }
	
	@Override
	public SimpleValue simplify(UnaryOperator<SimpleValue> op) {
		SimpleValue v = op.apply(this.value);
		if ( !v.isConstant() && v == this.value ) return this;
		switch ( v ) {
		case FPNumericVal fpn:
			if ( this.type == NativeType.FPNUM || this.type == NativeType.FPDWORD ) {
				return FPNumericVal.create((NativeType) this.type, fpn.value(), this.ctx);
			}
			return ScalarNumericVal.createAllowTruncate(this.type, (long) fpn.value(), this.ctx);
		case ScalarNumericVal snv:
			if ( this.type != NativeType.FPNUM && this.type != NativeType.FPDWORD ) {
				return ScalarNumericVal.createAllowTruncate(this.type, snv.value(), this.ctx);
			}
			return FPNumericVal.create((NativeType) this.type, snv.value(), this.ctx);
		case AddressOfVal aov: // be careful with direct construct invocations
			return new AddressOfVal(aov.a(), this.type, this.ctx);
		case DataVal dv:
			return new DataVal(dv.value(), this.type, this.ctx);
		case FunctionVal fv:
			return new FunctionVal(fv.func(), this.type, this.ctx);
		default:
		}
		return create(v, this.type, this.ctx);
	}
	
	@Override
	public String toString() {
		return "((" + this.type + ") " + this.value + " )";
	}
	
}
