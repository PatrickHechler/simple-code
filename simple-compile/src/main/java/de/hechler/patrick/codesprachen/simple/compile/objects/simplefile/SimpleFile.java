package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.DependencyVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.FunctionVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.VariableVal;

public class SimpleFile extends SimpleDependency {
	
	private Map<String, SimpleDependency> dependencies;
	private Map<String, SimpleTypedef>    typedefs;
	private Map<String, SimpleVariable>   variables;
	private Map<String, SimpleFunction>   functions;
	private SimpleFunction                main;
	private SimpleFunction                init;
	
	public SimpleFile(String binaryTarget) {
		super(binaryTarget);
		this.dependencies = new HashMap<>();
		this.typedefs     = new HashMap<>();
		this.variables    = new HashMap<>();
		this.functions    = new HashMap<>();
	}
	
	@Override
	public SimpleValue nameValueOrNull(String name, ErrorContext ctx) {
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
		return null;
	}
	
	@Override
	public Object nameTypeOrDepOrFuncOrNull(String name, @SuppressWarnings("unused") ErrorContext ctx) {
		SimpleTypedef t = this.typedefs.get(name);
		if ( t != null ) return t.type();
		SimpleDependency d = this.dependencies.get(name);
		if ( d != null ) return d;
		SimpleFunction f = this.functions.get(name);
		if ( f != null ) return f;
		return null;
	}
	
	public void dependency(SimpleDependency dep, String name, ErrorContext ctx) {
		checkDuplicateName(name, ctx);
		this.dependencies.put(name, dep);
	}
	
	public void typedef(SimpleTypedef typedef, ErrorContext ctx) {
		checkDuplicateName(typedef.name(), ctx);
		this.typedefs.put(typedef.name(), typedef);
	}
	
	public void variable(SimpleVariable sv, String name, ErrorContext ctx) {
		checkDuplicateName(name, ctx);
		this.variables.put(name, sv);
	}
	
	public void function(SimpleFunction func, ErrorContext ctx) {
		checkDuplicateName(func.name(), ctx);
		this.functions.put(func.name(), func);
		int flags = func.type().flags();
		if ( ( flags & FuncType.FLAG_INIT ) != 0 ) {
			if ( this.init != null ) {
				throw new CompileError(ctx,
					"there is already a function marked with init: " + this.init + " second init: " + func);
			}
			this.init = func;
		}
		if ( ( flags & FuncType.FLAG_MAIN ) != 0 ) {
			if ( this.main != null ) {
				throw new CompileError(ctx,
					"there is already a function marked with main: " + this.main + " second main: " + func);
			}
			this.main = func;
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
