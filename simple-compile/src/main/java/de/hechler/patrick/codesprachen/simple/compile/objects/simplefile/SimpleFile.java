package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public class SimpleFile extends SimpleDependency {
	
	private Map<String, SimpleDependency> dependencies;
	private Map<String, SimpleVariable>   variables;
	private Map<String, SimpleType>       typedefs;
	private Map<String, SimpleFunction>   functions;
	private String                        main;
	private String                        init;
	
	public SimpleFile(String binaryTarget) {
		super(binaryTarget);
		this.dependencies = new HashMap<>();
		this.variables    = new HashMap<>();
		this.typedefs     = new HashMap<>();
		this.functions    = new HashMap<>();
	}
	
}
