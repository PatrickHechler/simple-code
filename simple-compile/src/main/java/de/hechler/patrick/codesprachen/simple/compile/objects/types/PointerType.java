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
package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.List;
import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;

public record PointerType(SimpleType target) implements SimpleType {
	
	/**
	 * a {@link PointerType} which points to a {@link StructType#FLAG_NOUSE non usable} structure
	 */
	public static final PointerType POINTER = create(
		StructType.create(List.of(), StructType.FLAG_NOUSE, ErrorContext.NO_CONTEXT), ErrorContext.NO_CONTEXT);
	
	public PointerType {
		Objects.requireNonNull(target, "target");
	}
	
	public static PointerType create(SimpleType target, @SuppressWarnings( "unused" ) ErrorContext ctx) {
		return new PointerType(target);
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
		if ( type == NativeType.UNUM || type == NativeType.NUM || type == null ) {
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
	public String toString(String idention) {
		return "(" + this.target.toString(idention) + ")#";
	}
	
	@Override
	public String toStringSingleLine() {
		return "(" + this.target.toStringSingleLine() + ")#";
	}
	
}
