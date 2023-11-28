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
package de.hechler.patrick.code.simple.parser.objects.types;

import java.util.List;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;

public record StructType(List<SimpleVariable> members, int flags) implements SimpleType {
	
	public static final int FLAG_NOPAD = 0x0010;
	/**
	 * prevents this structure from being used by any program (used by {@link PointerType#POINTER})
	 */
	public static final int FLAG_NOUSE = 0x0020;
	public static final int ALL_FLAGS = FLAG_NOPAD | FLAG_NOUSE;
	
	public static StructType create(List<SimpleVariable> members, int flags, ErrorContext ctx) {
		members = List.copyOf(members);
		if ( ( flags & ALL_FLAGS ) != flags ) {
			throw new IllegalArgumentException("illegal flags value: " + Integer.toHexString(flags));
		}
		for (int i = 0, s = members.size(); i < s; i++) {
			String name = members.get(i).name();
			checkDupName(members, i + 1, name, ctx);
		}
		return new StructType(members, flags);
	}
	
	static void checkDupName(List<SimpleVariable> members, int i, String name, ErrorContext ctx) {
		for (int ci = i, s = members.size(); ci < s; ci++) {
			if ( name.equals(members.get(ci).name()) ) {
				throw new CompileError(ctx, "duplicate name: " + name);
			}
		}
	}
	
	@Override
	public long size() {
		return size(this.members, this.flags);
	}
	
	static long size(List<SimpleVariable> members, int flags) {
		if ( members.isEmpty() ) return 0L;
		long mySize = members.get(0).type().size();
		int totalAlign = members.get(0).type().align();
		for (int i = 1, s = members.size(); i < s; i++) {
			SimpleType t = members.get(i).type();
			long tsize = t.size();
			if ( ( flags & FLAG_NOPAD ) == 0 ) {
				int talign = t.align();
				if ( talign > totalAlign ) {
					totalAlign = talign;
				}
				int remain = (int) mySize & talign;
				if ( remain != 0 ) {
					mySize += talign - remain;
				}
			}
			mySize += tsize;
		}
		return mySize;
	}
	
	@Override
	public int align() {
		return align(this.members, this.flags);
	}
	
	static int align(List<SimpleVariable> members, int flags) {
		if ( members.isEmpty() || ( flags & FLAG_NOPAD ) != 0 ) return 1;
		int align = 0;
		for (SimpleVariable sv : members) {
			int sva = sv.type().align();
			if ( sva > align ) {
				align = sva;
			}
		}
		return align;
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
		StringBuilder sb = new StringBuilder().append("struct ");
		if ( ( this.flags & FLAG_NOPAD ) != 0 ) {
			sb.append("nopad ");
		}
		sb.append('{');
		String inner = idention.concat("  ");
		for (SimpleVariable sv : this.members) {
			sb.append('\n').append(inner).append(sv.type().toString(inner)).append(' ').append(sv.name()).append(';');
		}
		if ( !this.members.isEmpty() ) {
			sb.append('\n').append(idention);
		}
		return sb.append('}').toString();
	}
	
	@Override
	public String toStringSingleLine() {
		StringBuilder sb = new StringBuilder().append("struct ");
		if ( ( this.flags & FLAG_NOPAD ) != 0 ) {
			sb.append("nopad ");
		}
		if ( ( this.flags & FLAG_NOUSE ) != 0 ) {
			sb.append("/* no use */ ");
		}
		sb.append('{');
		for (SimpleVariable sv : this.members) {
			sb.append(' ').append(sv.type().toStringSingleLine()).append(' ').append(sv.name()).append(';');
		}
		return sb.append('}').toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.flags;
		return hashCode(result, this.members);
	}
	
	static int hashCode(int result, List<SimpleVariable> m) {
		final int prime = 31;
		for (int i = 0, s = m.size(); i < s; i++) {
			result = prime * result + m.get(i).type().hashCode();
		}
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		StructType other = (StructType) obj;
		if ( this.flags != other.flags ) return false;
		return equals(this.members, other.members);
	}
	
	static boolean equals(List<SimpleVariable> mm, List<SimpleVariable> om) {
		if (mm == om) return true;
		int s = mm.size();
		if ( s != om.size() ) return false;
		for (int i = s; --i >= 0;) {
			if ( !mm.get(i).type().equals(om.get(i).type()) ) {
				return false;
			}
		}
		return true;
	}
	
	public void checkHasMember(String name, ErrorContext ctx) {
		member(name, ctx);
	}
	
	public SimpleVariable member(String name, ErrorContext ctx) {
		for (SimpleVariable sv : this.members) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		throw new CompileError(ctx,
			"the structure has no member with the given name! name: " + name + " " + toStringSingleLine());
	}
	
	public long offset(String name) {
		long off = 0L;
		for (SimpleVariable sv : this.members) {
			if ( ( this.flags & FLAG_NOPAD ) == 0 ) {
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
	
}
