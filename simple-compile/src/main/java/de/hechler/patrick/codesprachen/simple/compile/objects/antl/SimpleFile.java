package de.hechler.patrick.codesprachen.simple.compile.objects.antl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleStructure;

public class SimpleFile {
	
	private final Set <String>                  dependencies = new HashSet <>();
	private final Map <String, SimpleVariable>  vars         = new HashMap <>();
	private final Map <String, SimpleStructure> structs      = new HashMap <>();
	private final Map <String, SimpleFunction>  funcs        = new HashMap <>();
	
	
	public SimpleFile() {}
	
	public void addDependency(String depend) {
		dependencies.add(depend);
	}
	
	public void addVariable(SimpleVariable vari) {
		SimpleVariable old = vars.put(vari.name, vari);
		if (old != null) {
			throw new IllegalStateException("variable already exist: name: " + vari.name);
		}
	}
	
	public void addStructure(SimpleStructure struct) {
		SimpleStructure old = structs.put(struct.name, struct);
		if (old != null) {
			throw new IllegalStateException("structure already exist: name: " + struct.name);
		}
	}
	
	public void addFunction(SimpleFunction func) {
		SimpleFunction old = funcs.put(func.name, func);
		if (old != null) {
			throw new IllegalStateException("function already exist: name: " + func.name);
		}
	}
	
}
