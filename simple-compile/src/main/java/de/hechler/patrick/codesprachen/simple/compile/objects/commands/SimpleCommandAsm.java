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
package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

public class SimpleCommandAsm implements SimpleCommand {
	
	public final SimplePool pool;
	public final AsmParam[] asmArguments;
	public final String     asmCode;
	public final AsmParam[] asmResults;
	
	public SimpleCommandAsm(SimplePool pool, List <AsmParam> asmArguments, String asmCode, List <AsmParam> asmResults) {
		this(pool, toa(asmArguments), asmCode, toa(asmResults));
	}
	
	private static AsmParam[] toa(List <AsmParam> list) {
		return list.toArray(new AsmParam[list.size()]);
	}
	
	public SimpleCommandAsm(SimplePool pool, AsmParam[] asmArguments, String asmCode, AsmParam[] asmResults) {
		this.pool = pool;
		this.asmArguments = asmArguments;
		assert asmCode.startsWith("::");
		assert asmCode.endsWith(">>");
		this.asmCode = asmCode.substring(2, asmCode.length() - 2);
		this.asmResults = asmResults;
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	public static class AsmParam {
		
		public final SimpleValue value;
		public final int         register;
		
		public AsmParam(SimpleValue value, int register) {
			this.value = value;
			this.register = register;
		}
		
		public static AsmParam create(SimpleValue val, String xnn) {
			assert xnn.matches("^(X[0-9A-E][0-9A-F]|XF[0-9])$");
			int reg = PrimAsmConstants.X_ADD + Integer.parseInt(xnn.substring(1), 16);
			return new AsmParam(val, reg);
		}
		
	}
	
}
