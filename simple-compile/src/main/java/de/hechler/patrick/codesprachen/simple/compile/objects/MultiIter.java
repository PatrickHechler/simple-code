package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultiIter<T> implements Iterator<T> {
	
	private final Iterator<?>[] iters;
	private int                 index;
	
	public MultiIter(Iterator<?>... iters) {
		this.iters = iters;
	}
	
	@Override
	public boolean hasNext() {
		for (; index < iters.length; index++) {
			if (iters[index].hasNext()) return true;
		}
		return false;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public T next() throws NoSuchElementException {
		for (; index < iters.length; index++) {
			try {
				return (T) iters[index].next();
			} catch (NoSuchElementException ignore) {}
		}
		throw new NoSuchElementException("iterated over all iterators");
	}
	
}
