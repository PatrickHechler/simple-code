package de.hechler.patrick.codesprachen.simple.compile.objects.antl.types;

import java.util.Collections;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;


public class SimpleStructure implements SimpleType {

	public final String name;
	public final List<SimpleVariable> members;
	
	public SimpleStructure(String name, List <SimpleVariable> members) {
		this.name = name;
		this.members = Collections.unmodifiableList(members);
	}
	
}
