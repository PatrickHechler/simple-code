package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.ArrayType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.StructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.BinaryOpVal.BinaryOp;

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
		case ARR_PNTR_INDEX -> {
			checkArrPntr(a.type(), ctx);
			checkNoPntrScalarNumeric(b.type(), ctx);
		}
		case DEREF_BY_NAME -> {
			String name = ( (NameVal) b ).name();
			if ( a instanceof DependencyVal dv ) {
				return dv.dep().nameValueOrErr(name, ctx);
			} else if ( a.type() instanceof StructType st ) {
				st.checkHasMember(name, ctx);
			} else if ( a.type() instanceof FuncType ft ) {
				ft.checkHasMember(name, ctx, false);
			} else {
				SimpleType.castErrImplicit(a.type(), "something where I can dereference with a name", ctx);
			}
		}
		default -> throw new AssertionError("illegal BinaryOperator type: " + op.type.name());
		}
		return new BinaryOpVal(a, op, b, ctx);
	}
	
	private static void checkArrPntr(SimpleType t, ErrorContext ctx) {
		if ( t instanceof PointerType ) {
			return;
		}
		if ( t instanceof ArrayType ) {
			return;
		}
		SimpleType.castErrImplicit(t, "a pointer type", ctx);
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
		if ( this.op.type == BinaryOpType.CMP ) return NativeType.UBYTE;
		if ( this.op.type == BinaryOpType.ARR_PNTR_INDEX ) {
			if ( this.a.type() instanceof PointerType pt ) {
				return pt.target();
			}
			if ( this.a.type() instanceof ArrayType at ) {
				return at.target();
			}
			throw new AssertionError(
				"binary operator array/pointer index, but the first operants type is no array/pointer: " + this);
		}
		return this.a.type();
	}
	
	@Override
	public void checkAssignable(SimpleType type, ErrorContext ctx) throws CompileError {
		final SimpleType at = this.a.type();
		SimpleType       target;
		if ( this.op == BinaryOp.DEREF_BY_NAME ) {
			String name = ( (NameVal) this.b ).name();
			if ( at instanceof StructType st ) {
				target = st.member(name, ctx).type();
			} else {
				FuncType ft = (FuncType) at;
				target = ft.member(name, ctx, false).type();
			}
		} else if ( this.op == BinaryOp.ARR_PNTR_INDEX ) {
			target = at instanceof PointerType pt ? pt.target() : ( (ArrayType) at ).target();
		} else {
			throw new CompileError(ctx, "this value " + this + " is not assignable");
		}
		type.checkCastable(target, ctx, false);
		this.a.checkAssignable(at, ctx);
	}
	
	public enum BinaryOpType {
		BOOL, BIT, CMP, MATH_ADDSUB, MATH, SHIFT, ARR_PNTR_INDEX, DEREF_BY_NAME,
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
		
		CMP_NAN_EQ(BinaryOpType.CMP),
		CMP_NAN_NEQ(BinaryOpType.CMP),
		CMP_NAN_GT(BinaryOpType.CMP),
		CMP_NAN_GE(BinaryOpType.CMP),
		CMP_NAN_LT(BinaryOpType.CMP),
		CMP_NAN_LE(BinaryOpType.CMP),
		
		SHIFT_LEFT(BinaryOpType.SHIFT),
		SHIFT_LOGIC_RIGTH(BinaryOpType.SHIFT),
		SHIFT_ARITMETIC_RIGTH(BinaryOpType.SHIFT),
		
		MATH_ADD(BinaryOpType.MATH_ADDSUB),
		MATH_SUB(BinaryOpType.MATH_ADDSUB),
		MATH_MUL(BinaryOpType.MATH),
		MATH_DIV(BinaryOpType.MATH),
		MATH_MOD(BinaryOpType.MATH),
		
		ARR_PNTR_INDEX(BinaryOpType.ARR_PNTR_INDEX),
		
		DEREF_BY_NAME(BinaryOpType.DEREF_BY_NAME),
		;//@formatter:on
		
		public final BinaryOpType type;
		
		private BinaryOp(BinaryOpType type) {
			this.type = type;
		}
		
	}
	
	@Override
	public String toString() {
		return switch ( this.op ) {
		case ARR_PNTR_INDEX -> this.a + "[" + this.b + "]";
		case DEREF_BY_NAME -> this.a + ":" + this.b;
		case BIT_AND -> "(" + this.a + " & " + this.b + ")";
		case BIT_OR -> "(" + this.a + " | " + this.b + ")";
		case BIT_XOR -> "(" + this.a + " ^ " + this.b + ")";
		case BOOL_AND -> "(" + this.a + " && " + this.b + ")";
		case BOOL_OR -> "(" + this.a + " || " + this.b + ")";
		case CMP_EQ -> "(" + this.a + " == " + this.b + ")";
		case CMP_GE -> "(" + this.a + " >= " + this.b + ")";
		case CMP_GT -> "(" + this.a + " > " + this.b + ")";
		case CMP_LE -> "(" + this.a + " <= " + this.b + ")";
		case CMP_LT -> "(" + this.a + " < " + this.b + ")";
		case CMP_NEQ -> "(" + this.a + " != " + this.b + ")";
		case CMP_NAN_EQ -> "( !(" + this.a + " == " + this.b + ") )";
		case CMP_NAN_GE -> "( !(" + this.a + " >= " + this.b + ") )";
		case CMP_NAN_GT -> "( !(" + this.a + " > " + this.b + ") )";
		case CMP_NAN_LE -> "( !(" + this.a + " <= " + this.b + ") )";
		case CMP_NAN_LT -> "( !(" + this.a + " < " + this.b + ") )";
		case CMP_NAN_NEQ -> "( !(" + this.a + " != " + this.b + ") )";
		case MATH_ADD -> "(" + this.a + " + " + this.b + ")";
		case MATH_SUB -> "(" + this.a + " - " + this.b + ")";
		case MATH_MUL -> "(" + this.a + " * " + this.b + ")";
		case MATH_DIV -> "(" + this.a + " / " + this.b + ")";
		case MATH_MOD -> "(" + this.a + " % " + this.b + ")";
		case SHIFT_LEFT -> "(" + this.a + " << " + this.b + ")";
		case SHIFT_ARITMETIC_RIGTH -> "(" + this.a + " >> " + this.b + ")";
		case SHIFT_LOGIC_RIGTH -> "(" + this.a + " >>> " + this.b + ")";
		};
	}
	
	@Override
	public boolean isConstant() {
		switch ( this.op ) {
		case DEREF_BY_NAME:
			return this.a.isConstant();
		case BOOL_AND:
			return this.a.isConstant() && this.b.isConstant()
				|| ( this.a.superSimplify() instanceof ScalarNumericVal snv && snv.value() == 0L );
		case BOOL_OR:
			return this.a.isConstant() && this.b.isConstant()
				|| ( this.a.superSimplify() instanceof ScalarNumericVal snv && snv.value() != 0L );
		// $CASES-OMITTED$
		default:
			return this.a.isConstant() && this.b.isConstant();
		}
	}
	
	@Override
	public SimpleValue simplify(UnaryOperator<SimpleValue> op) {
		SimpleValue sa = op.apply(this.a);
		SimpleValue sb = op.apply(this.b);
		if ( !isConstant() ) {
			if ( this.op == BinaryOp.CMP_EQ && sb instanceof ScalarNumericVal nb && nb.value() == 0L
				&& sa instanceof BinaryOpVal ba && ba.op.type == BinaryOpType.CMP ) {
				switch ( ba.op ) {
				case CMP_EQ:
					return create(ba.a, BinaryOp.CMP_NAN_NEQ, ba.b, this.ctx);
				case CMP_GE:
					return create(ba.a, BinaryOp.CMP_NAN_LT, ba.b, this.ctx);
				case CMP_GT:
					return create(ba.a, BinaryOp.CMP_NAN_LE, ba.b, this.ctx);
				case CMP_LE:
					return create(ba.a, BinaryOp.CMP_NAN_GT, ba.b, this.ctx);
				case CMP_LT:
					return create(ba.a, BinaryOp.CMP_NAN_GE, ba.b, this.ctx);
				case CMP_NEQ:
					return create(ba.a, BinaryOp.CMP_NAN_EQ, ba.b, this.ctx);
				case CMP_NAN_EQ:
					return create(ba.a, BinaryOp.CMP_NEQ, ba.b, this.ctx);
				case CMP_NAN_GE:
					return create(ba.a, BinaryOp.CMP_LT, ba.b, this.ctx);
				case CMP_NAN_GT:
					return create(ba.a, BinaryOp.CMP_LE, ba.b, this.ctx);
				case CMP_NAN_LE:
					return create(ba.a, BinaryOp.CMP_GT, ba.b, this.ctx);
				case CMP_NAN_LT:
					return create(ba.a, BinaryOp.CMP_GE, ba.b, this.ctx);
				case CMP_NAN_NEQ:
					return create(ba.a, BinaryOp.CMP_EQ, ba.b, this.ctx);
				// $CASES-OMITTED$
				default:
				}
			}
			return fallbackSimplify(sa, sb);
		}
		SimpleValue val = switch ( this.op ) {
		case ARR_PNTR_INDEX -> {
			if ( sa instanceof DataVal d && sb instanceof ScalarNumericVal n ) {
				yield new DataVal(d, n.value() * type().size(), true, type(), this.ctx);
			}
			yield null;
		}
		case BIT_AND -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() & nb.value(), this.ctx);
			}
			if ( sa instanceof ScalarNumericVal na && na.value() == 0L
				|| sb instanceof ScalarNumericVal nb && nb.value() == 0L ) {
				yield ScalarNumericVal.createAllowTruncate(type(), 0L, this.ctx);
			}
			yield null;
		}
		case BIT_OR -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() & nb.value(), this.ctx);
			}
			long s    = type().size();
			long mask = s == 8L ? -1L : 1L << ( s << 3 );
			if ( sa instanceof ScalarNumericVal na && na.value() == mask
				|| sb instanceof ScalarNumericVal nb && nb.value() == mask ) {
				yield ScalarNumericVal.createAllowTruncate(type(), 0L, this.ctx);
			}
			yield null;
		}
		case BIT_XOR -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() ^ nb.value(), this.ctx);
			}
			yield null;
		}
		case BOOL_AND -> null;
		case BOOL_OR -> null;
		case CMP_EQ -> null;
		case CMP_GE -> null;
		case CMP_GT -> null;
		case CMP_LE -> null;
		case CMP_LT -> null;
		case CMP_NEQ -> null;
		case CMP_NAN_EQ -> null;
		case CMP_NAN_GE -> null;
		case CMP_NAN_GT -> null;
		case CMP_NAN_LE -> null;
		case CMP_NAN_LT -> null;
		case CMP_NAN_NEQ -> null;
		case DEREF_BY_NAME -> null;
		case MATH_ADD -> null;
		case MATH_DIV -> null;
		case MATH_MOD -> null;
		case MATH_MUL -> null;
		case MATH_SUB -> null;
		case SHIFT_ARITMETIC_RIGTH -> null;
		case SHIFT_LEFT -> null;
		case SHIFT_LOGIC_RIGTH -> null;
		};
		if ( val != null ) return val;
		return fallbackSimplify(sa, sb);
	}
	
	private SimpleValue fallbackSimplify(SimpleValue sa, SimpleValue sb) {
		return create(sa, this.op, sb, this.ctx);
	}
	
}
