package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

public class SimpleCommandVarDecl extends SimpleFunctionVariable implements SimpleCommand {
	
	public final SimplePool pool;
	public SimpleValue      init = null;
	
	public SimpleCommandVarDecl(SimplePool pool, SimpleType type, String name) {
		super(type, name);
		this.pool = pool;
	}
	
	public static SimpleCommandVarDecl create(SimplePool pool, SimpleType t, String text) {
		return new SimpleCommandVarDecl(pool, t, text);
	}
	
	public void initValue(SimpleValue val) {
		assert init == null;
		init = val;
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	@Override
	public int hashCode() {
		return 128 ^ super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) return false;
		else return getClass() == obj.getClass();
	}
	
}
