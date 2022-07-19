package de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands;

import java.util.Collections;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;

public class SimpleCommandBlock implements SimpleCommand {
	
	// used by the compiler
	public long primitiveVariableCount = -1;
	// used by the compiler
	public long structuresByteCount = -1;
	
	public final SimplePool           pool;
	public final List <SimpleCommand> cmds;
	
	public SimpleCommandBlock(SimplePool pool, List <SimpleCommand> cmds) {
		this.pool = pool;
		this.cmds = Collections.unmodifiableList(cmds);
	}
	
	public static SimpleCommandBlock create(SimplePool pool, List <SimpleCommand> cmds) {
		return new SimpleCommandBlock(pool, cmds);
	}

	@Override
	public SimplePool pool() {
		return pool;
	}
	
}
