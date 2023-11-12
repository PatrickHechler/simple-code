package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record CastVal(SimpleValue value, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleValue value, SimpleType type, ErrorContext ctx) {
		if ( value.type() == type ) {
			return value;
		}
		value.type().checkCastable(type, ctx, false);
		return new CastVal(value, type, ctx);
	}
	
	@Override
	public String toString() {
		return "((" + this.type + ") " + this.value + " )";
	}
	
}
