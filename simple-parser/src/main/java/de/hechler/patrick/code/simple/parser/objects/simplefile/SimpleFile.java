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
package de.hechler.patrick.code.simple.parser.objects.simplefile;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.value.DependencyVal;
import de.hechler.patrick.code.simple.parser.objects.value.FunctionVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;

public class SimpleFile extends SimpleDependency {
	
	private Map<String,SimpleDependency> dependencies;
	private Map<String,SimpleTypedef>    typedefs;
	private Map<String,SimpleVariable>   variables;
	private Map<String,SimpleFunction>   functions;
	private SimpleFunction               main;
	private SimpleFunction               init;
	
	public SimpleFile(String sourceFile, String binaryTarget) {
		super(sourceFile, binaryTarget);
		this.dependencies = new LinkedHashMap<>();
		this.typedefs = new LinkedHashMap<>();
		this.variables = new LinkedHashMap<>();
		this.functions = new LinkedHashMap<>();
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
	public Object nameTypeOrDepOrFuncOrNull(String name) {
		SimpleTypedef t = this.typedefs.get(name);
		if ( t != null ) return t.type();
		SimpleDependency d = this.dependencies.get(name);
		if ( d != null ) return d;
		SimpleFunction f = this.functions.get(name);
		if ( f != null ) return f;
		return null;
	}
	
	public void dependency(SimpleDependency dep, String name, ErrorContext ctx) {
		if ( name == null ) {
			mergeMeDep(dep);
			return;
		}
		checkDuplicateName(name, ctx, CDN_DEP);
		this.dependencies.compute(name, (n, old) -> {
			if ( old.sourceFile.equals(old.sourceFile) ) {
				if ( old.binaryTarget == null ) {
					return dep;
				}
				if ( old.binaryTarget.equals(dep.binaryTarget) ) {
					return old;
				}
			}
			nameUsed(name, "dependency", ctx);
		});
	}
	
	public void typedef(SimpleTypedef typedef, ErrorContext ctx) {
		checkDuplicateName(typedef.name(), ctx, CDN_TDF);
		this.typedefs.merge(typedef.name(), typedef, (old, td) -> {
			if ( ( old.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) == 0
				&& ( ( td.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) == 0 ) ) {
				nameUsed(td.name(), "typedef", ctx);
			}
			if ( !old.type().equals(td.type()) ) {
				nameUsed(td.name(), "typedef", ctx);
			}
			if ( ( old.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) != 0 ) {
				return td;
			}
			return old;
		});
	}
	
	public void variable(SimpleVariable sv, ErrorContext ctx) {
		checkDuplicateName(sv.name(), ctx, CDN_VAR);
		this.variables.merge(sv.name(), sv, (old, svar) -> {
			if ( ( old.flags() & SimpleVariable.FLAG_FROM_ME_DEP ) == 0
				&& ( ( svar.flags() & SimpleVariable.FLAG_FROM_ME_DEP ) == 0 ) ) {
				nameUsed(svar.name(), "variable", ctx);
			}
			if ( !old.type().equals(svar.type()) ) {
				nameUsed(svar.name(), "variable", ctx);
			}
			if ( ( old.flags() & ~( SimpleTypedef.FLAG_FROM_ME_DEP | SimpleTypedef.FLAG_EXPORT ) )//
				!= ( svar.flags() & ~( SimpleTypedef.FLAG_FROM_ME_DEP | SimpleTypedef.FLAG_EXPORT ) ) ) {
				nameUsed(svar.name(), "variable", ctx);
			}
			if ( old.initialValue() != null && svar.initialValue() != null ) {
				nameUsed(svar.name(), "variable", ctx);
			}
			if ( ( old.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) != 0 ) {
				return svar;
			}
			return old;
		});
	}
	
	public void function(SimpleFunction func, ErrorContext ctx) {
		checkDuplicateName(func.name(), ctx);
		this.functions.put(func.name(), func);
		int flags = func.type().flags();
		if ( ( flags & FuncType.FLAG_INIT ) != 0 ) {
			if ( this.init != null ) {
				ctx.setOffendingTokenCach("");
				throw new CompileError(ctx,
					"there is already a function marked with init: " + this.init + " second init: " + func);
			}
			if ( !func.type().equalsIgnoreNonStructuralFlags(FuncType.INIT_TYPE) ) {
				ctx.setOffendingTokenCach("");
				throw new CompileError(ctx, "a function marked with `init´ must be of type: " + FuncType.INIT_TYPE
					+ " but the function has the type: " + func.type());
			}
			this.init = func;
		}
		if ( ( flags & FuncType.FLAG_MAIN ) != 0 ) {
			if ( this.main != null ) {
				ctx.setOffendingTokenCach("");
				throw new CompileError(ctx,
					"there is already a function marked with main: " + this.main + " second main: " + func);
			}
			if ( !func.type().equalsIgnoreNonStructuralFlags(FuncType.MAIN_TYPE) ) {
				ctx.setOffendingTokenCach("");
				throw new CompileError(ctx, "a function marked with `main´ must be of type: " + FuncType.MAIN_TYPE
					+ " but the function has the type: " + func.type());
			}
			this.main = func;
		}
	}
	
	private static final int CDN_DEP = 1;
	private static final int CDN_TDF = 2;
	private static final int CDN_VAR = 3;
	private static final int CDN_FNC = 4;
	
	private void checkDuplicateName(String name, ErrorContext ctx, int type) {
		if ( type != CDN_DEP && this.dependencies.containsKey(name) ) {
			nameUsed(name, "dependency", ctx);
		}
		if ( type != CDN_TDF && this.typedefs.containsKey(name) ) {
			nameUsed(name, "typedef", ctx);
		}
		if ( type != CDN_VAR && this.variables.containsKey(name) ) {
			nameUsed(name, "variable", ctx);
		}
		if ( type != CDN_FNC && this.functions.containsKey(name) ) {
			nameUsed(name, "function", ctx);
		}
	}
	
	public SimpleDependency dependency(String name) {
		return this.dependencies.get(name);
	}
	
	public Collection<SimpleDependency> allDependencies() {
		return Collections.unmodifiableCollection(this.dependencies.values());
	}
	
	public SimpleTypedef typedef(String name) {
		return this.typedefs.get(name);
	}
	
	public Collection<SimpleTypedef> allTypedefs() {
		return Collections.unmodifiableCollection(this.typedefs.values());
	}
	
	public SimpleVariable variable(String name) {
		return this.variables.get(name);
	}
	
	public Collection<SimpleVariable> allVariables() {
		return Collections.unmodifiableCollection(this.variables.values());
	}
	
	public SimpleFunction function(String name) {
		return this.functions.get(name);
	}
	
	public Collection<SimpleFunction> allFunctions() {
		return Collections.unmodifiableCollection(this.functions.values());
	}
	
	public SimpleFunction init() {
		return this.init;
	}
	
	public SimpleFunction main() {
		return this.main;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (Entry<String,SimpleDependency> e : this.dependencies.entrySet()) {
			b.append("dep ").append(e.getKey()).append(" [PATH] \"").append(e.getValue().binaryTarget).append("\";\n");
		}
		for (SimpleTypedef t : this.typedefs.values()) {
			b.append("typedef ");
			if ( ( t.flags() & SimpleTypedef.FLAG_EXPORT ) != 0 ) {
				b.append("exp ");
			}
			if ( ( t.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) != 0 ) {
				b.append("<ME-DEPENDENCY> ");
			}
			b.append(t.type()).append(' ').append(t.name()).append(";\n");
		}
		for (SimpleVariable sv : this.variables.values()) {
			if ( ( sv.flags() & SimpleVariable.FLAG_CONSTANT ) != 0 ) {
				b.append("const ");
			}
			if ( ( sv.flags() & SimpleVariable.FLAG_EXPORT ) != 0 ) {
				b.append("exp ");
			}
			b.append(sv.type()).append(' ').append(sv.name());
			if ( sv.initialValue() != null ) {
				b.append(" <-- ").append(sv.initialValue());
			}
			b.append(";\n");
		}
		StringBuilder indent = new StringBuilder();
		for (SimpleFunction sf : this.functions.values()) {
			b.append("func ");
			if ( ( sf.type().flags() & FuncType.FLAG_EXPORT ) != 0 ) {
				b.append("exp ");
			}
			if ( ( sf.type().flags() & FuncType.FLAG_MAIN ) != 0 ) {
				b.append("main ");
			}
			if ( ( sf.type().flags() & FuncType.FLAG_INIT ) != 0 ) {
				b.append("init ");
			}
			if ( sf.name() != null ) {
				b.append(sf.name()).append(' ');
			}
			if ( ( sf.type().flags() & FuncType.FLAG_NOPAD ) != 0 ) {
				b.append("nopad ");
			}
			sf.type().toStringNoFlags("", b);
			if ( sf.block() != null ) {
				b.append(' ');
				sf.block().toString(b, indent);
				b.append('\n');
			} else {
				b.append(";\n");
			}
		}
		return b.toString();
	}
	
	public String toExportString() {
		StringBuilder b = new StringBuilder();
		for (Entry<String,SimpleDependency> e : this.dependencies.entrySet()) {
			if ( e.getValue().sourceFile != null ) {
				b.append("dep ").append(e.getKey()).append(" \"").append(e.getValue().sourceFile).append("\";\n");
			} else if ( !"std".equals(e.getKey()) ) {
				b.append("dep ").append(e.getKey()).append(" NULL \"").append(e.getValue().binaryTarget).append("\";\n");
			}
		}
		for (SimpleTypedef t : this.typedefs.values()) {
			if ( ( t.flags() & SimpleTypedef.FLAG_EXPORT ) == 0 ) continue;
			b.append("typedef exp ");
			if ( ( t.flags() & SimpleTypedef.FLAG_FROM_ME_DEP ) != 0 ) {
				b.append("<ERROR: THATS FROM A ME-DEPENDENCY> ");
			}
			b.append(t.type()).append(' ').append(t.name()).append(";\n");
		}
		for (SimpleVariable sv : this.variables.values()) {
			if ( ( sv.flags() & SimpleVariable.FLAG_EXPORT ) == 0 ) continue;
			if ( ( sv.flags() & SimpleVariable.FLAG_CONSTANT ) != 0 ) {
				b.append("const ");
			}
			b.append("exp ");
			b.append(sv.type()).append(' ').append(sv.name());
			if ( sv.initialValue() != null ) {
				b.append(" <-- ").append(sv.initialValue());
			}
			b.append(";\n");
		}
		for (SimpleFunction sf : this.functions.values()) {
			if ( ( sf.type().flags() & FuncType.FLAG_EXPORT ) == 0 ) continue;
			b.append("func exp ").append(sf.name()).append(' ');
			if ( ( sf.type().flags() & FuncType.FLAG_NOPAD ) != 0 ) {
				b.append("nopad ");
			}
			sf.type().toStringNoFlags("", b);
			if ( sf.block() != null ) {
				b.append(";\n");
			} else {
				b.append("{ ERROR: THERE IS NO BLOCK FOR THE EXPORTED FUNCTION }\n");
			}
		}
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return this.binaryTarget.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( !( obj instanceof SimpleFile sf ) ) return false;
		return this.binaryTarget.equals(sf.binaryTarget);
	}
	
}
