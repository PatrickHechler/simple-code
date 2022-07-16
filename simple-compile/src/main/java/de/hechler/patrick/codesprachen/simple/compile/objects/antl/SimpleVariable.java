package de.hechler.patrick.codesprachen.simple.compile.objects.antl;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public class SimpleVariable {

	public final SimpleType type;
	public final String name;
	
	public SimpleVariable(SimpleType type, String name) {
		this.type = type;
		this.name = name;
	}
	
}
