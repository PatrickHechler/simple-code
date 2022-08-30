package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.exportVars;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleVariable;

public class SimpleStructType implements SimpleType, SimpleExportable {
	
	public final String           name;
	public final boolean          export;
	/**
	 * this array should not be modified
	 */
	public final SimpleVariable[] members;
	
	public SimpleStructType(String name, boolean export, List <SimpleVariable> members) {
		this(name, export, members.toArray(new SimpleVariable[members.size()]));
	}
	
	public SimpleStructType(String name, boolean export, SimpleVariable[] members) {
		this.name = name;
		this.members = members;
		this.export = export;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public boolean isPrimitive() {
		return false;
	}
	
	@Override
	public boolean isPointerOrArray() {
		return false;
	}
	
	@Override
	public boolean isStruct() {
		return true;
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
	public boolean isFunc() {
		return false;
	}
	
	@Override
	public int byteCount() {
		int bytes = 0;
		for (SimpleVariable sv : members) {
			bytes += sv.type.byteCount();
		}
		return bytes;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (SimpleVariable sv : members) {
			result = prime * result + sv.type.hashCode();
		}
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if ( ! (obj instanceof SimpleType)) return false;
		if ( ! (obj instanceof SimpleStructType)) return false;
		SimpleStructType other = (SimpleStructType) obj;
		if (members.length != other.members.length) return false;
		for (int i = 0; i < members.length; i ++ ) {
			if ( !members[i].type.equals(other.members[i].type)) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("struct ").append(name).append(" { ");
		for (SimpleVariable sv : members) {
			builder.append(sv.type).append(' ').append(sv.name).append("; ");
		}
		builder.append("}");
		return builder.toString();
	}
	
	@Override
	public void appendToExportStr(StringBuilder build) {
		build.append(this.name);
	}
	
	@Override
	public boolean isExport() {
		return export;
	}
	
	@Override
	public String toExportString() {
		if (!export) {
			throw new IllegalStateException("this is not marked as export!");
		}
		StringBuilder b = new StringBuilder();
		b.append(STRUCT);
		b.append(this.name);
		b.append(STRUCT);
		exportVars(b, this.members);;
		b.append(STRUCT);
		return b.toString();
	}
	
}
