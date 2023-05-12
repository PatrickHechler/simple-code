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

import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.SimpleCompiler;

@SuppressWarnings("javadoc")
public class UsedData implements Cloneable {
	
	public int  regs        = SimpleCompiler.MIN_VAR_REGISTER;
	public int  maxregs     = -1;
	public long currentaddr = 0L;
	public long maxaddr     = -1L;
	
	@Override
	public UsedData clone() {
		try {
			return (UsedData) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}
	
}
