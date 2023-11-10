package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.DependencyVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.FunctionVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.VariableVal;

public class SimpleFile extends SimpleDependency {
	
	private Map<String, SimpleDependency> dependencies;
	private Map<String, SimpleType>       typedefs;
	private Map<String, SimpleVariable>   variables;
	private Map<String, SimpleFunction>   functions;
	private String                        main;
	private String                        init;
	
	public SimpleFile(String binaryTarget) {
		super(binaryTarget);
		this.dependencies = new HashMap<>();
		this.typedefs     = new HashMap<>();
		this.variables    = new HashMap<>();
		this.functions    = new HashMap<>();
	}
	
	@Override
	public SimpleValue nameValue(String name, ErrorContext ctx) {
		SimpleVariable sv = this.variables.get(name);
		if ( sv != null ) {
			return VariableVal.create(sv, ctx);
		}
		SimpleFunction func = this.functions.get(name);
		if ( func != null ) {
			return FunctionVal.create(func, ctx);
		}
		SimpleDependency dep = this.dependencies.get(name);
		if ( dep != null ) {
			return DependencyVal.create(dep, ctx);
		}
		throw new CompileError(ctx, "nothing with the name '" + name + "' could be found");
	}
	
}
