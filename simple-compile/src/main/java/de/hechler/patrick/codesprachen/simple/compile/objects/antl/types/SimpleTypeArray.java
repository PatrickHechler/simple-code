package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;

import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.E_T_ARRAY;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.E_T_EMPTY_ARRAY;

import java.util.Set;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValueConst;

public class SimpleTypeArray extends SimpleTypePointer {
	
	public final int elementCount;
	
	public SimpleTypeArray(SimpleType target, int elementCount) {
		super(target);
		this.elementCount = elementCount;
	}
	
	public static SimpleTypeArray create(SimpleType target, SimpleValue val) {
		if (target.byteCount() <= 0) { // Occurs for example when an array has element count zero (or -1 for not told, but also implicit zero)
			throw new IllegalStateException("the child type of an array has to be positive! (child element byte count: " + target.byteCount() + " child type: " + target + ")");
		}
		if (val == null) {
			return new SimpleTypeArray(target, -1);
		} else if ( !val.isConstNoData()) {
			throw new IllegalStateException("arrays can only have a constant size (val: " + val + ")");
		} else {
			SimpleValueConst cval = (SimpleValueConst) val;
			if ( !cval.implicitNumber()) {
				throw new IllegalStateException("arrays can only have a constant size which has to be a number (no fp number) (value: " + val + ")");
			} else {
				long num = cval.getNumber();
				long size = num * target.byteCount();
				if (size > Integer.MAX_VALUE) {
					throw new AssertionError("arrays with a size larger than " + Integer.MAX_VALUE + " bytes are not supported! (element count: " + num + " byte size: " + size + " child type: " + target + ")");
				}
				return new SimpleTypeArray(target, (int) num);
			}
		}
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
	
	@Override
	public void appendToExportStr(StringBuilder build, Set <String> exportedStructs) {
		if (elementCount == -1) {
			build.append(E_T_EMPTY_ARRAY);
		} else {
			build.append(E_T_ARRAY).append(elementCount);
		}
		target.appendToExportStr(build, exportedStructs);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(target).append('[');
		if (elementCount != -1) {
			b.append(elementCount);
		}
		return b.append(']').toString();
	}
	
}
