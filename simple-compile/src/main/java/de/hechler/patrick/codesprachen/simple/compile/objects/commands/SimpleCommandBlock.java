package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleSubPool;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;

public class SimpleCommandBlock implements SimpleCommand {
	
	public final SimplePool           pool;
	private final List<SimpleCommand> cmds     = new LinkedList<>();
	public final List<SimpleCommand>  commands = Collections.unmodifiableList(this.cmds);
	private boolean                   sealed   = false;
	
	public SimpleCommandBlock(SimplePool pool) {
		this.pool = pool;
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	public void addCmd(SimpleCommand cmd) {
		if (sealed) { throw new IllegalStateException("already sealed"); }
		cmds.add(cmd);
	}
	
	public void seal() {
		if (sealed) { throw new IllegalStateException("already sealed"); }
		sealed = true;
	}
	
	public SimpleCommandBlock snapshot(SimpleSubPool snapshotPool) {
		SimpleCommandBlock res = new SimpleCommandBlock(snapshotPool);
		res.cmds.addAll(this.cmds);
		res.sealed = true;
		return res;
	}
	
}
