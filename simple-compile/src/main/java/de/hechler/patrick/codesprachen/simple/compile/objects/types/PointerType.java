package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.List;
import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;

public record PointerType(SimpleType target) implements SimpleType {
	
	/**
	 * a {@link PointerType} which points to a {@link StructType#FLAG_NOUSE non usable} structure
	 */
	public static final PointerType POINTER = new PointerType(new StructType(List.of(), StructType.FLAG_NOUSE));
	
	public PointerType {
		Objects.requireNonNull(target, "target");
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
	public SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( type instanceof PointerType pt ) {
			if ( this.target.equals(pt.target) ) {
				return this;
			}
			return POINTER;
		}
		if ( type instanceof ArrayType at ) {
			if ( this.target.equals(at.target()) ) {
				return this;
			}
			return POINTER;
		}
		if ( type == NativeType.UNUM || type == NativeType.NUM ) {
			return type;
		}
		return SimpleType.castErrImplicit(this, type, ctx);
		
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx, boolean explicit) throws CompileError {
		if ( type instanceof PointerType ) {
			return;
		}
		if ( type == NativeType.UNUM || type == NativeType.NUM ) {
			return;
		}
		if ( explicit ) SimpleType.castErrExplicit(this, type, ctx);
		SimpleType.castErrImplicit(this, type, ctx);
	}
	
	@Override
	public String toString() {
		return "(" + this.target + ")#";
	}
	
	@Override
	public String toStringSingleLine() {
		return "(" + this.target.toStringSingleLine() + ")#";
	}
	
}
