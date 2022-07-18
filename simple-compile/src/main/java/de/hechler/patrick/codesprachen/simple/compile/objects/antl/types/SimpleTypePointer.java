package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;


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
		return 64;
	}
	
}
