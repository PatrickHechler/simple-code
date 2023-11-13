package de.hechler.patrick.codesprachen.simple.compile.objects.value;

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
	public SimpleValue simplify() {
		SimpleValue v = this.value.simplify();
		if ( !v.isConstant() && v == value ) return this;
		switch ( v ) {
		case FPNumericVal fpn:
			if ( type == NativeType.FPNUM || type == NativeType.FPDWORD ) {
				return FPNumericVal.create((NativeType) type, fpn.value(), ctx);
			}
			return ScalarNumericVal.createAllowTruncate(type, (long) fpn.value(), ctx);
		case ScalarNumericVal snv:
			if ( type != NativeType.FPNUM && type != NativeType.FPDWORD ) {
				return ScalarNumericVal.createAllowTruncate(type, snv.value(), ctx);
			}
			return FPNumericVal.create((NativeType) type, snv.value(), ctx);
		case AddressOfVal aov: // be careful with direct construct invocations
			return new AddressOfVal(aov.a(), type, ctx);
		case DataVal dv:
			return new DataVal(dv.value(), type, ctx);
		case FunctionVal fv:
			return new FunctionVal(fv.func(), type, ctx);
		default:
		}
		return create(v, type, ctx);
	}
	
	@Override
	public String toString() {
		return "((" + this.type + ") " + this.value + " )";
	}
	
}
