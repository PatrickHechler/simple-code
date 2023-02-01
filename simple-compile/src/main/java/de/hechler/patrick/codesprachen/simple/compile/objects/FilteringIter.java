package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteringIter<T> implements Iterator<T> {
	
	private T next;
	
	private final Iterator<T> iter;
	
	private final Predicate<T> func;
	
	public FilteringIter(Iterator<T> iter, Predicate<T> func) {
		this.iter = iter;
		this.func = func;
	}
	
	@Override
	public boolean hasNext() {
		if (next != null) return true;
		try {
			while (true) {
				T t = iter.next();
				if (func.test(t)) {
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
			if (func.test(t)) return t;
		}
	}
	
}
