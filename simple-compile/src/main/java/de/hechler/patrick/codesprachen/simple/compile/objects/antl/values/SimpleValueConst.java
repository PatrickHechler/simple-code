package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public abstract class SimpleValueConst extends SimpleValueNoConst {
	
	public SimpleValueConst(SimpleType type) {
		super(type);
	}
	
	@Deprecated(forRemoval = true)
	protected boolean isDataPointerOrArray() {
		return false;
	}
	
	@Override
	@Deprecated(forRemoval = true)
	public boolean isConstData() {
		return isDataPointerOrArray();
	}
	
	@Override
	@Deprecated(forRemoval = true)
	public boolean isConstNoData() {
		return !isDataPointerOrArray();
	}
	
	public abstract boolean implicitNumber();
	
	public abstract boolean implicitFPNumber();
	
	public abstract long getNumber();
	
	public abstract int getNumberBits();
	
	public abstract double getFPNumber();
	
	@Override
	public final boolean isConstant() {
		return true;
	}
	
	@Override
	public SimpleValue addExpCond(SimplePool pool, SimpleValue val, SimpleValue val2) {
		if ( !implicitNumber()) {
			assert implicitFPNumber();
			if (getFPNumber() != 0.0) {
				return val;
			} else {
				return val2;
			}
		} else if (isDataPointerOrArray() || getNumber() != 0L) {
			return val;
		} else {
			return val2;
		}
	}
	
	@Override
	public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
		if (t.isArray() || t.isStruct()) {
			throw new InternalError("arrays and structures should overwrite this method!");
		}
		if (type.isArray()) {
			throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
		}
		if (type == t) {
			return this;
		} else if (type.isStruct()) {
			throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
		} else if (type.isPointer() ^ t.isPointer()) {
			if (t == SimpleType.FPNUM || type == SimpleType.FPNUM) {
				throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
			} else if (t.isPointer()) {
				long num = getNumber();
				return new SimpleNumberValue(type, num == 0L ? -1L : num);
			} else {
				long num = getNumber();
				return new SimpleNumberValue(type, num == -1L ? 0L : num);
			}
		} else if (type.isPointer() /* && t.isPointer() */) {
			return new SimpleNumberValue(type, getNumber());
		} else if (t == SimpleType.FPNUM ^ type == SimpleType.FPNUM) {
			if (t == SimpleType.FPNUM) {
				return new SimpleNumberValue(type, (long) getFPNumber());
			} else {
				return new SimpleFPNumberValue((double) getNumber());
			}
			// } else if (t == SimpleType.FPNUM) { // if (type == t) return this; at start
			// return new SimpleFPNumberValue(getFPNumber());
		} else if (t.isPrimitive() && type.isPrimitive()) {
			return new SimpleNumberValue(type, getNumber());
		} else {
			throw new InternalError("unknown cast!");
		}
	}
	
	@Override
	public SimpleValue addExpLOr(SimplePool pool, SimpleValue val) {
		if (val.isConstant()) {
			return logicOr((SimpleValueConst) val);
		}
		return super.addExpLOr(pool, val);
	}
	
	private SimpleValue logicOr(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() | val.getNumber());
	}
	
	@Override
	public SimpleValue addExpLAnd(SimplePool pool, SimpleValue val) {
		if (val.isConstant()) {
			return logicAnd((SimpleValueConst) val);
		}
		return super.addExpLAnd(pool, val);
	}
	
	private SimpleValue logicAnd(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical and only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() & val.getNumber());
	}
	
	@Override
	public SimpleValue addExpOr(SimplePool pool, SimpleValue val) {
		if (val.isConstant()) {
			return boolOr((SimpleValueConst) val);
		}
		return super.addExpOr(pool, val);
	}
	
	private SimpleValue boolOr(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("bool or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), (getNumber() != 0) || (val.getNumber() != 0) ? 1L : 0L);
	}
	
	@Override
	public SimpleValue addExpXor(SimplePool pool, SimpleValue val) {
		if (val.isConstant()) {
			return exclusiveOr((SimpleValueConst) val);
		}
		return super.addExpXor(pool, val);
	}
	
	private SimpleValue exclusiveOr(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() ^ val.getNumber());
	}
	
	@Override
	public SimpleValue addExpAnd(SimplePool pool, SimpleValue val) {
		if (val.isConstant()) {
			return boolAnd((SimpleValueConst) val);
		}
		return super.addExpAnd(pool, val);
	}
	
	private SimpleValue boolAnd(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), (getNumber() != 0) && (val.getNumber() != 0) ? 1L : 0L);
	}
	
	@Override
	public SimpleValue addExpEq(SimplePool pool, boolean equal, SimpleValue val) {
		if (val.isConstant()) {
			return equal(equal, (SimpleValueConst) val);
		}
		return super.addExpEq(pool, equal, val);
	}
	
	private SimpleValue equal(boolean equal, SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			assert implicitFPNumber();
			assert val.implicitFPNumber();
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), (val.getFPNumber() == getFPNumber() ? equal : !equal) ? 1L : 0L);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), (val.getNumber() == getNumber() ? equal : !equal) ? 1L : 0L);
	}
	
	@Override
	public SimpleValue addExpRel(SimplePool pool, int type, SimpleValue val) {
		if (val.isConstant()) {
			return relativeCheck(type, (SimpleValueConst) val);
		}
		return super.addExpRel(pool, type, val);
	}
	
	private SimpleValue relativeCheck(int type, SimpleValueConst val) {
		int cmp;
		if ( !implicitNumber() || !val.implicitNumber()) {
			assert implicitFPNumber();
			assert val.implicitFPNumber();
			cmp = (int) Math.signum(val.getFPNumber() - getFPNumber());
		} else {
			cmp = (int) Math.signum(val.getNumber() - getNumber());
		}
		switch (type) {
		case EXP_GREATHER:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), cmp > 0 ? 1L : 0L);
		case EXP_GREATHER_EQUAL:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), cmp >= 0 ? 1L : 0L);
		case EXP_SMALLER_EQUAL:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), cmp <= 0 ? 1L : 0L);
		case EXP_SMALLER:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), cmp < 0 ? 1L : 0L);
		default:
			throw new InternalError("unknown type: " + type);
		}
	}
	
	@Override
	public SimpleValue addExpShift(SimplePool pool, int type, SimpleValue val) {
		if (val.isConstant()) {
			return shift(type, (SimpleValueConst) val);
		}
		return super.addExpShift(pool, type, val);
	}
	
	private SimpleValue shift(int type, SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("shift operations only work on numbers (not fpnums)");
		}
		switch (type) {
		case EXP_SHIFT_LOGIC_RIGTH:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() >>> val.getNumber());
		case EXP_SHIFT_ARITMETIC_RIGTH:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() >> val.getNumber());
		case EXP_SHIFT_LEFT:
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() << val.getNumber());
		default:
			throw new InternalError("unknown type: " + type);
		}
	}
	
	@Override
	public SimpleValue addExpAdd(SimplePool pool, boolean add, SimpleValue val) {
		if (val.isConstant()) {
			return addOrSubtract(add, (SimpleValueConst) val);
		}
		return super.addExpAdd(pool, add, val);
	}
	
	private SimpleValue addOrSubtract(boolean add, SimpleValueConst val) {
		if (implicitNumber() && val.implicitNumber()) {
			long value;
			if (add) {
				value = getNumber() + val.getNumber();
			} else {
				value = getNumber() - val.getNumber();
			}
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), value);
		} else if (implicitFPNumber() && val.implicitFPNumber()) {
			double value;
			if (add) {
				value = getFPNumber() + val.getFPNumber();
			} else {
				value = getFPNumber() - val.getFPNumber();
			}
			return new SimpleFPNumberValue(value);
		} else {
			throw new IllegalStateException("add and subtract only possible with numbers (and fpnums)");
		}
	}
	
	@Override
	public SimpleValue addExpMul(SimplePool pool, int type, SimpleValue val) {
		if (val.isConstant()) {
			return multiplyDivideOrModulo(type, (SimpleValueConst) val);
		}
		return super.addExpMul(pool, type, val);
	}
	
	private SimpleValue multiplyDivideOrModulo(int type, SimpleValueConst val) {
		if (implicitNumber() && val.implicitNumber()) {
			long value;
			switch (type) {
			case EXP_MULTIPLY:
				value = getNumber() * val.getNumber();
				break;
			case EXP_DIVIDE:
				value = getNumber() / val.getNumber();
				break;
			case EXP_MODULO:
				value = getNumber() % val.getNumber();
				break;
			default:
				throw new InternalError("unknown type: " + type);
			}
			return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), value);
		} else if (implicitFPNumber() && val.implicitFPNumber()) {
			double value;
			switch (type) {
			case EXP_MULTIPLY:
				value = getFPNumber() * val.getFPNumber();
				break;
			case EXP_DIVIDE:
				value = getFPNumber() / val.getFPNumber();
				break;
			case EXP_MODULO:
				value = getFPNumber() % val.getFPNumber();
				break;
			default:
				throw new InternalError("unknown type: " + type);
			}
			return new SimpleFPNumberValue(value);
		} else {
			throw new IllegalStateException("multiply, divide and modulo is only possible with numbers (and fpnums)");
		}
	}
	
	@Override
	public SimpleValue addExpArrayRef(SimplePool pool, SimpleValue val) {
		if ( !isDataPointerOrArray() || !val.isConstant()) {
			return super.addExpArrayRef(pool, val);
		} else {
			SimpleValueConst value = (SimpleValueConst) val;
			if ( !value.implicitNumber()) {
				throw new IllegalStateException("array referencing is not possible without an number index!");
			}
			try {
				return arrayDeref(value.getNumber());
			} catch (IndexOutOfBoundsException | UnsupportedOperationException e) {
				if (e instanceof IndexOutOfBoundsException) {
					System.err.println("[WARN]: constant array index out of bounds of a constant array! index: " + value + " array: " + this);
				}
				return super.addExpArrayRef(pool, val);
			}
		}
	}
	
	protected SimpleValue arrayDeref(long val) {
		throw new InternalError("this method should only be called when this method has been overwritten! " + getClass());
	}
	
}
