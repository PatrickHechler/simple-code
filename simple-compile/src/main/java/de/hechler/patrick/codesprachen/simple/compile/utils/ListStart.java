package de.hechler.patrick.codesprachen.simple.compile.utils;

import java.util.AbstractList;
import java.util.List;


@SuppressWarnings("javadoc")
public class ListStart<T> extends AbstractList<T> implements List<T> {
	
	private final List<T> list;
	private final int     size;
	
	public ListStart(List<T> list, int size) {
		this.list = list;
		this.size = size;
	}

	@Override
	public int size() { 
	return this.size; }

	@Override
	public T get(int index) { 
		if (index < 0 || index > this.size) throw new IndexOutOfBoundsException("index=" + index + " size=" + this.size);
		return this.list.get(index);
	}

}
