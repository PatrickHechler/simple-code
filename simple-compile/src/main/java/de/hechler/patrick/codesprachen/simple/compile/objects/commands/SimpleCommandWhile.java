package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

public class SimpleCommandWhile implements SimpleCommand {
	
	public final SimplePool    pool;
	public final SimpleValue   condition;
	public final SimpleCommand whileCmd;
	
	public SimpleCommandWhile(SimplePool pool, SimpleValue condition, SimpleCommand whileCmd) {
		this.pool = pool;
		this.condition = condition;
		this.whileCmd = whileCmd;
	}
	
	public static SimpleCommandWhile create(SimplePool pool, SimpleValue val, SimpleCommand whileCmd) {
		return new SimpleCommandWhile(pool, val, whileCmd);
	}

	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
