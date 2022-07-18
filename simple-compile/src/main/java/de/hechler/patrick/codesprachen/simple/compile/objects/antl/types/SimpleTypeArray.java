package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;


public class SimpleTypeArray extends SimpleTypePointer {
	
	public final int elementCount;
	
	public SimpleTypeArray(SimpleType target, int elementCount) {
		super(target);
		this.elementCount = elementCount;
	}
	
	@Override
	public boolean isPointer() {
		return false;
	}
	
	@Override
	public boolean isArray() {
		return true;
	}
	
	@Override
	public int byteCount() {
		if (elementCount == -1) {
			return 0;
		}
		return target.byteCount() * elementCount;
	}
	
}
