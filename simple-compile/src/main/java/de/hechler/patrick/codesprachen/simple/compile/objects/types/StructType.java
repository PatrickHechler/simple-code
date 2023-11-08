package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import java.util.List;
import java.util.Objects;

import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;

public record StructType(String name, List<SimpleVariable> members) implements SimpleType {
	
	public StructType(String name, List<SimpleVariable> members) {
		this.name    = Objects.requireNonNull(name, "name");
		this.members = List.copyOf(members);
	}
	
	@Override
	public long size() {
		if ( this.members.isEmpty() ) return 0L;
		long mySize     = this.members.get(0).type().size();
		int  totalAlign = this.members.get(0).type().align();
		for (int i = 1, s = this.members.size(); i < s; i++) {
			SimpleType t      = this.members.get(i).type();
			long       tsize  = t.size();
			int        talign = t.align();
			if ( talign > totalAlign ) {
				totalAlign = talign;
			}
			int remain = (int) mySize & talign;
			if ( remain != 0 ) {
				mySize += talign - remain;
			}
			mySize += tsize;
		}
		return mySize;
	}
	
	@Override
	public int align() {
		if ( this.members.isEmpty() ) return 0;
		return this.members.stream().mapToInt(a -> a.type().align()).max().orElse(0);
	}
	
	@Override
	public String toString() {
		return this.members.stream().reduce(new StringBuilder().append("struct ").append(this.name).append(" {\n"),
			(a, b) -> a.append("  ").append(b).append(";\n"), (a, b) -> a.append(b)).append("}").toString();
	}
	
}
