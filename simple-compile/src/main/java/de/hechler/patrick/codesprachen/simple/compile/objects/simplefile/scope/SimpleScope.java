package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public interface SimpleScope {
	
	SimpleValue nameValue(String name, ErrorContext ctx) throws CompileError;
	
}
