package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;

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
				return extracted(nt);
			}
			return nt.extracted(this);
		}
		return SimpleType.castErr(this, type, ctx);
	}
	
	@SuppressWarnings("incomplete-switch")
	private NativeType extracted(NativeType type) {
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
	public void checkCastable(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( type instanceof NativeType ) {
			return;
		}
		if ( ( this == UNUM || this == NUM ) && type instanceof PointerType ) {
			return;
		}
		SimpleType.castErr(this, type, ctx);
	}
	
	@Override
	public String toString() {
		return name().toLowerCase();
	}
	
	@Override
	public String toStringSingleLine() {
		return toString();
	}
}
