package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.BinaryOpVal.BinaryOp;

public class UnaryOpVal {
	
	// no need to give them an error context
	private static final SimpleValue MINUS_ONE = NumericVal.create(NativeType.BYTE, -1L, ErrorContext.NO_CONTEXT);
	private static final SimpleValue ONE       = NumericVal.create(NativeType.UBYTE, 1L, ErrorContext.NO_CONTEXT);
	private static final SimpleValue ZERO      = NumericVal.create(NativeType.UBYTE, 0L, ErrorContext.NO_CONTEXT);
	private static final SimpleValue ZERO_8B   = NumericVal.create(NativeType.UNUM, 0L , ErrorContext.NO_CONTEXT);
	
	public SimpleValue create(UnaryOp op, SimpleValue a, ErrorContext ctx) {
		switch ( op ) {
		case MINUS:
			return BinaryOpVal.create(a, BinaryOp.MATH_MUL, MINUS_ONE, ctx);
		case PLUS:
			return BinaryOpVal.create(a, BinaryOp.MATH_MUL, ONE, ctx);
		case ADDRESSOF:
			break;
		case BIT_NOT:
			return BinaryOpVal.create(a, BinaryOp.BIT_XOR, MINUS_ONE, ctx);
		case BOOL_NOT:
			if ( a.type() instanceof PointerType ) {
				return BinaryOpVal.create(a, BinaryOp.CMP_NEQ, ZERO_8B, ctx);
			}
			return BinaryOpVal.create(a, BinaryOp.CMP_NEQ, ZERO, ctx);
		default:
			throw new AssertionError("unknown Unary Operator: " + op.name());
		}
	}
	
	public enum UnaryOp {
		PLUS, MINUS, ADDRESSOF, BIT_NOT, BOOL_NOT
	}
	
}
