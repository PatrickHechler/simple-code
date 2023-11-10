package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.StructType;

public record NameVal(String name) implements SimpleValue {
	
	@Override
	public SimpleType type() {
		new Throwable("DependencyVal.type() called").printStackTrace();
		return StructType.create(List.of(), StructType.FLAG_NOUSE, ErrorContext.NO_CONTEXT);
	}
	
	@Override
	public ErrorContext ctx() {
		return ErrorContext.NO_CONTEXT;
	}
	
}
