package de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;

public class SimpleCommandIf implements SimpleCommand {
	
	public final SimplePool    pool;
	public final SimpleValue   condition;
	public final SimpleCommand ifCmd;
	public final SimpleCommand elseCmd;
	
	public SimpleCommandIf(SimplePool pool, SimpleValue condition, SimpleCommand ifCmd, SimpleCommand elseCmd) {
		this.pool = pool;
		this.condition = condition;
		this.ifCmd = ifCmd;
		this.elseCmd = elseCmd;
	}
	
	public static SimpleCommandIf create(SimplePool pool, SimpleValue val, SimpleCommand cmd, SimpleCommand elseCmd) {
		return new SimpleCommandIf(pool, val, cmd, elseCmd);
	}

	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
