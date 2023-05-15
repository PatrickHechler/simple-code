//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.FileTypes;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.TriFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.SimpleCompiler;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleNumberValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleStringValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleVariableValue;
import de.hechler.patrick.codesprachen.simple.compile.utils.StdLib;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

@SuppressWarnings({ "javadoc" })
public class SimpleFile implements SimplePool {
	
	private final TriFunction<String, String, String, SimpleDependency> dependencyProvider;
	private final Map<String, SimpleDependency>                         dependencies = new HashMap<>();
	private final Map<String, SimpleOffsetVariable>                     vars         = new LinkedHashMap<>();
	private final Map<String, SimpleStructType>                         structs      = new HashMap<>();
	private final Map<String, SimpleFunction>                           funcs        = new LinkedHashMap<>();
	private final Map<String, SimpleConstant>                           consts       = new HashMap<>();
	private final List<SimpleValueDataPointer>                          datas        = new ArrayList<>();
	private final List<SimpleExportable>                                exports      = new ArrayList<>();
	private SimpleFunction                                              main         = null;
	
	public SimpleFile(TriFunction<String, String, String, SimpleDependency> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.dependencies.put(StdLib.DEP.name(), StdLib.DEP);
	}
	
	private void checkName(String name) {
		if (this.dependencies.containsKey(name) || this.vars.containsKey(name) || this.structs.containsKey(name) || this.funcs.containsKey(name)
			|| this.consts.containsKey(name)) {
			throw new IllegalArgumentException("name already in use! name: '" + name + "'");
		}
	}
	
	public void addDependency(String name, String depend, String runtime) {
		checkName(name);
		this.dependencies.put(name, this.dependencyProvider.apply(name, depend, runtime));
	}
	
	public void addVariable(SimpleOffsetVariable vari) {
		checkName(vari.name);
		this.vars.put(vari.name, vari);
		if (vari.export) {
			this.exports.add(vari);
		}
	}
	
	public void addStructure(SimpleStructType struct) {
		checkName(struct.name);
		SimpleStructType old = this.structs.put(struct.name, struct);
		if (old != null) { throw new IllegalStateException("structure already exist: name: " + struct.name); }
	}
	
	private static final SimpleFuncType MAIN_TYPE = new SimpleFuncType(
		Arrays.asList(new SimpleOffsetVariable(SimpleType.NUM, "argc"),
			new SimpleOffsetVariable(new SimpleTypePointer(new SimpleTypePointer(SimpleType.UWORD)), "argv")),
		Arrays.asList(new SimpleOffsetVariable(SimpleType.NUM, "exitnum")));
	
	public void addFunction(SimpleFunction func) {
		checkName(func.name);
		if (func.main) {
			if (this.main != null) { throw new IllegalStateException("there is already a main function!"); }
			if (!MAIN_TYPE.equals(func.type)) {
				throw new IllegalStateException("the main function needs to have a head like this: '" + MAIN_TYPE + "'! (main: '" + func + "')");
			}
			this.main = func;
		}
		SimpleFunction old = this.funcs.put(func.name, func);
		if (old != null) { throw new IllegalStateException("function already exist: name: " + func.name); }
		if (func.export) {
			this.exports.add(func);
		}
	}
	
	public void addConstant(SimpleConstant constant) {
		checkName(constant.name());
		this.consts.put(constant.name(), constant);
	}
	
	public Collection<SimpleFunction> functions() {
		return this.funcs.values();
	}
	
	public Collection<SimpleDependency> dependencies() {
		return this.dependencies.values();
	}
	
	public SimpleFunction mainFunction() {
		return this.main;
	}
	
	public Collection<SimpleValueDataPointer> dataValues() {
		List<SimpleValueDataPointer> result = new ArrayList<>();
		for (SimpleDependency sd : dependencies.values()) {
			result.add(sd.path);
		}
		result.addAll(datas);
		return result;
	}
	
	public SimpleFuncPool newFuncPool(List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> results) {
		SimpleFuncType           type     = new SimpleFuncType(args, results);
		SimpleFunctionVariable[] funcargs = new SimpleFunctionVariable[type.arguments.length];
		for (int i = 0; i < funcargs.length; i++) {
			funcargs[i] = new SimpleFunctionVariable(type.arguments[i].type, type.arguments[i].name); // init done in count
		}
		UsedData used = new UsedData();
		count(used, Arrays.asList(funcargs));
		SimpleFunctionVariable[] funcres = new SimpleFunctionVariable[type.results.length];
		for (int i = 0; i < funcres.length; i++) {
			funcres[i] = new SimpleFunctionVariable(type.results[i].type, type.results[i].name);
			funcres[i].init(type.results[i].offset(), SimpleCompiler.REG_FUNC_STRUCT);
		}
		return new SimpleFuncPool(funcargs, funcres, used);
	}
	
