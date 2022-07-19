package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;

import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.E_T_STRUCT_END;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.E_T_STRUCT_NAME_END;
import static de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.E_T_STRUCT_START;

import java.util.List;
import java.util.Set;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;

public class SimpleStructType implements SimpleType {
	
	public final String           name;
	/**
	 * this array should not be modified
	 */
	public final SimpleVariable[] members;
	
	public SimpleStructType(String name, List <SimpleVariable> members) {
		this.name = name;
		this.members = members.toArray(new SimpleVariable[members.size()]);
	}
	
	public SimpleStructType(String name, SimpleVariable[] members) {
		this.name = name;
		this.members = members;
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
	public void appendToExportStr(StringBuilder build, Set <String> exportedStructs) {
		build.append(E_T_STRUCT_START).append(name).append(E_T_STRUCT_NAME_END);
		if (exportedStructs.add(name)) {
			for (SimpleVariable sv : members) {
				build.append(sv.type);
			}
			build.append(E_T_STRUCT_END);
		}
	}
	
}
