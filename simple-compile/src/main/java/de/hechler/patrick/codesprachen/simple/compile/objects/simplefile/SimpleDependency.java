package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;

public abstract class SimpleDependency implements SimpleScope {
	
	public final String binaryTarget;
	
	public SimpleDependency(String binaryTarget) {// NOSONAR
		this.binaryTarget = binaryTarget;
	}
	
}
