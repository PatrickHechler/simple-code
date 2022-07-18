package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;

public class SimpleStructure implements SimpleType {
	
	public final String           name;
	/**
	 * this array is not allowed to be modified
	 */
	public final SimpleVariable[] members;
	
	public SimpleStructure(String name, List <SimpleVariable> members) {
		this.name = name;
		this.members = members.toArray(new SimpleVariable[members.size()]);
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
		if (getClass() != obj.getClass()) return false;
		SimpleStructure other = (SimpleStructure) obj;
		if (members.length != other.members.length) return false;
		for (int i = 0; i < members.length; i ++ ) {
			if ( !members[i].type.equals(other.members[i].type)) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(name).append(" { ");
		for (SimpleVariable sv : members) {
			builder.append(sv.type).append(' ').append(sv.name).append("; ");
		}
		builder.append("}");
		return builder.toString();
	}
	
}
