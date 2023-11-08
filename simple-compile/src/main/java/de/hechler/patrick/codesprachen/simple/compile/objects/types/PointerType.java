package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.Objects;

public record PointerType(SimpleType target) implements SimpleType {
	
	public PointerType {
		Objects.requireNonNull(target, "pointer target");
	}
	
	@Override
	public long size() {
		return 8L;
	}
	
	@Override
	public int align() {
		return 8;
	}
	
	@Override
	public String toString() {
		return "(" + this.target + ")#";
	}
	
}
