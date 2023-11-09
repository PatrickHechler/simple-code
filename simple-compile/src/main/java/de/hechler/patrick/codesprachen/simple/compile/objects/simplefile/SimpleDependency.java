package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

public abstract class SimpleDependency {
	
	public final String binaryTarget;
	
	public SimpleDependency(String binaryTarget) {// NOSONAR
		this.binaryTarget = binaryTarget;
	}
	
}
