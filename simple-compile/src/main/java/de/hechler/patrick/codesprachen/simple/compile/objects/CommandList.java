package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;


public class CommandList extends LinkedList <Command> implements List <Command> {
	
	/** UID */
	private static final long serialVersionUID = -8618629641584207656L;
	
	
	public CommandList() {
		super();
	}
	
	public CommandList(Collection <Command> cmds) {
		super(cmds);
	}
	
	@Override
	public String toString() {
		Iterator <Command> iter = iterator();
		if ( !iter.hasNext()) return "{}";
		StringBuilder b = new StringBuilder();
		b.append('{');
		b.append(iter.next());
		while (iter.hasNext()) {
			b.append("; ");
			b.append(iter.next());
		}
		b.append('}');
		return b.toString();
	}
	
	@Override
	public List <Command> subList(int fromIndex, int toIndex) {
		return new SubCmdList(fromIndex, toIndex);
	}
	
	public class SubCmdList extends AbstractList <Command> implements List <Command> {
		
		private final int base;
		private int       size;
		private int       modCnt = CommandList.super.modCount;
		
		public SubCmdList(int base, int size) {
			this.base = base;
			this.size = size;
		}
		
		@Override
		public Command get(int index) {
			synchronized (CommandList.this) {
				modCheck();
				if (index >= size) {
					throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
				}
				return CommandList.this.get(base + index);
			}
		}
		
		@Override
		public Command set(int index, Command element) {
			synchronized (CommandList.this) {
				modCheck();
				if (index >= size) {
					throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
				}
				return CommandList.this.set(base + index, element);
			}
		}
		
		@Override
		public void add(int index, Command element) {
			synchronized (CommandList.this) {
				modCheck();
				if (index >= size) {
					throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
				}
				CommandList.this.add(base + index, element);
				size ++ ;
				modCnt = CommandList.super.modCount;
			}
		}
		
		@Override
		public boolean add(Command element) {
			synchronized (CommandList.this) {
				modCheck();
				CommandList.this.add(base + size, element);
				size ++ ;
				modCnt = CommandList.super.modCount;
				return true;
			}
		}
		
		@Override
		public int size() {
			synchronized (CommandList.this) {
				modCheck();
				return size;
			}
		}
		
		private void modCheck() {
			if (modCnt != CommandList.super.modCount) {
				throw new ConcurrentModificationException();
			}
		}
		
	}
	
}
