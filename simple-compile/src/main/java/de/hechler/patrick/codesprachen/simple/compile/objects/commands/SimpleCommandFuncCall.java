package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

public class SimpleCommandFuncCall implements SimpleCommand {
	
	public final SimplePool  pool;
	public final String      firstName;
	public final String      secondName;
	public final SimpleValue function;
	
	public SimpleCommandFuncCall(SimplePool pool, String firstName, String secondName, SimpleValue function) {
		this.pool = pool;
		this.firstName = firstName;
		this.secondName = secondName;
		this.function = function;
	}
	
	public static SimpleCommandFuncCall create(SimplePool pool, String fn, String sn, SimpleValue val) {
		if ( !val.type().isFunc()) {
			throw new IllegalArgumentException("a function call needs a function structure as argument! (arg-type: " + val.type() + " arg: '" + val + "')");
		}
		return new SimpleCommandFuncCall(pool, fn, sn, val);
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
