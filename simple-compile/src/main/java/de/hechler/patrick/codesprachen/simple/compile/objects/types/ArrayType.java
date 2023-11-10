package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;

public record ArrayType(SimpleType target, int length) implements SimpleType {
	
	public ArrayType {
		Objects.requireNonNull(target, "array target");
		if ( length < -1 ) {
			throw new IllegalArgumentException("invalid length: " + length);
		}
	}
	
	@Override
	public long size() {
		if ( this.length <= 0 ) {
			return 0L;
		}
		return this.target.size() * this.length;
	}
	
	@Override
	public int align() {
		return this.target.align();
	}
	
	@Override
	public SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( type instanceof PointerType pt ) {
			if ( this.target.equals(pt.target()) ) {
				return this;
			}
			return PointerType.POINTER;
		}
		if ( type instanceof ArrayType at ) {
			if ( this.target.equals(at.target()) ) {
				return this;
			}
			return PointerType.POINTER;
		}
		if ( type == NativeType.UNUM || type == NativeType.NUM ) {
			return type;
		}
		return SimpleType.castErr(this, type, ctx);
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( type instanceof PointerType ) {
			return;
		}
		if ( type instanceof ArrayType ) {
			return;
		}
		if ( type == NativeType.UNUM || type == NativeType.NUM ) {
			return;
		}
		SimpleType.castErr(this, type, ctx);
	}
	
	@Override
	public String toString() {
		if ( this.length == -1 ) {
			return "(" + this.target + ")[]";
		}
		return "(" + this.target + ")[" + this.length + "]";
	}
	
	@Override
	public String toStringSingleLine() {
		if ( this.length == -1 ) {
			return "(" + this.target.toStringSingleLine() + ")[]";
		}
		return "(" + this.target.toStringSingleLine() + ")[" + this.length + "]";
	}
	
}
