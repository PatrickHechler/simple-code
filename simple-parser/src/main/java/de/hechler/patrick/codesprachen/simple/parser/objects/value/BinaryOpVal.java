//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.parser.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.parser.error.CompileError;
import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.StructType;

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
			return switch ( this.a.type() ) {
			case PointerType pt -> pt.target();
			case ArrayType at -> at.target();
			default -> throw new AssertionError(
				"binary operator array/pointer index, but the first operants type is no array/pointer: " + this);
			};
		}
		if ( this.op.type == BinaryOpType.DEREF_BY_NAME ) {
			String name = ( (NameVal) this.b ).name();
			return switch ( this.a.type() ) {
			case StructType st -> st.member(name, ctx).type();
			case FuncType ft when ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ->
				ft.member(name, ctx, false).type();
			default -> throw new AssertionError(
				"binary operator deref by name, but the first operants type is no (function) structure: " + this);
			};
		}
		return this.a.type();
	}
	
	@Override
	public void checkAssignable(SimpleType type, ErrorContext ctx) throws CompileError {
		final SimpleType at = this.a.type();
		SimpleType target;
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
		SHIFT_RIGTH(BinaryOpType.SHIFT),
		
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
		case CMP_NAN_EQ -> "( !(" + this.a + " != " + this.b + ") )";
		case CMP_NAN_GE -> "( !(" + this.a + " < " + this.b + ") )";
		case CMP_NAN_GT -> "( !(" + this.a + " <= " + this.b + ") )";
		case CMP_NAN_LE -> "( !(" + this.a + " > " + this.b + ") )";
		case CMP_NAN_LT -> "( !(" + this.a + " >= " + this.b + ") )";
		case CMP_NAN_NEQ -> "( !(" + this.a + " == " + this.b + ") )";
		case MATH_ADD -> "(" + this.a + " + " + this.b + ")";
		case MATH_SUB -> "(" + this.a + " - " + this.b + ")";
		case MATH_MUL -> "(" + this.a + " * " + this.b + ")";
		case MATH_DIV -> "(" + this.a + " / " + this.b + ")";
		case MATH_MOD -> "(" + this.a + " % " + this.b + ")";
		case SHIFT_LEFT -> "(" + this.a + " << " + this.b + ")";
		case SHIFT_RIGTH -> "(" + this.a + " >> " + this.b + ")";
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
		case DEREF_BY_NAME -> {
			String name = ( (NameVal) sb ).name();
			long offset = switch ( this.a.type() ) {
			case StructType st -> st.offset(name);
			case FuncType t -> t.offset(name);
			default -> throw new AssertionError(this);
			};
			yield switch ( sa ) {
			case DataVal d when d.deref() -> new DataVal(d, offset, true, type(), this.ctx);
			case ScalarNumericVal n -> ScalarNumericVal.create(type(), n.value() + offset, this.ctx);
			default -> null;
			};
		}
		case ARR_PNTR_INDEX -> {
			if ( sa instanceof DataVal d && sb instanceof ScalarNumericVal n ) {
				yield new DataVal(d, n.value() * type().size(), true, type(), this.ctx);
			}
			yield null;
		}
		case BIT_AND -> {
			if ( sa instanceof ScalarNumericVal na && na.value() == 0L ) {
				yield ScalarNumericVal.createAllowTruncate(type(), 0L, this.ctx);
			}
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() & nb.value(), this.ctx);
			}
			yield null;
		}
		case BIT_OR -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() & nb.value(), this.ctx);
			}
			long s = type().size();
			long mask = s == 8L ? -1L : 1L << ( s << 3 );
			if ( sa instanceof ScalarNumericVal na && na.value() == mask ) {
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
		case BOOL_AND -> {
			if ( sa instanceof ScalarNumericVal na && na.value() == 0L ) {
				yield ScalarNumericVal.createAllowTruncate(type(), 0L, this.ctx);
			}
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), ( na.value() != 0L && nb.value() != 0L ) ? 1L : 0L,
					this.ctx);
			}
			yield null;
		}
		case BOOL_OR -> {
			if ( sa instanceof ScalarNumericVal na && na.value() != 0L ) {
				yield ScalarNumericVal.createAllowTruncate(type(), 1L, this.ctx);
			}
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), ( na.value() != 0L || nb.value() != 0L ) ? 1L : 0L,
					this.ctx);
			}
			yield null;
		}
		case CMP_EQ -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() == nb.value() ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() == nb.value() ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_GE -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() >= nb.value() ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() >= nb.value() ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_GT -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() > nb.value() ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() > nb.value() ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_LE -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() <= nb.value() ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() <= nb.value() ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_LT -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() < nb.value() ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() < nb.value() ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NEQ -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() != nb.value() ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() != nb.value() ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NAN_EQ -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() != nb.value() ) ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() != nb.value() ) ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NAN_GE -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() < nb.value() ) ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() < nb.value() ) ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NAN_GT -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() <= nb.value() ) ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() <= nb.value() ) ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NAN_LE -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() > nb.value() ) ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() > nb.value() ) ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NAN_LT -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() >= nb.value() ) ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() >= nb.value() ) ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case CMP_NAN_NEQ -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() == nb.value() ) ? 1L : 0L, this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), !( na.value() == nb.value() ) ? 1L : 0L, this.ctx);
			}
			yield null;
		}
		case MATH_ADD -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() + nb.value(), this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield FPNumericVal.create(na.type(), na.value() + nb.value(), this.ctx);
			}
			yield null;
		}
		case MATH_DIV -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() / nb.value(), this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield FPNumericVal.create(na.type(), na.value() / nb.value(), this.ctx);
			}
			yield null;
		}
		case MATH_MOD -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() % nb.value(), this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield FPNumericVal.create(na.type(), na.value() % nb.value(), this.ctx);
			}
			yield null;
		}
		case MATH_MUL -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() * nb.value(), this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield FPNumericVal.create(na.type(), na.value() * nb.value(), this.ctx);
			}
			yield null;
		}
		case MATH_SUB -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() - nb.value(), this.ctx);
			}
			if ( sa instanceof FPNumericVal na && sb instanceof FPNumericVal nb ) {
				yield FPNumericVal.create(na.type(), na.value() - nb.value(), this.ctx);
			}
			yield null;
		}
		case SHIFT_LEFT -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				yield ScalarNumericVal.createAllowTruncate(type(), na.value() << nb.value(), this.ctx);
			}
			yield null;
		}
		case SHIFT_RIGTH -> {
			if ( sa instanceof ScalarNumericVal na && sb instanceof ScalarNumericVal nb ) {
				switch ( sa.type() ) {
				case NativeType.NUM, NativeType.DWORD, NativeType.WORD, NativeType.BYTE:
					yield ScalarNumericVal.createAllowTruncate(type(), na.value() >> nb.value(), this.ctx);
				default:
					yield ScalarNumericVal.createAllowTruncate(type(), na.value() >>> nb.value(), this.ctx);
				}
			}
			yield null;
		}
		};
		if ( val != null ) return val;
		return fallbackSimplify(sa, sb);
	}
	
	private SimpleValue fallbackSimplify(SimpleValue sa, SimpleValue sb) {
		return create(sa, this.op, sb, this.ctx);
	}
	
}
