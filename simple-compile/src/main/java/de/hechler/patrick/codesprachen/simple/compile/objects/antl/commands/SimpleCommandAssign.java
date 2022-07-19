package de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;

public class SimpleCommandAssign implements SimpleCommand {
	
	public final SimplePool  pool;
	public final SimpleValue target;
	public final SimpleValue value;
	
	public SimpleCommandAssign(SimplePool pool, SimpleValue target, SimpleValue value) {
		this.pool = pool;
		this.target = target;
		this.value = value;
	}
	
	public static SimpleCommandAssign create(SimplePool pool, SimpleValue target, SimpleValue value) {
		SimpleType t = value.type();
		if ( !t.isPrimitive() && !t.isPointer()) {
			throw new IllegalStateException("can not assign with array/structure values! (target: " + target + " value: " + value + ")");
		}
		t = target.type();
		if ( !t.isPrimitive() && !t.isPointer()) {
			throw new IllegalStateException("can not assign with array/structure values! (target: " + target + " value: " + value + ")");
		}
		return new SimpleCommandAssign(pool, target, value);
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
