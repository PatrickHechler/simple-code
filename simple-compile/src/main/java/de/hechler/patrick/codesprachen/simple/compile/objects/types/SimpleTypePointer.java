package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.E_T_POINTER;

import java.util.Set;

public class SimpleTypePointer implements SimpleType {
	
	public final SimpleType target;
	
	public SimpleTypePointer(SimpleType target) {
		this.target = target;
	}
	
	@Override
	public boolean isPrimitive() {
		return false;
	}
	
	@Override
	public boolean isPointerOrArray() {
		return true;
	}
	
	@Override
	public boolean isPointer() {
		return true;
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
		return 8;
	}
	
	@Override
	public void appendToExportStr(StringBuilder build, Set <String> exportedStructs) {
		build.append(E_T_POINTER);
		target.appendToExportStr(build, exportedStructs);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(target).append('*');
		return b.toString();
	}
	
}