	public static void count(UsedData used, Iterable<SimpleFunctionVariable> countTarget) {
		count(used, countTarget, null);
	}
	
	public static void count(UsedData used, SimpleCommandBlock countTarget) {
		count(used, countTarget.commands, countTarget);
	}
	
	public static void count(UsedData used, Iterable<?> countTarget, SimpleCommandBlock blk) {
		final int  startRegs = used.regs;
		final long startAddr = used.currentaddr;
		for (Object obj : countTarget) {
			if (obj instanceof SimpleCommandBlock scb) {
				count(used, scb.commands, scb);
			} else if (obj instanceof SimpleFunctionVariable sv) {
				long regLen = (sv.type.byteCount() >>> 3) + ((sv.type.byteCount() & 7) != 0 ? 1 : 0);
				if (used.regs < SimpleCompiler.MAX_VAR_REGISTER - regLen && !sv.watsPointer()) {
					sv.init(-1L, used.regs);
					used.regs += regLen;
				} else {
					long bc = sv.type.byteCount();
					used.currentaddr = align(used.currentaddr, bc);
					sv.init(used.currentaddr, SimpleCompiler.REG_VAR_PNTR);
					used.currentaddr += bc;
				}
			} else if (obj instanceof SimpleCommand sc) {
				sc.pool().initRegMax(used.regs);
			} else {
				throw new AssertionError("unknown class: '" + obj.getClass().getName() + "' of object: '" + obj + "'");
			}
		}
		if (used.currentaddr > used.maxaddr) {
			used.maxaddr = used.currentaddr;
		}
		if (used.regs > used.maxregs) {
			used.maxregs = used.regs;
		}
		used.regs        = startRegs;
		used.currentaddr = startAddr;
	}
	
	@Override
	public SimpleStructType getStructure(String name) {
		SimpleStructType struct = structs.get(name);
		if (struct != null) {
			return struct;
		} else {
			throw new NoSuchElementException("struct " + name + " (known structures: " + structs.values() + ")");
		}
	}
	
	public Collection<SimpleOffsetVariable> vars() {
		return vars.values();
	}
	
	@Override
	public void registerDataValue(SimpleValueDataPointer dataVal) {
		this.datas.add(dataVal);
	}
	
	@Override
	public SimpleValue newNameUseValue(String name) {
		SimpleVariable vari = vars.get(name);
		if (vari != null) { return new SimpleVariableValue(vari); }
		SimpleDependency dep = dependencies.get(name);
		if (dep != null) { return new SimpleVariableValue(dep); }
		SimpleConstant constant = consts.get(name);
		if (constant != null) { return new SimpleNumberValue(SimpleType.NUM, constant.value()); }
		throw new IllegalArgumentException("there is nothign with the given name! (name='" + name + "')");
	}
	
	@Override
	public SimpleDependency getDependency(String name) {
		SimpleDependency dep = dependencies.get(name);
		if (dep == null) throw new NoSuchElementException("there is no dependency with the name '" + name + "'");
		return dep;
	}
	
	@Override
	public SimpleFunction getFunction(String name) {
		SimpleFunction func = funcs.get(name);
		if (func == null) { throw new NoSuchElementException("there is no function with the name '" + name + "'"); }
		return func;
	}
	
	public SimpleExportable getExport(String name) {
		SimpleExportable se = funcs.get(name);
		if (se == null) {
			se = vars.get(name);
			if (se == null) {
				se = consts.get(name);
				if (se == null) { throw new NoSuchElementException("there is no export with the name '" + name + "'"); }
			}
		}
		if (!se.isExport()) {
			throw new NoSuchElementException("there is no export with the name '" + name + "' (the needed export is NOT declared as export)");
		}
		return se;
	}
	
	public Iterator<SimpleExportable> exportsIter() {
		return new FilteringIter<>(new MultiIter<>(consts.values().iterator(), funcs.values().iterator(), structs.values().iterator(), vars.values().iterator()),
			SimpleExportable::isExport);
	}
	
