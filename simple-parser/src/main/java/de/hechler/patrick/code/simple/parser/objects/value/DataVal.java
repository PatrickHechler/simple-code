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
package de.hechler.patrick.code.simple.parser.objects.value;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;

public record DataVal(byte[] value, SimpleType type, ErrorContext ctx, DataVal orig, long off, boolean deref)
	implements SimpleValue {
	
	public DataVal(byte[] value, SimpleType type, ErrorContext ctx) {
		this(value, type, ctx, null, 0L, false);
	}
	
	public DataVal(DataVal orig, long off, boolean derefValue, SimpleType type, ErrorContext ctx) {
		this(null, type, ctx, orig.orig == null ? orig : orig.orig, orig.orig == null ? off : off + orig.off,
			derefValue);
	}
	
	public static SimpleValue createString(String value, ErrorContext ctx) {
		byte[] val = value.concat("\0").getBytes(StandardCharsets.UTF_8);
		return new DataVal(val, stringType(val, ctx), ctx);
	}
	
	public static SimpleValue createString(StringBuilder value, ErrorContext ctx) {
		value.append('\0');
		byte[] val = value.toString().getBytes(StandardCharsets.UTF_8);
		return new DataVal(val, stringType(val, ctx), ctx);
	}
	
	private static SimpleValue create(List<SimpleValue> value, ErrorContext ctx) {
		if ( value.isEmpty() ) {
			return new DataVal(new byte[0],
				ArrayType.create(StructType.create(List.of(), StructType.FLAG_NOUSE, ctx), 0, ctx), ctx);
		}
		SimpleValue val = value.get(0);
		SimpleType t = val.type();
		long len = t.size() * value.size();
		if ( len != (int) len ) throw new CompileError(ctx,
			"the maximum size of a constant array 2^31-1 bytes (at least for this compiler)");
		byte[] data = new byte[(int) len];
		return new DataVal(data, new ArrayType(t, value.size()), ctx);
	}
	
	private static SimpleType stringType(byte[] data, ErrorContext ctx) {
		return ArrayType.create(NativeType.UBYTE, data.length, ctx);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.type.hashCode();
		result = prime * result + Arrays.hashCode(this.value);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		DataVal other = (DataVal) obj;
		if ( !this.type.equals(other.type) ) return false;
		return Arrays.equals(this.value, other.value);
	}
	
	@Override
	public boolean isConstant() { return true; }
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public SimpleValue simplify(@SuppressWarnings("unused") UnaryOperator<SimpleValue> op) { return this; }
	
	@Override
	public String toString() {
		if ( this.orig != null ) {
			if ( this.deref ) {
				PointerType ptype = PointerType.create(this.type, ErrorContext.NO_CONTEXT);
				return "( (" + ptype + ") ((ubyte#) " + this.orig + " + " + this.off + ") )#";
			}
			return "( (" + this.type + ") ((ubyte#) " + this.orig + " + " + this.off + ") )";
		}
		return fallbackToString();
	}
	
	private String fallbackToString() {
		StringBuilder b = new StringBuilder();
		b.append("DataVal [value=");
		boolean first = true;
		for (byte valb : this.value) {
			if ( !first ) {
				b.append(", ");
			} else {
				first = false;
			}
			String str = Integer.toHexString(0xFF & valb);
			if ( str.length() == 1 ) b.append('0');
			b.append(str);
		}
		b.append(", type=");
		b.append(this.type);
		b.append("]");
		return b.toString();
	}
	
}
