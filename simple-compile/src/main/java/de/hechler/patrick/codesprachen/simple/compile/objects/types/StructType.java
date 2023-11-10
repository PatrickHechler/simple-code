package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;

public record StructType(List<SimpleVariable> members, int flags) implements SimpleType {
	
	public static final int FLAG_NOPAD = 0x0010;
	/**
	 * prevents this structure from being used by any program (used by {@link PointerType#POINTER})
	 */
	public static final int FLAG_NOUSE = 0x0020;
	public static final int ALL_FLAGS = FLAG_NOPAD | FLAG_NOUSE;
	
	public StructType(List<SimpleVariable> members, int flags) {
		this.members = List.copyOf(members);
		this.flags   = flags & ALL_FLAGS;
		if ( this.flags != flags ) {
			throw new IllegalArgumentException("illegal flags value: " + Integer.toHexString(flags));
		}
	}
	
	@Override
	public long size() {
		if ( this.members.isEmpty() ) return 0L;
		long mySize     = this.members.get(0).type().size();
		int  totalAlign = this.members.get(0).type().align();
		for (int i = 1, s = this.members.size(); i < s; i++) {
			SimpleType t     = this.members.get(i).type();
			long       tsize = t.size();
			if ( ( this.flags & FLAG_NOPAD ) == 0 ) {
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
		if ( this.members.isEmpty() || ( this.flags & FLAG_NOPAD ) != 0 ) return 1;
		return this.members.stream().mapToInt(a -> a.type().align()).max().orElse(0);
	}
	
	@Override
	public SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( !equals(type) ) {
			SimpleType.castErr(this, type, ctx);
		}
		return this;
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( !equals(type) ) {
			SimpleType.castErr(this, type, ctx);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append("struct ");
		if ( ( this.flags & FLAG_NOPAD ) != 0 ) {
			sb.append("nopad ");
		}
		sb.append('{');
		for (SimpleVariable sv : this.members) {
			sb.append("\n  ").append(sv).append(';');
		}
		if (!this.members.isEmpty()) {
			sb.append('\n');
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
			sb.append(' ').append(sv).append(';');
		}
		return sb.append('}').toString();
	}
	
	@Override
	public int hashCode() {
		final int            prime  = 31;
		int                  result = 1;
		List<SimpleVariable> m      = this.members;
		result = prime * result + this.flags;
		for (int i = 0, s = m.size(); i < s; i++) {
			result = prime * result + m.get(i).type().hashCode();
		}
		result = prime * result + m.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		StructType           other = (StructType) obj;
		if ( this.flags != other.flags ) return false;
		List<SimpleVariable> mm    = this.members;
		List<SimpleVariable> om    = other.members;
		int                  s     = mm.size();
		if ( s != om.size() ) return false;
		for (int i = s; --i >= 0;) {
			if ( mm.get(i).type().equals(om.get(i).type()) ) {
				return false;
			}
		}
		return true;
	}
	
}
