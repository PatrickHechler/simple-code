package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.StructType;

public record DependencyVal(SimpleDependency dep, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleDependency dep, ErrorContext ctx) {
		return new DependencyVal(dep, ctx);
	}
	
	@Override
	public SimpleType type() {
		new Throwable("DependencyVal.type() called").printStackTrace();
		return StructType.create(List.of(), StructType.FLAG_NOUSE, ErrorContext.NO_CONTEXT);
	}
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public String toString() {
		return "(dependency: " + dep.binaryTarget + ")";
	}
	
}
