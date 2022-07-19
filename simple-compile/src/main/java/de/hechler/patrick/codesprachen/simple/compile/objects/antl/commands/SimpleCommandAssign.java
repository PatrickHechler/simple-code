package de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleVariableValue;

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
		SimpleType tt = target.type();
		SimpleType vt = value.type();
		if (vt.isArray() || tt.isArray() || vt.isStruct() || tt.isStruct()) {
			throw new IllegalStateException("can not assign with array/structure values! (target: " + tt + " value: " + vt + ")");
		}
		if (! (target instanceof SimpleVariableValue)) {
			target = target.addExpUnary(pool, SimpleValue.EXP_UNARY_AND);
		}
		return new SimpleCommandAssign(pool, target, value);
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
