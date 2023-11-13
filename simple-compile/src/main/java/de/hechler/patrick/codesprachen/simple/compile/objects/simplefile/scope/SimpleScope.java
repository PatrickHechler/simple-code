package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public interface SimpleScope {
	
	SimpleValue nameValueOrNull(String name, ErrorContext ctx);
	
	default SimpleValue nameValueOrErr(String name, ErrorContext ctx) throws CompileError {
		throw new CompileError(ctx, "nothing with the name '" + name + "' could be found");
	}
	
	Object nameTypeOrDepOrFuncOrNull(String typedefName, ErrorContext ctx);
	
}
