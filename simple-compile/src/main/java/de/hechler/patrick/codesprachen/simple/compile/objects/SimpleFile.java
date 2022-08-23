package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.TriFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleTypePointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleStringValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleVariableValue;

public class SimpleFile implements SimplePool {
	
	private final TriFunction <String, String, String, SimpleDependency> dependencyProvider;
	private final Map <String, SimpleDependency>                         dependencies = new HashMap <>();
	private final Map <String, SimpleVariable>                           vars         = new LinkedHashMap <>();
	private final Map <String, SimpleStructType>                         structs      = new HashMap <>();
	private final Map <String, SimpleFunction>                           funcs        = new LinkedHashMap <>();
	private final List <SimpleValueDataPointer>                          datas        = new ArrayList <>();
	private final List <SimpleExportable>                                exports      = new ArrayList <>();
	private SimpleFunction                                               main         = null;
	
	public SimpleFile(TriFunction <String, String, String, SimpleDependency> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}
	
	private void checkName(String name) {
		if (dependencies.containsKey(name)
			|| vars.containsKey(name)
			|| structs.containsKey(name)
			|| funcs.containsKey(name)) {
			throw new IllegalArgumentException("name already in use! name: '" + name + "'");
		}
	}
	
	public void addDependency(String name, String depend, String runtime) {
		checkName(name);
		dependencies.put(name, dependencyProvider.apply(name, depend, runtime));
	}
	
	public void addVariable(SimpleVariable vari) {
		checkName(vari.name);
		vars.put(vari.name, vari);
		if (vari.export) {
			this.exports.add(vari);
		}
	}
	
	public void addStructure(SimpleStructType struct) {
		checkName(struct.name);
		SimpleStructType old = structs.put(struct.name, struct);
		if (old != null) {
			throw new IllegalStateException("structure already exist: name: " + struct.name);
		}
	}
	
	private static final SimpleFuncType MAIN_TYPE = new SimpleFuncType(
		Arrays.asList(new SimpleVariable(SimpleType.NUM, "argc", false), new SimpleVariable(new SimpleTypePointer(new SimpleTypePointer(SimpleType.UWORD)), "argv", false)),
		Arrays.asList(new SimpleVariable(SimpleType.NUM, "exitnum", false)));
	
	public void addFunction(SimpleFunction func) {
		checkName(func.name);
		if (func.main) {
			if (this.main != null) {
				throw new IllegalStateException("there is already a main function!");
			}
			if ( !MAIN_TYPE.equals(func.type)) {
				throw new IllegalStateException("the main function needs to have a head like this: '" + MAIN_TYPE + "'! (main: '" + func + "')");
			}
			this.main = func;
		}
		SimpleFunction old = funcs.put(func.name, func);
		if (old != null) {
			throw new IllegalStateException("function already exist: name: " + func.name);
		}
		if (func.export) {
			this.exports.add(func);
		}
	}
	
	public Collection <SimpleFunction> functions() {
		return this.funcs.values();
	}
	
	public SimpleFunction mainFunction() {
		return this.main;
	}
	
	public Collection <SimpleValueDataPointer> dataValues() {
		List <SimpleValueDataPointer> result = new ArrayList <>();
		for (SimpleDependency sd : dependencies.values()) {
			result.add(sd.path);
		}
		result.addAll(datas);
		return result;
	}
	
