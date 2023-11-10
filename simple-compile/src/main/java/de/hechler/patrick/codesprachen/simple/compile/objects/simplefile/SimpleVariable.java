package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record SimpleVariable(SimpleType type, String name, int flags) {
	
	public static final int FLAG_CONSTANT = 0x0001;
	public static final int FLAG_EXPORT = 0x0002;
	public static final int ALL_FLAGS = FLAG_CONSTANT | FLAG_EXPORT;
	
	public SimpleVariable(SimpleType type, String name, int flags) {
		this.type  = Objects.requireNonNull(type, "type");
		this.name  = Objects.requireNonNull(name, "name");
		this.flags = flags & ALL_FLAGS;
		if ( this.flags != flags ) {
			throw new IllegalArgumentException("invalid flags: " + Integer.toHexString(flags));
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( ( this.flags & FLAG_CONSTANT ) != 0 ) {
			sb.append("const ");
		}
		if ( ( this.flags & FLAG_EXPORT ) != 0 ) {
			sb.append("exp ");
		}
		return sb.append(this.type).append(' ').append(this.name).append(';').toString();
	}
	
}
