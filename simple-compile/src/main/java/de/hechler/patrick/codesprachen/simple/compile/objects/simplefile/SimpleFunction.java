package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;

public class SimpleFunction {
	
	public final String   name;
	public final FuncType type;
	
	public SimpleFunction(String name, FuncType type) {
		this.name = name;
		this.type = type;
		if ((type.flags() & FuncType.FLAG_FUNC_ADDRESS) == 0) {
			throw new AssertionError();
		}
	}
	
}
