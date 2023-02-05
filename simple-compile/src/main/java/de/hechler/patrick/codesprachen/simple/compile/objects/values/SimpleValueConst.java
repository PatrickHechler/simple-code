package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

public abstract class SimpleValueConst extends SimpleValueNoConst {
	
	public SimpleValueConst(SimpleType type) {
		super(type);
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
	
}
