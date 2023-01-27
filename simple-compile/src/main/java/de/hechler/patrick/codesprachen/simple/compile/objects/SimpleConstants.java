package de.hechler.patrick.codesprachen.simple.compile.objects;

import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueConst;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;

public class SimpleConstants {
	
	private SimpleConstants() { }
	
	public static SimpleConstant create(String name, SimpleValue val, boolean export) {
		if ( !val.isConstant()) {
			throw new IllegalArgumentException("the constant '" + name + "' has no constant value: (" + val + ")");
		}
		SimpleValueConst cv = (SimpleValueConst) val;
		if ( !cv.implicitNumber()) {
			throw new IllegalArgumentException("the constant '" + name + "' has no constant (implicit) number value: (" + cv + ")");
		}
		return new SimpleConstant(name, cv.getNumber(), export);
	}
	
}
