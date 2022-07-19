package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;

import java.util.Set;

public interface SimpleType {
	
	public static final SimpleTypePrimitive FPNUM  = SimpleTypePrimitive.pt_fpnum;
	public static final SimpleTypePrimitive NUM    = SimpleTypePrimitive.pt_num;
	public static final SimpleTypePrimitive UNUM   = SimpleTypePrimitive.pt_unum;
	public static final SimpleTypePrimitive DWORD  = SimpleTypePrimitive.pt_dword;
	public static final SimpleTypePrimitive UDWORD = SimpleTypePrimitive.pt_udword;
	public static final SimpleTypePrimitive WORD   = SimpleTypePrimitive.pt_word;
	public static final SimpleTypePrimitive UWORD  = SimpleTypePrimitive.pt_uword;
	public static final SimpleTypePrimitive BYTE   = SimpleTypePrimitive.pt_byte;
	public static final SimpleTypePrimitive UBYTE  = SimpleTypePrimitive.pt_ubyte;
	
	/**
	 * returns <code>true</code> if this type is a {@link SimpleTypePrimitive} and <code>false</code> if not
	 * 
	 * @return <code>true</code> if this type is a {@link SimpleTypePrimitive} and <code>false</code> if not
	 */
	boolean isPrimitive();
	
	/**
	 * returns <code>true</code> if this type is a pointer or an array and <code>false</code> if not
	 * 
	 * @return <code>true</code> if this type is a pointer or an array and <code>false</code> if not
	 */
	boolean isPointerOrArray();
	
	/**
	 * returns <code>true</code> if this type is a pointer and <code>false</code> if not
	 * 
	 * @return <code>true</code> if this type is a pointer and <code>false</code> if not
	 */
	boolean isPointer();
	
	/**
	 * returns <code>true</code> if this type is an array and <code>false</code> if not
	 * 
	 * @return <code>true</code> if this type is an array and <code>false</code> if not
	 */
	boolean isArray();
	
	/**
	 * returns <code>true</code> if this type is a structure and <code>false</code> if not
	 * 
	 * @return <code>true</code> if this type is a structure and <code>false</code> if not
	 */
	boolean isStruct();
	
	/**
	 * returns <code>true</code> if this type is a function call structure and <code>false</code> if not
	 * <p>
	 * note that a function call structure is also a structure, so {@link #isStruct()} will also return <code>true</code>
	 * 
	 * @return <code>true</code> if this type is a function call structure and <code>false</code> if not
	 */
	boolean isFunc();
	
	/**
	 * returns the number of bytes used to represent a member of this type
	 * 
	 * @return the number of bytes used to represent a member of this type
	 */
	int byteCount();
	
	void appendToExportStr(StringBuilder build, Set <String> exportedStructs);
	
}
