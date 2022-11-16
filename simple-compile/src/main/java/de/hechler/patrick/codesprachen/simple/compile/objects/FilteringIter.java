package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanFunction;

public class FilteringIter<T> implements Iterator<T> {
	
	private T next;
	
	private final Iterator<T> iter;
	
	private final ThrowingBooleanFunction<T, RuntimeException> func;
	
	public FilteringIter(Iterator<T> iter, ThrowingBooleanFunction<T, RuntimeException> func) {
		this.iter = iter;
		this.func = func;
	}
	
	@Override
	public boolean hasNext() {
		if (next != null) return true;
		try {
			while (true) {
				T t = iter.next();
				if (func.calc(t)) {
					next = t;
					return true;
				}
			}
		} catch (NoSuchElementException e) {
			return false;
		}
	}
	
	@Override
	public T next() {
		if (next != null) {
			T n = next;
			next = null;
			return n;
		}
		while (true) {
			T t = iter.next();
			if (func.calc(t)) return t;
		}
	}
	
}