	@Override
	public Map<String, SimpleConstant> getConstants() { return consts; }
	
	@Override
	public Map<String, SimpleStructType> getStructures() { return structs; }
	
	@Override
	public Map<String, SimpleVariable> getVariables() { return new HashMap<>(vars); }
	
	@Override
	public void addCmd(@SuppressWarnings("unused") SimpleCommand add) {
		throw new UnsupportedOperationException("only block pools can add commands (this is a file pool)!");
	}
	
	@Override
	public void seal() {
		throw new UnsupportedOperationException("only block pools can be sealed (this is a file pool)!");
	}
	
	@Override
	public void initRegMax(@SuppressWarnings("unused") int regMax) {
		throw new UnsupportedOperationException("only block pools can initilize their regMax (this is a file pool)!");
	}
	
	@Override
	public int regMax() {
		throw new UnsupportedOperationException("only block pools can have a regMax (this is a file pool)!");
	}
	
	public class SimpleFuncPool implements SimplePool {
		
		// these arrays are not allowed to be modified
		public final SimpleFunctionVariable[] myargs;
		public final SimpleFunctionVariable[] myresults;
		private final UsedData                used;
		
		private SimpleFuncPool(SimpleFunctionVariable[] myargs, SimpleFunctionVariable[] myresults, UsedData used) {
			this.myargs    = myargs;
			this.myresults = myresults;
			this.used      = used;
		}
		
		public UsedData used() {
			return this.used.clone();
		}
		
		@Override
		public SimpleStructType getStructure(String name) {
			return SimpleFile.this.getStructure(name);
		}
		
		@Override
		public SimpleSubPool newSubPool() {
			return new SimpleSubPool(SimpleFuncPool.this);
		}
		
		@Override
		public SimpleValue newNameUseValue(String name) {
			for (SimpleFunctionVariable sv : myargs) {
				if (sv.name.equals(name)) { return new SimpleVariableValue(sv); }
			}
			for (SimpleFunctionVariable sv : myresults) {
				if (sv.name.equals(name)) { return new SimpleVariableValue(sv); }
			}
			return SimpleFile.this.newNameUseValue(name);
		}
		
		@Override
		public void registerDataValue(SimpleValueDataPointer dataVal) {
			SimpleFile.this.registerDataValue(dataVal);
		}
		
		@Override
		public SimpleDependency getDependency(String name) {
			return SimpleFile.this.getDependency(name);
		}
		
		@Override
		public SimpleFunction getFunction(String name) {
			return SimpleFile.this.getFunction(name);
		}
		
		@Override
		public Map<String, SimpleConstant> getConstants() { return SimpleFile.this.getConstants(); }
		
		@Override
		public Map<String, SimpleStructType> getStructures() { return SimpleFile.this.getStructures(); }
		
		@Override
		public Map<String, SimpleVariable> getVariables() {
			Map<String, SimpleVariable> res = SimpleFile.this.getVariables();
			for (SimpleFunctionVariable sv : myargs) {
				res.put(sv.name, sv);
			}
			for (SimpleFunctionVariable sv : myresults) {
				res.put(sv.name, sv);
			}
			return res;
		}
		
		@Override
		public void addCmd(@SuppressWarnings("unused") SimpleCommand add) {
			throw new UnsupportedOperationException("only block pools can add commands (this is a function pool)!");
		}
		
		@Override
		public void seal() {
			throw new UnsupportedOperationException("only block pools can be sealed (this is a function pool)!");
		}
		
		@Override
		public void initRegMax(@SuppressWarnings("unused") int size) {
			throw new UnsupportedOperationException("only block pools can initilize their regMax (this is a function pool)!");
		}
		
		@Override
		public int regMax() {
			throw new UnsupportedOperationException("only block pools can have a regMax (this is a function pool)!");
		}
		
	}
	
	public static class SimpleSubPool implements SimplePool, Cloneable {
		
		public final SimplePool         parent;
		public final SimpleCommandBlock block;
		
		public SimpleSubPool(SimplePool parent) {
			this.parent = parent;
			this.block  = new SimpleCommandBlock(this);
		}
		
		public SimpleSubPool(SimplePool parent, SimpleCommandBlock block) {
			this.parent = parent;
			this.block  = block.snapshot(this);
		}
		
		private SimplePool p() {
			SimplePool p = parent;
			while (p.getClass() == SimpleSubPool.class) {
				p = ((SimpleSubPool) p).parent;
			}
			return p;
		}
		
