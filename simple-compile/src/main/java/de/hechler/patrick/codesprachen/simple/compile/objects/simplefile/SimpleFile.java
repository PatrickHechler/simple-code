package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
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
	
	public void dependency(SimpleDependency dep, String name, ErrorContext ctx) {
		checkDuplicateName(name, ctx);
		this.dependencies.put(name, dep);
	}
	
	public void typedef(SimpleType type, String name, ErrorContext ctx) {
		checkDuplicateName(name, ctx);
		this.typedefs.put(name, type);
	}
	
	public void variable(SimpleVariable sv, String name, ErrorContext ctx) {
		checkDuplicateName(name, ctx);
		this.variables.put(name, sv);
	}
	
	public void function(SimpleFunction func, String name, ErrorContext ctx) {
		checkDuplicateName(name, ctx);
		this.functions.put(name, func);
		int flags = func.type().flags();
		if ( ( flags & FuncType.FLAG_INIT ) != 0 ) {
			if ( this.init != null ) {
				throw new CompileError(ctx, "there is already a function marked with init: "
					+ this.functions.get(this.init) + " second init: " + func);
			}
			this.init = name;
		}
		if ( ( flags & FuncType.FLAG_MAIN ) != 0 ) {
			if ( this.main != null ) {
				throw new CompileError(ctx, "there is already a function marked with main: "
					+ this.functions.get(this.main) + " second main: " + func);
			}
			this.main = name;
		}
	}
	
	private void checkDuplicateName(String name, ErrorContext ctx) {
		if ( this.dependencies.get(name) != null ) {
			throw new CompileError(ctx, "there is already a something (a dependency) with the name " + name + ": "
				+ this.dependencies.get(name));
		}
		if ( this.typedefs.get(name) != null ) {
			throw new CompileError(ctx,
				"there is already a something (a typedef) with the name " + name + ": " + this.typedefs.get(name));
		}
		if ( this.variables.get(name) != null ) {
			throw new CompileError(ctx,
				"there is already a something (a variable) with the name " + name + ": " + this.variables.get(name));
		}
		if ( this.functions.get(name) != null ) {
			throw new CompileError(ctx,
				"there is already a something (a function) with the name " + name + ": " + this.functions.get(name));
		}
	}
	
}
