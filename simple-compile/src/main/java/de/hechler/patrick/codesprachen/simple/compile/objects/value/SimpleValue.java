package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public interface SimpleValue {
	
	SimpleType type();
	
	ErrorContext ctx();
	
	default void checkAssignable(@SuppressWarnings( "unused" ) SimpleType type, ErrorContext ctx) throws CompileError {
		throw new CompileError(ctx, "this value (" + this + ") is not assignable");
	}
	
}
