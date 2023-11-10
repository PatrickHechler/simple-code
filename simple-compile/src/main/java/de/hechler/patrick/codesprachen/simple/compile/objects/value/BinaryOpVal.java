package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record BinaryOpVal(SimpleValue a, BinaryOp op, SimpleValue b, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleValue a, BinaryOp op, SimpleValue b, ErrorContext ctx) {
		if (a instanceof PointerType) {
		}
		return new BinaryOpVal(a, op, b, ctx);
	}
	
	@Override
	public SimpleType type() {
		return this.a.type();
	}
	
	public enum BinaryOp {
		BOOL_OR,
		BOOL_AND,
		BIT_OR,
		BIT_XOR,
		BIT_AND,
		CMP_EQ,
		CMP_NEQ,
		CMP_GT,
		CMP_GE,
		CMP_LT,
		CMP_LE,
		SHIFT_LEFT,
		SHIFT_LOGIC_RIGTH,
		SHIFT_ARITMETIC_RIGTH,
		MATH_ADD,
		MATH_SUB,
		MATH_MUL,
		MATH_DIV,
		MATH_MOD,
	}
	
}
