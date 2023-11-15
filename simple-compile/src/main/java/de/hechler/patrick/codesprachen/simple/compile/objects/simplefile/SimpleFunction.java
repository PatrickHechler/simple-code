package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.BlockCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;

public record SimpleFunction(String name, FuncType type, BlockCmd block) {
	
	public SimpleFunction(String name, FuncType type) {
		this(name, type, null);
	}
	
	public SimpleFunction {
		if ( ( type.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new AssertionError();
		}
	}
	
}
