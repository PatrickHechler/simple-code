package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;

public interface SimpleType {
	
	long size();
	
	int align();
	
	SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError;
	
	void checkCastable(SimpleType type, ErrorContext ctx) throws CompileError;
	
	String toStringSingleLine();
	
	static <T> T castErr(SimpleType from, SimpleType to, ErrorContext ctx) {
		throw new CompileError(ctx, "can't cast from " + from.toStringSingleLine() + " to " + to.toStringSingleLine());
	}
	
}
