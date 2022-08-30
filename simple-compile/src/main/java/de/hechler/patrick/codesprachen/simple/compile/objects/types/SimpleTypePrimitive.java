package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_BYTE;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_DWORD;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_FPNUM;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_NUM;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_UBYTE;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_UDWORD;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_UNUM;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_UWORD;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.PRIM_WORD;

public enum SimpleTypePrimitive implements SimpleType {
	
	pt_num(64, true), pt_unum(64, false), pt_dword(32, true), pt_udword(32, false), pt_word(16, true), pt_uword(16, false), pt_byte(8, true), pt_ubyte(8, false),
	pt_fpnum(64, true) {
		
		@Override
		public boolean signed() {
			throw new IllegalStateException("fpnum");
		}
		
	},
	pt_inval( -1, false) {
		
		@Override
		public int bits() {
			throw new IllegalStateException("inval");
		}
		
		@Override
		public boolean signed() {
			throw new IllegalStateException("inval");
		}
		
		@Override
		public int byteCount() {
			throw new IllegalStateException("inval");
		}
		
	};
	
	public static SimpleType get(int bits, boolean signed) {
		switch (bits) {
		case 64:
			if (signed) {
				return pt_num;
			} else {
				return pt_num;
			}
		case 32:
			if (signed) {
				return pt_dword;
			} else {
				return pt_udword;
			}
		case 16:
			if (signed) {
				return pt_word;
			} else {
				return pt_uword;
			}
		case 8:
			if (signed) {
				return pt_byte;
			} else {
				return pt_ubyte;
			}
		default:
			throw new InternalError();
		}
	}
	
	private final int     bits;
	private final boolean signed;
	
	private SimpleTypePrimitive(int bits, boolean signed) {
		this.bits = bits;
		this.signed = signed;
	}
	
	public int bits() {
		return bits;
	}
	
	public boolean signed() {
		return signed;
	}
	
	@Override
	public boolean isPrimitive() {
		return true;
	}
	
	@Override
	public boolean isPointerOrArray() {
		return false;
	}
	
	@Override
	public boolean isPointer() {
		return false;
	}
	
	@Override
	public boolean isArray() {
		return false;
	}
	
	@Override
	public boolean isStruct() {
		return false;
	}
	
	@Override
	public boolean isFunc() {
		return false;
	}
	
	@Override
	public int byteCount() {
		return bits >> 3;
	}
	
	@Override
	public String toString() {
		return name().substring(3);
	}
	
	@Override
	public void appendToExportStr(StringBuilder build) {
		switch (this) {
		case pt_fpnum:
			build.append(PRIM_FPNUM);
			break;
		case pt_unum:
			build.append(PRIM_UNUM);
			break;
		case pt_num:
			build.append(PRIM_NUM);
			break;
		case pt_udword:
			build.append(PRIM_UDWORD);
			break;
		case pt_dword:
			build.append(PRIM_DWORD);
			break;
		case pt_uword:
			build.append(PRIM_UWORD);
			break;
		case pt_word:
			build.append(PRIM_WORD);
			break;
		case pt_ubyte:
			build.append(PRIM_UBYTE);
			break;
		case pt_byte:
			build.append(PRIM_BYTE);
			break;
		case pt_inval:
			throw new InternalError("the primitive type inval is only for intern use, thus it can not be exported!");
		default:
			throw new InternalError("unknown primitive type: " + name());
		}
	}
	
}
