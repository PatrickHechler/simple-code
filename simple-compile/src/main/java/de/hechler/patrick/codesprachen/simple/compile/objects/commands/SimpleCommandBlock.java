package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;

public class SimpleCommandBlock implements SimpleCommand {
	
	// used by the compiler
	public long primitiveVariableCount = -1;
	// used by the compiler
	public long structuresByteCount = -1;
	
	public final SimplePool            pool;
	private final List <SimpleCommand> cmds;
	public final List <SimpleCommand>  commands;
	private boolean                    sealed = false;
	
	public SimpleCommandBlock(SimplePool pool) {
		this.pool = pool;
		this.cmds = new LinkedList <>();
		this.commands = Collections.unmodifiableList(this.cmds);
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	public void addCmd(SimpleCommand cmd) {
		if (sealed) {
			throw new IllegalStateException("already sealed");
		}
		cmds.add(cmd);
	}
	
	public void seal() {
		if (sealed) {
			throw new IllegalStateException("already sealed");
		}
		sealed = true;
	}
	
}