		@Override
		public SimpleStructType getStructure(String name) {
			return p().getStructure(name);
		}
		
		@Override
		public SimpleSubPool newSubPool() {
			return new SimpleSubPool(new SimpleSubPool(parent, block));
		}
		
		@Override
		public SimpleValue newNameUseValue(String name) {
			for (SimpleCommand cmd : block.commands) {
				if (cmd instanceof SimpleCommandVarDecl vd && vd.name.equals(name)) {
					return new SimpleVariableValue(vd);
				}
			}
			return parent.newNameUseValue(name);
		}
		
		@Override
		public void addCmd(SimpleCommand add) {
			block.addCmd(add);
		}
		
		@Override
		public void seal() {
			block.seal();
		}
		
		@Override
		public void initRegMax(int regMax) {
			block.initRegMax(regMax);
		}
		
		@Override
		public int regMax() {
			return block.regMax();
		}
		
		@Override
		public void registerDataValue(SimpleValueDataPointer dataVal) {
			p().registerDataValue(dataVal);
		}
		
		@Override
		public SimpleDependency getDependency(String name) {
			return p().getDependency(name);
		}
		
		@Override
		public SimpleFunction getFunction(String name) {
			return p().getFunction(name);
		}
		
		@Override
		public Map<String, SimpleConstant> getConstants() { return p().getConstants(); }
		
		@Override
		public Map<String, SimpleStructType> getStructures() { return p().getStructures(); }
		
		@Override
		public Map<String, SimpleVariable> getVariables() { return parent.getVariables(); }
		
	}
	
	@Override
	public SimpleSubPool newSubPool() {
		throw new InternalError("this method should be only called on function and sub pools not on the file pool!");
	}
	
	public static final SimpleType DEPENDENCY_TYPE = new SimpleType() { // @formatter:off
		@Override public boolean isStruct()                             { return true; }
		@Override public boolean isPrimitive()                          { return false; }
		@Override public boolean isPointerOrArray()                     { return false; }
		@Override public boolean isPointer()                            { return false; }
		@Override public boolean isArray()                              { return false; }
		@Override public boolean isFunc()                               { return false; }
		@Override public long    byteCount()                            { throw new UnsupportedOperationException(); }
		@Override public void    appendToExportStr(StringBuilder build) { throw new UnsupportedOperationException(); }
	}; // @formatter:on
	
	public abstract static class SimpleDependency extends SimpleVariable {
		
		public final SimpleValueDataPointer path;
		public final String                 depend;
		
		public SimpleDependency(String name, String runtimeDepend) {
			super(DEPENDENCY_TYPE, name, false);
			this.path   = new SimpleStringValue(runtimeDepend);
			this.depend = runtimeDepend;
		}
		
		public SimpleDependency(String name, String runtimeDepend, boolean allowNull) {
			super(DEPENDENCY_TYPE, name, false);
			if (allowNull && runtimeDepend == null) {
				this.path   = null;
				this.depend = null;
			} else {
				this.path   = new SimpleStringValue(runtimeDepend);
				this.depend = runtimeDepend;
			}
		}
		
		public static String runtimeName(String compileDepend) {
			return switch (FileTypes.getTypeFromName(compileDepend, FileTypes.PRIMITIVE_MASHINE_CODE)) {
			case PRIMITIVE_SYMBOL_FILE -> compileDepend.substring(0, compileDepend.length() - FileTypes.PRIMITIVE_SOURCE_CODE.getExtensionWithDot().length());
			case SIMPLE_SYMBOL_FILE -> compileDepend.substring(0, compileDepend.length() - FileTypes.PRIMITIVE_SOURCE_CODE.getExtensionWithDot().length());
			case PRIMITIVE_MASHINE_CODE, PRIMITIVE_SOURCE_CODE, SIMPLE_SOURCE_CODE -> compileDepend;
			};
		}
		
		public abstract SimpleExportable get(String name);
		
		public abstract Iterator<SimpleExportable> getAll();
		
		@Override
		public SimpleExportable changeRelative(Object relative) {
			throw new AssertionError("change relative on dependency called");
		}
		
		@Override
		public int hashCode() {
			return depend.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SimpleDependency other = (SimpleDependency) obj;
			return depend.equals(other.depend);
		}
		
		@Override
		public String toExportString() throws IllegalStateException {
			throw new AssertionError("this is a dependnecy (toExportString() was called)");
		}
		
	}
	
}
