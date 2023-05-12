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
