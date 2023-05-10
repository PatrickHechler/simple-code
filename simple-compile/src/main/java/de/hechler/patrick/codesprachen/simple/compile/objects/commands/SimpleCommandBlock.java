package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleSubPool;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;

@SuppressWarnings("javadoc")
public class SimpleCommandBlock implements SimpleCommand {
	
	public final SimplePool           pool;
	private final List<SimpleCommand> cmds;
	public final List<SimpleCommand>  commands;
	private boolean                   sealed;
	private int                       regMax = -1;
	
	public SimpleCommandBlock(SimplePool pool) {
		this.pool     = pool;
		this.cmds     = new LinkedList<>();
		this.commands = Collections.unmodifiableList(this.cmds);
	}
	
	public SimpleCommandBlock(SimplePool pool, SimpleCommandBlock orig) {
		this.pool     = pool;
		this.cmds     = null;
		this.commands = orig.commands.subList(0, orig.cmds.size());
		this.sealed   = true;
	}
	
	@Override
	public SimplePool pool() {
		return this.pool;
	}
	
	public void addCmd(SimpleCommand cmd) {
		if (this.sealed) { throw new IllegalStateException("already sealed"); }
		this.cmds.add(cmd);
	}
	
	public void seal() {
		if (this.sealed) { throw new IllegalStateException("already sealed"); }
		this.sealed = true;
	}
	
	public SimpleCommandBlock snapshot(SimpleSubPool snapshotPool) {
		return new SimpleCommandBlock(snapshotPool, this);
	}
	
	public void initRegMax(int regMax) {
		if (!this.sealed) throw new IllegalStateException("this block still can be modified");
		if (this.regMax != -1) throw new IllegalStateException("this block has its regMax already initilized");
		if (regMax == -1) throw new AssertionError("regMax is -1");
		this.regMax = regMax;
	}
	
	public int regMax() {
		int s = this.regMax;
		if (s == -1) throw new IllegalStateException("this block has its regMax not yet set");
		return s;
	}
	
}