	public SimplePool newFuncPool(List <SimpleVariable> args, List <SimpleVariable> results) {
		return new SimpleFuncPool(new SimpleFuncType(args, results));
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
	
	public Collection <SimpleVariable> vars() {
		return vars.values();
	}
	
	@Override
	public void registerDataValue(SimpleValueDataPointer dataVal) {
		this.datas.add(dataVal);
	}
	
	@Override
	public SimpleValue newNameUseValue(String name) {
		SimpleVariable vari = vars.get(name);
		if (vari != null) {
			return new SimpleVariableValue(vari);
		}
		SimpleDependency dep = dependencies.get(name);
		if (dep != null) {
			return new SimpleVariableValue(dep);
		}
		throw new IllegalArgumentException("there is nothign with the given name!");
	}
	
	@Override
	public SimpleDependency getDependency(String name) {
		SimpleDependency dep = dependencies.get(name);
		if (dep == null) {
			throw new NoSuchElementException("there is no dependency with the name '" + name + "'");
		}
		return dep;
	}
	
	@Override
	public SimpleFunction getFunction(String name) {
		SimpleFunction func = funcs.get(name);
		if (func == null) {
			throw new NoSuchElementException("there is no function with the name '" + name + "'");
		}
		return func;
	}
	
	public SimpleExportable getExport(String name) {
		SimpleExportable se = funcs.get(name);
		if (se == null) {
			se = vars.get(name);
			if (se == null) {
				throw new NoSuchElementException("there is no export with the name '" + name + "'");
			}
		}
		if ( !se.isExport()) {
			throw new NoSuchElementException("there is no export with the name '" + name + "' (the needed export is NOT declared as export)");
		}
		return se;
	}
	
	public class SimpleFuncPool implements SimplePool {
		
		private final SimpleFuncType func;
		
		public SimpleFuncPool(SimpleFuncType func) {
			this.func = func;
		}
		
		@Override
		public SimpleStructType getStructure(String name) {
			return SimpleFile.this.getStructure(name);
		}
		
		@Override
		public SimplePool newSubPool() {
			return new SimpleSubPool(SimpleFuncPool.this);
		}
		
		@Override
		public void initBlock(SimpleCommandBlock block) {
			throw new InternalError("this method should be called only on sub pools!");
		}
		
		@Override
		public SimpleValue newNameUseValue(String name) {
			for (SimpleVariable sv : func.arguments) {
				if (sv.name.equals(name)) {
					return new SimpleVariableValue(sv);
				}
			}
			for (SimpleVariable sv : func.results) {
				if (sv.name.equals(name)) {
					return new SimpleVariableValue(sv);
				}
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
		
	}
	
	public static class SimpleSubPool implements SimplePool {
		
		public final SimplePool   parent;
		public SimpleCommandBlock block;
		
		public SimpleSubPool(SimplePool parent) {
			this.parent = parent;
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
		public SimplePool newSubPool() {
			return new SimpleSubPool(this);
		}
		
		@Override
		public void initBlock(SimpleCommandBlock block) {
			if (block != null) {
				throw new IllegalStateException("blockalready initilized!");
			}
			this.block = block;
		}
		
		@Override
		public SimpleValue newNameUseValue(String name) {
			for (SimpleCommand cmd : block.cmds) {
				if (cmd instanceof SimpleCommandVarDecl) {
					SimpleCommandVarDecl vd = (SimpleCommandVarDecl) cmd;
					if (vd.name.equals(name)) {
						return new SimpleVariableValue(vd);
					}
				}
			}
			return parent.newNameUseValue(name);
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
		
	}
	
	@Override
	public SimplePool newSubPool() {
		throw new InternalError("this method should be only called on function and sub pools not on the file pool!");
	}
	
	@Override
	public void initBlock(SimpleCommandBlock block) {
		throw new InternalError("this method should be only called on sub pools not on the file pool!");
	}
	
	public static final SimpleType DEPENDENCY_TYPE = new SimpleStructType("--DEPENDENCY--", Collections.emptyList()) {
		
		public boolean isStruct() {
			return false;
		};
		
		@Override
		public int hashCode() {
			return "--DEPENDENCY--".hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}
		
	};
	
	public static abstract class SimpleDependency extends SimpleVariable {
		
		public final SimpleValueDataPointer path;
		public final String                 depend;
		
		public SimpleDependency(String name, String depend) {
			super(DEPENDENCY_TYPE, name, false);
			this.path = new SimpleStringValue(normalize(depend));
			this.depend = depend;
		}
		
		
		public abstract SimpleExportable get(String name);
		
		private static final List <String> normalize(String depend) {
			String[] segs = depend.replace('\\', '/').split("\\/");
			int len, start, i;
			for (start = 0; start < segs.length && segs[start].isEmpty(); start ++ );
			for (i = start, len = segs.length; i < len;) {
				switch (segs[i]) {
				case ".":
					System.arraycopy(segs, i + 1, segs, i, len - i);
					break;
				case "..":
					System.arraycopy(segs, i + 1, segs, i - 1, len - i);
					i -- ;
					len -- ;
					break;
				default:
					i ++ ;
				}
			}
			return Arrays.asList(segs).subList(start, start + len);
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
			if ( !depend.equals(other.depend)) return false;
			return true;
		}
		
	}
	
}
