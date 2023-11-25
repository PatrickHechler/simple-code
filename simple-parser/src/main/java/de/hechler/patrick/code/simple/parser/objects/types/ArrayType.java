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

import java.util.Objects;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.value.ScalarNumericVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public record ArrayType(SimpleType target, long elementCount) implements SimpleType {
	
	public ArrayType {
		Objects.requireNonNull(target, "array target");
		if ( elementCount < -1L ) {
			throw new IllegalArgumentException("invalid length: " + elementCount);
		}
	}
	
	public static ArrayType create(SimpleType target, long length, @SuppressWarnings("unused") ErrorContext ctx) {
		return new ArrayType(target, length);
	}
	
	public static ArrayType create(SimpleType target, SimpleValue length, ErrorContext ctx) {
		if ( length == null ) return create(target, -1L, ctx);
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
		if ( this.elementCount <= 0L ) {
			return 0L;
		}
		return this.target.size() * this.elementCount;
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
		if ( this.elementCount == -1 ) {
			return "(" + this.target + ")[]";
		}
		return "(" + this.target + ")[" + this.elementCount + "]";
	}
	
	@Override
	public String toString(String idention) {
		if ( this.elementCount == -1 ) {
			return "(" + this.target.toString(idention) + ")[]";
		}
		return "(" + this.target.toString(idention) + ")[" + this.elementCount + "]";
	}
	
	@Override
	public String toStringSingleLine() {
		if ( this.elementCount == -1 ) {
			return "(" + this.target.toStringSingleLine() + ")[]";
		}
		return "(" + this.target.toStringSingleLine() + ")[" + this.elementCount + "]";
	}
	
}
