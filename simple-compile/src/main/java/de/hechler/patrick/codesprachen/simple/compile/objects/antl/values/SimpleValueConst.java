package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public abstract class SimpleValueConst extends SimpleValueNoConst {
	
	public SimpleValueConst(SimpleType type) {
		super(type);
	}

	/**
	 * used when this constant contains of data and not a number (for example a string)
	 * <p>
	 * a constant number casted to a pointer type will still return <code>false</code>, since the number value is known at compile time
	 * 
	 * @return <code>true</code> when this value is a data pointer value and has not a constant number
	 */
	protected boolean isDataPointer() {
		return false;
	}
	
	@Override
	public boolean isConstDataPointer() {
		return isDataPointer();
	}
	
	@Override
	public boolean isConstNoDataPointer() {
		return !isDataPointer();
	}
	
	protected abstract boolean implicitNumber();
	
	protected abstract boolean implicitFPNumber();
	
	protected abstract long getNumber();
	
	protected abstract int getNumberBits();
	
	protected abstract double getFPNumber();
	
	@Override
	public final boolean isConstant() {
		return true;
	}
	
	@Override
	public SimpleValue addExpCond(SimpleValue val, SimpleValue val2) {
		if ( !implicitNumber()) {
			assert implicitFPNumber();
			if (getFPNumber() != 0.0) {
				return val;
			} else {
				return val2;
			}
		} else if (isDataPointer() || getNumber() != 0L) {
			return val;
		} else {
			return val2;
		}
	}
	
	@Override
	public SimpleValue addExpLOr(SimpleValue val) {
		if (val.isConstant()) {
			return logicOr((SimpleValueConst) val);
		}
		return super.addExpLOr(val);
	}
	
	private SimpleValue logicOr(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() | val.getNumber());
	}
	
	@Override
	public SimpleValue addExpLAnd(SimpleValue val) {
		if (val.isConstant()) {
			return logicAnd((SimpleValueConst) val);
		}
		return super.addExpLAnd(val);
	}
	
	private SimpleValue logicAnd(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical and only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() & val.getNumber());
	}
	
	@Override
	public SimpleValue addExpOr(SimpleValue val) {
		if (val.isConstant()) {
			return boolOr((SimpleValueConst) val);
		}
		return super.addExpOr(val);
	}
	
	private SimpleValue boolOr(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("bool or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), (getNumber() != 0) || (val.getNumber() != 0) ? 1L : 0L);
	}
	
	@Override
	public SimpleValue addExpXor(SimpleValue val) {
		if (val.isConstant()) {
			return exclusiveOr((SimpleValueConst) val);
		}
		return super.addExpXor(val);
	}
	
	private SimpleValue exclusiveOr(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), getNumber() ^ val.getNumber());
	}
	
	@Override
	public SimpleValue addExpAnd(SimpleValue val) {
		if (val.isConstant()) {
			return boolAnd((SimpleValueConst) val);
		}
		return super.addExpAnd(val);
	}
	
	private SimpleValue boolAnd(SimpleValueConst val) {
		if ( !implicitNumber() || !val.implicitNumber()) {
			throw new IllegalStateException("logical or only possible when both values are impicit numbers! this:" + this + " otehr: " + val);
		}
		return new SimpleNumberValue(Math.max(getNumberBits(), val.getNumberBits()), (getNumber() != 0) && (val.getNumber() != 0) ? 1L : 0L);
	}
	
	@Override
	public SimpleValue addExpEq(boolean equal, SimpleValue val) {
		if (val.isConstant()) {
			return equal(equal, (SimpleValueConst) val);
		}
		return super.addExpEq(equal, val);
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
	public SimpleValue addExpRel(int type, SimpleValue val) {
		if (val.isConstant()) {
			return relativeCheck(type, (SimpleValueConst) val);
		}
		return super.addExpRel(type, val);
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
	public SimpleValue addExpShift(int type, SimpleValue val) {
		if (val.isConstant()) {
			return shift(type, (SimpleValueConst) val);
		}
		return super.addExpShift(type, val);
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
	public SimpleValue addExpAdd(boolean add, SimpleValue val) {
		if (val.isConstant()) {
			return addOrSubtract(add, (SimpleValueConst) val);
		}
		return super.addExpAdd(add, val);
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
	public SimpleValue addExpMul(int type, SimpleValue val) {
		if (val.isConstant()) {
			return multiplyDivideOrModulo(type, (SimpleValueConst) val);
		}
		return super.addExpMul(type, val);
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
	public SimpleValue addExpArrayRef(SimpleValue val) {
		if ( !isDataPointer() || !val.isConstant()) {
			return super.addExpArrayRef(val);
		} else {
			SimpleValueConst value = (SimpleValueConst) val;
			if ( !value.implicitNumber()) {
				throw new IllegalStateException("array referencing is not possible without an number index!");
			}
			try {
				return arrayDeref(value.getNumber());
			} catch (IndexOutOfBoundsException e) {
				System.err.println("[WARN]: constant array index out of bounds of a constant array! index: " + value + " array: " + this);
				return super.addExpArrayRef(val);
			}
		}
	}
	
	protected SimpleValue arrayDeref(long val) {
		throw new InternalError("this method should only be called when this method has been overwritten! " + getClass());
	}
	
}
