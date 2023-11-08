package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.Objects;

public record ArrayType(SimpleType target, int length) implements SimpleType {
	
	public ArrayType {
		Objects.requireNonNull(target, "array target");
		if (length < -1) {
			throw new IllegalArgumentException("invalid length: " + length);
		}
	}
	
	@Override
	public long size() {
		if (this.length <= 0) {
			return 0L;
		}
		return this.target.size() * this.length;
	}
	
	@Override
	public int align() {
		return this.target.align();
	}
	
	@Override
	public String toString() {
		if ( this.length == -1 ) {
			return "(" + this.target + ")[]";
		}
		return "(" + this.target + ")[" + this.length + "]";
	}
	
}
