// This file is part of the Simple Code Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import java.util.List;
import java.util.regex.Pattern;

import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

@SuppressWarnings("javadoc")
public class SimpleCommandAsm implements SimpleCommand {
	
	public final SimplePool pool;
	public final AsmParam[] asmArguments;
	public final String     asmCode;
	public final AsmParam[] asmResults;
	
	public SimpleCommandAsm(SimplePool pool, List<AsmParam> asmArguments, String asmCode, List<AsmParam> asmResults) {
		this(pool, toa(asmArguments), asmCode, toa(asmResults));
	}
	
	private static AsmParam[] toa(List<AsmParam> list) {
		return list.toArray(new AsmParam[list.size()]);
	}
	
	public SimpleCommandAsm(SimplePool pool, AsmParam[] asmArguments, String asmCode, AsmParam[] asmResults) {
		if (!asmCode.startsWith("::")||!asmCode.endsWith(">>")) throw new AssertionError("asm block is invalid: '"+asmCode+"'");
		for (AsmParam ap : asmArguments) {
			if (ap.value.type().isPrimitive() || ap.value.type().isPointer()) continue;
			throw new IllegalStateException("invalid asm argument type: " + ap.value.type() + " (only primitive values and pointers are supported)");
		}
		for (AsmParam ap : asmResults) {
			if (ap.value.type().isPrimitive() || ap.value.type().isPointer()) continue;
			throw new IllegalStateException("invalid asm result type: " + ap.value.type() + " (only primitive values and pointers are supported)");
		}
		this.pool         = pool;
		this.asmArguments = asmArguments;
		this.asmCode    = asmCode.substring(2, asmCode.length() - 2);
		this.asmResults = asmResults;
	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	public static class AsmParam {
		
		private static final Pattern XNN_PATTERN = Pattern.compile("^(X[0-9A-E][0-9A-F]|XF[0-9])$");
		public final SimpleValue value;
		public final int         register;
		
		public AsmParam(SimpleValue value, int register) {
			this.value    = value;
			this.register = register;
		}
		
		public static AsmParam create(SimpleValue val, String xnn) {
			if (!XNN_PATTERN.matcher(xnn).matches()) throw new AssertionError("illegal XNN: " + xnn);
			int reg = PrimAsmConstants.X_ADD + Integer.parseInt(xnn.substring(1), 16);
			return new AsmParam(val, reg);
		}
		
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString(b, "");
		return b.toString();
	}
	
	@Override
	public void toString(StringBuilder b, String indention) {
		b.append("asm");
		for (AsmParam ap : this.asmArguments) {
			b.append(' ').append(ap.value).append(" --> ");
			b.append('X').append(Integer.toHexString(ap.register - PrimAsmConstants.X_ADD).toUpperCase());
		}
		b.append(this.asmCode);
		for (AsmParam ap : this.asmResults) {
			b.append(" X").append(Integer.toHexString(ap.register - PrimAsmConstants.X_ADD).toUpperCase());
			b.append(" --> ").append(ap.value);
		}
		b.append(';');
	}
	
}
