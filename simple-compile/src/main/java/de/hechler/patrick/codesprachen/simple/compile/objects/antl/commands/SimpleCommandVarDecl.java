package de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;

public class SimpleCommandVarDecl extends SimpleVariable implements SimpleCommand {
	
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
	
}
