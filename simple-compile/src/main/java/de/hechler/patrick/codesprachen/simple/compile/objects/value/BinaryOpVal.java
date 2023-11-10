package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record BinaryOpVal(SimpleValue a, BinaryOp op, SimpleValue b, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleValue a, BinaryOp op, SimpleValue b, ErrorContext ctx) {
		switch ( op.type ) {
		case BIT -> {
			SimpleType t = a.type().commonType(b.type(), ctx);
			checkNoPntrScalarNumeric(t, ctx);
			a = CastVal.create(a, t, ctx);
			b = CastVal.create(b, t, ctx);
		}
		case BOOL -> {
			SimpleType t = a.type().commonType(b.type(), ctx);
			checkScalarNumeric(t, ctx);
			a = CastVal.create(a, t, ctx);
			b = CastVal.create(b, t, ctx);
		}
		case CMP -> {
			SimpleType t = a.type().commonType(b.type(), ctx);
				checkNumeric(t, ctx);
			a = CastVal.create(a, t, ctx);
			b = CastVal.create(b, t, ctx);
		}
		case MATH_ADDSUB -> {
			if ( a.type() instanceof PointerType ) {
				checkScalarNumeric(b.type(), ctx);
			} else {
				SimpleType t = a.type().commonType(b.type(), ctx);
				checkNoPntrNumeric(t, ctx);
				a = CastVal.create(a, t, ctx);
				b = CastVal.create(b, t, ctx);
			}
		}
		case MATH -> {
			SimpleType t = a.type().commonType(b.type(), ctx);
			checkNoPntrNumeric(t, ctx);
			a = CastVal.create(a, t, ctx);
			b = CastVal.create(b, t, ctx);
		}
		case SHIFT -> {
			checkNoPntrScalarNumeric(a.type(), ctx);
			checkNoPntrScalarNumeric(b.type(), ctx);
		}
		default -> throw new AssertionError("illegal BinaryOperator type: " + op.type.name());
		}
		return new BinaryOpVal(a, op, b, ctx);
	}
	
	private static void checkNumeric(SimpleType t, ErrorContext ctx) {
		if ( t instanceof NativeType ) {
			return;
		}
		if ( t instanceof PointerType ) {
			return;
		}
		SimpleType.castErrImplicit(t, "a numeric type", ctx);
	}
	
	private static void checkNoPntrNumeric(SimpleType t, ErrorContext ctx) {
		if ( t instanceof NativeType ) {
			return;
		}
		SimpleType.castErrImplicit(t, "a non pointer numeric type", ctx);
	}
	
	private static void checkScalarNumeric(SimpleType t, ErrorContext ctx) {
		if ( t instanceof NativeType nta && nta != NativeType.FPNUM && nta != NativeType.FPDWORD ) {
			return;
		}
		if ( t instanceof PointerType ) {
			return;
		}
		SimpleType.castErrImplicit(t, "a scalar numeric type", ctx);
	}
	
	private static void checkNoPntrScalarNumeric(SimpleType t, ErrorContext ctx) {
		if ( !( t instanceof NativeType nta ) || nta == NativeType.FPNUM || nta == NativeType.FPDWORD ) {
			SimpleType.castErrImplicit(t, "a non pointer scalar numeric type", ctx);
		}
	}
	
	@Override
	public SimpleType type() {
		if (this.op.type == BinaryOpType.CMP) return NativeType.UBYTE;
		return this.a.type();
	}
	
	public enum BinaryOpType {
		BOOL, BIT, CMP, MATH_ADDSUB, MATH, SHIFT,
	}
	
	public enum BinaryOp {//@formatter:off
		BOOL_OR(BinaryOpType.BOOL),
		BOOL_AND(BinaryOpType.BOOL),
		BIT_OR(BinaryOpType.BIT),
		BIT_XOR(BinaryOpType.BIT),
		BIT_AND(BinaryOpType.BIT),
		CMP_EQ(BinaryOpType.CMP),
		CMP_NEQ(BinaryOpType.CMP),
		CMP_GT(BinaryOpType.CMP),
		CMP_GE(BinaryOpType.CMP),
		CMP_LT(BinaryOpType.CMP),
		CMP_LE(BinaryOpType.CMP),
		SHIFT_LEFT(BinaryOpType.SHIFT),
		SHIFT_LOGIC_RIGTH(BinaryOpType.SHIFT),
		SHIFT_ARITMETIC_RIGTH(BinaryOpType.SHIFT),
		MATH_ADD(BinaryOpType.MATH_ADDSUB),
		MATH_SUB(BinaryOpType.MATH_ADDSUB),
		MATH_MUL(BinaryOpType.MATH),
		MATH_DIV(BinaryOpType.MATH),
		MATH_MOD(BinaryOpType.MATH),
		;//@formatter:on
		
		public final BinaryOpType type;
		
		private BinaryOp(BinaryOpType type) {
			this.type = type;
		}
		
	}
	
}
