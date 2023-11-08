package de.hechler.patrick.codesprachen.simple.compile.objects.types;

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
	public String toString() {
		return name().toLowerCase();
	}
	
}
