package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public interface SimpleValue {
	
	SimpleType type();
	
	ErrorContext ctx();
	
}
