//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
