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
package de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.types;

import static de.hechler.patrick.codesprachen.simple.compile.interpreter.error.ErrorContext.NO_CONTEXT;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.interpreter.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.simplefile.SimpleVariable;

public record FuncType(List<SimpleVariable> resMembers, List<SimpleVariable> argMembers, int flags)
	implements SimpleType {
	
	public static final int FLAG_EXPORT = 0x0100;
	public static final int FLAG_MAIN = 0x0200;
	public static final int FLAG_INIT = 0x0400;
	public static final int FLAG_FUNC_ADDRESS = 0x0800;
	public static final int FLAG_NOPAD = StructType.FLAG_NOPAD;
	public static final int ALL_FLAGS = FLAG_EXPORT | FLAG_MAIN | FLAG_INIT | FLAG_FUNC_ADDRESS | FLAG_NOPAD;
	public static final int STRUCTURAL_FLAGS = FLAG_NOPAD | FLAG_FUNC_ADDRESS;
	public static final int FLAG_NO_STRUCT = FLAG_FUNC_ADDRESS;
	
	static {
		if ( Integer.bitCount(ALL_FLAGS) != 5 ) {
			throw new AssertionError("StructType.FLAG_NOPAD uses a bit I want to use for an other purpose");
		}
	}
	
	public static final FuncType MAIN_TYPE = create(List.of(new SimpleVariable(NativeType.UBYTE, "exitnum", null, 0)),
		List.of(//
			new SimpleVariable(NativeType.UNUM, "argc", null, 0), //
			new SimpleVariable( //
				PointerType.create(PointerType.create(NativeType.UBYTE, NO_CONTEXT), NO_CONTEXT), //
				"argv", null, 0)//
		), FLAG_MAIN | FLAG_FUNC_ADDRESS, NO_CONTEXT);
		
	public static final FuncType INIT_TYPE = create(List.of(), List.of(), FLAG_INIT | FLAG_FUNC_ADDRESS, NO_CONTEXT);
	
	public static FuncType create(List<SimpleVariable> resMembers, List<SimpleVariable> argMembers, int flags,
		ErrorContext ctx) {
		argMembers = List.copyOf(argMembers);
		if ( ( flags & ALL_FLAGS ) != flags ) {
			throw new IllegalArgumentException("illegal flags value: " + Integer.toHexString(flags));
		}
		for (int i = 0, s = argMembers.size(); i < s; i++) {
			String name = argMembers.get(i).name();
			StructType.checkDupName(argMembers, i + 1, name, ctx);
			StructType.checkDupName(resMembers, 0, name, ctx);
		}
		for (int i = 0, s = resMembers.size(); i < s; i++) {
			String name = resMembers.get(i).name();
			StructType.checkDupName(resMembers, i + 1, name, ctx);
		}
		return new FuncType(resMembers, argMembers, flags);
	}
	
	public FuncType asFStruct() {
		if ( ( this.flags & FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new IllegalStateException("I am already a fstruct");
		}
		return new FuncType(this.resMembers, this.argMembers, this.flags & ~FLAG_FUNC_ADDRESS);
	}
	
	@Override
	public long size() {
		return Math.max(StructType.size(this.resMembers, this.flags), StructType.size(this.argMembers, this.flags));
	}
	
	@Override
	public int align() {
		return Math.max(StructType.align(this.resMembers, this.flags), StructType.align(this.argMembers, this.flags));
	}
	
	@Override
	public SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( !equals(type) ) {
			SimpleType.castErrImplicit(this, type, ctx);
		}
		return this;
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx, boolean explicit) throws CompileError {
		if ( !equals(type) ) {
			if ( explicit ) SimpleType.castErrExplicit(this, type, ctx);
			SimpleType.castErrImplicit(this, type, ctx);
		}
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public String toString(String idention) {
		StringBuilder sb = new StringBuilder();
		if ( ( this.flags & FLAG_EXPORT ) != 0 ) {
			sb.append("exp ");
		}
		if ( ( this.flags & FLAG_MAIN ) != 0 ) {
			sb.append("main ");
		}
		if ( ( this.flags & FLAG_INIT ) != 0 ) {
			sb.append("init ");
		}
		if ( ( this.flags & FLAG_FUNC_ADDRESS ) == 0 ) {
			sb.append("fstruct ");
		}
		if ( ( this.flags & FLAG_NOPAD ) != 0 ) {
			sb.append("nopad ");
		}
		toStringNoFlags(idention, sb);
		return sb.toString();
	}
	
	public void toStringNoFlags(String idention, StringBuilder sb) {
		idention = idention.concat("    ");
		sb.append('<');
		append(sb, idention, this.resMembers);
		sb.append("> <-- (");
		append(sb, idention, this.argMembers);
		sb.append(')');
	}
	
	private static void append(StringBuilder sb, String idention, List<SimpleVariable> m) {
		if ( !m.isEmpty() ) {
			SimpleVariable val = m.get(0);
			sb.append(val.type().toString(idention)).append(' ').append(val.name());
			for (int i = 1, s = m.size(); i < s; i++) {
				val = m.get(i);
				sb.append(", ").append(val.type().toString(idention)).append(' ').append(val.name());
			}
		}
	}
	
	@Override
	public String toStringSingleLine() {
		return toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.flags;
		result = StructType.hashCode(result, this.argMembers);
		result = StructType.hashCode(result, this.resMembers);
		return result;
	}
	
	public SimpleVariable memberOrNull(String name, ErrorContext ctx, boolean allowFuncAddr) {
		if ( !allowFuncAddr && ( this.flags & FLAG_FUNC_ADDRESS ) != 0 ) {
			throw new CompileError(ctx, "a function address is no structure!");
		}
		for (SimpleVariable sv : this.resMembers) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		for (SimpleVariable sv : this.argMembers) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		return null;
	}
	
	public SimpleVariable member(String name, ErrorContext ctx, boolean allowFuncAddr) {
		if ( !allowFuncAddr && ( this.flags & FLAG_FUNC_ADDRESS ) != 0 ) {
			throw new CompileError(ctx, "a function address is no structure!");
		}
		for (SimpleVariable sv : this.resMembers) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		for (SimpleVariable sv : this.argMembers) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		throw new CompileError(ctx,
			"the structure has no member with the given name! name: " + name + " " + toStringSingleLine());
	}
	
	public long offset(String name) {
		if ( ( this.flags & FLAG_FUNC_ADDRESS ) != 0 ) {
			throw new AssertionError("offset() called on a function address is no structure!");
		}
		long off = 0L;
		for (SimpleVariable sv : this.argMembers) {
			if ( ( flags & FLAG_NOPAD ) == 0 ) {
				int alignment = sv.type().align();
				int missAlign = (int) ( off % alignment );
				if ( missAlign != 0 ) {
					off += alignment - missAlign;
				}
			}
			if ( name.equals(sv.name()) ) {
				return off;
			}
			off += sv.type().size();
		}
		off = 0L;
		for (SimpleVariable sv : this.resMembers) {
			if ( ( flags & FLAG_NOPAD ) == 0 ) {
				int alignment = sv.type().align();
				int missAlign = (int) ( off % alignment );
				if ( missAlign != 0 ) {
					off += alignment - missAlign;
				}
			}
			if ( name.equals(sv.name()) ) {
				return off;
			}
			off += sv.type().size();
		}
		throw new AssertionError("could not find the member with name '" + name + "'");
	}
	
	public void checkHasMember(String name, ErrorContext ctx, boolean allowFuncAddr) {
		member(name, ctx, allowFuncAddr);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		FuncType other = (FuncType) obj;
		if ( this.flags != other.flags ) return false;
		if ( !StructType.equals(this.argMembers, other.argMembers) ) return false;
		return StructType.equals(this.resMembers, other.resMembers);
	}
	
	public boolean isInvokableBy(Object obj) {
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		FuncType other = (FuncType) obj;
		assert ( this.flags & FLAG_FUNC_ADDRESS ) != 0;
		if ( ( this.flags & STRUCTURAL_FLAGS & ~FLAG_FUNC_ADDRESS ) != ( other.flags & STRUCTURAL_FLAGS ) ) {
			return false;
		}
		if ( !StructType.equals(this.argMembers, other.argMembers) ) return false;
		return StructType.equals(this.resMembers, other.resMembers);
	}
	
	public boolean equalsIgnoreNonStructuralFlags(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		FuncType other = (FuncType) obj;
		if ( ( this.flags & STRUCTURAL_FLAGS ) != ( other.flags & STRUCTURAL_FLAGS ) ) return false;
		if ( !StructType.equals(this.argMembers, other.argMembers) ) return false;
		return StructType.equals(this.resMembers, other.resMembers);
	}
	
}
