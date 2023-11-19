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
package de.hechler.patrick.codesprachen.simple.compile.interpreter.objects.types;

import de.hechler.patrick.codesprachen.simple.compile.interpreter.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.interpreter.error.ErrorContext;

public enum NativeType implements SimpleType {
	
	NUM, UNUM,
	
	FPNUM, FPDWORD,
	
	DWORD, UDWORD,
	
	WORD, UWORD,
	
	BYTE, UBYTE,
	
	;
	
	@Override
	public long size() {
		switch ( this ) {
		case BYTE, UBYTE:
			return 1L;
		case WORD, UWORD:
			return 2L;
		case DWORD, UDWORD, FPDWORD:
			return 4L;
		case NUM, UNUM, FPNUM:
			return 8L;
		}
		throw new AssertionError("unknown NativeType: " + name());
	}
	
	@Override
	public int align() {
		switch ( this ) {
		case BYTE, UBYTE:
			return 1;
		case WORD, UWORD:
			return 2;
		case DWORD, UDWORD, FPDWORD:
			return 4;
		case NUM, UNUM, FPNUM:
			return 8;
		}
		throw new AssertionError("unknown NativeType: " + name());
	}
	
	@Override
	public SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( type instanceof NativeType nt ) {
			if ( this == nt ) return this;
			if ( this == FPNUM || nt == FPNUM ) {
				return FPNUM;
			}
			if ( this == FPDWORD || nt == FPDWORD ) {
				return FPDWORD;
			}
			if ( this.size() > nt.size() ) {
				return scalarCommonType(nt);
			}
			return nt.scalarCommonType(this);
		}
		return SimpleType.castErrImplicit(this, type, ctx);
	}
	
	@SuppressWarnings("incomplete-switch")
	private NativeType scalarCommonType(NativeType type) {
		switch ( this ) {// NOSONAR
		case NUM, DWORD, WORD, BYTE:
			return this;
		case UNUM:
			switch ( type ) {// NOSONAR
			case NUM, DWORD, WORD, BYTE:
				return NUM;
			default:
				return this;
			}
		case UDWORD:
			switch ( type ) {// NOSONAR
			case DWORD, WORD, BYTE:
				return DWORD;
			default:
				return this;
			}
		case UWORD:
			if ( type == BYTE || type == WORD ) {
				return BYTE;
			}
			return this;
		case UBYTE:
			if ( type == BYTE ) {
				return BYTE;
			}
			return this;
		}
		throw new AssertionError("this method is only allowed to be called with floating point types");
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx, boolean explicit) throws CompileError {
		if ( type instanceof NativeType || type == null ) {
			return;
		}
		if ( ( this == UNUM || this == NUM ) && type instanceof PointerType ) {
			return;
		}
		if ( explicit ) SimpleType.castErrExplicit(this, type, ctx);
		SimpleType.castErrImplicit(this, type, ctx);
	}
	
	@Override
	public String toString() {
		return name().toLowerCase();
	}
	
	@Override
	public String toString(@SuppressWarnings("unused") String idention) {
		return toString();
	}
	
	@Override
	public String toStringSingleLine() {
		return toString();
	}
}
