package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.ScalarNumericVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public record ArrayType(SimpleType target, long length) implements SimpleType {
	
	public ArrayType {
		Objects.requireNonNull(target, "array target");
		if ( length < -1L ) {
			throw new IllegalArgumentException("invalid length: " + length);
		}
	}
	
	public static ArrayType create(SimpleType target, long length, @SuppressWarnings( "unused" ) ErrorContext ctx) {
		return new ArrayType(target, length);
	}
	
	public static ArrayType create(SimpleType target, SimpleValue length, ErrorContext ctx) {
		if ( length == null ) create(target, -1L, ctx);
		length = length.superSimplify();
		if ( length instanceof ScalarNumericVal snv && snv.value() >= 0L ) {
			return create(target, snv.value(), ctx);
		}
		if ( length.type() instanceof NativeType nt && nt != NativeType.FPNUM && nt != NativeType.FPDWORD ) {
			throw new CompileError(ctx, "the array length value " + length + " is no compile time constant!");
		}
		throw new CompileError(ctx, "the array length value " + length + " is of an invalid type!");
	}
	
	@Override
	public long size() {
		if ( this.length <= 0L ) {
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
		return SimpleType.castErrImplicit(this, type, ctx);
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx, boolean explicit) throws CompileError {
		if ( type instanceof PointerType ) {
			return;
		}
		if ( type instanceof ArrayType ) {
			return;
		}
		if ( type == NativeType.UNUM || type == NativeType.NUM || type == null ) {
			return;
		}
		if ( explicit ) SimpleType.castErrExplicit(this, type, ctx);
		SimpleType.castErrImplicit(this, type, ctx);
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
