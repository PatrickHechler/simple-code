package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

@SuppressWarnings("javadoc")
public class SimpleCommandAssign implements SimpleCommand {
	
	public final SimplePool  pool;
	public final SimpleValue target;
	public final SimpleValue value;
	
	public SimpleCommandAssign(SimplePool pool, SimpleValue target, SimpleValue value) {
		this.pool   = pool;
		this.target = target;
		this.value  = value;
	}
	
	public static SimpleCommandAssign create(SimplePool pool, SimpleValue target, SimpleValue value) {
		SimpleType t = target.type();
		if (t.equals(value.type())) {
			throw new IllegalStateException("target and value type are different");
		}
		if (!t.isPrimitive() && !t.isPointer()) {
			throw new IllegalStateException("can not assign with array/structure values! (target: " + target + " value: " + value + ")");
		}
		return new SimpleCommandAssign(pool, target, value);
	}
	
	@Override
	public SimplePool pool() {
		return this.pool;
	}
	
}
