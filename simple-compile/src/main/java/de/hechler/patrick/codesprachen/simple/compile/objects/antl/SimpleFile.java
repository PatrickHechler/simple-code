package de.hechler.patrick.codesprachen.simple.compile.objects.antl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValueConstPointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleVariableValue;

public class SimpleFile implements SimplePool {
	
	private final Path[]                         lockups;
	private final Charset                        cs;
	private final Map <String, SimpleDependency> dependencies = new HashMap <>();
	private final Map <String, SimpleVariable>   vars         = new HashMap <>();
	private final Map <String, SimpleStructType> structs      = new HashMap <>();
	private final Map <String, SimpleFunction>   funcs        = new HashMap <>();
	private final List <SimpleValueConstPointer> datas        = new ArrayList <>();
	private final List <SimpleExportable>        exports      = new ArrayList <>();
	private SimpleFunction                       main         = null;
	
	public SimpleFile(Path[] lockups, Charset cs) {
		this.lockups = lockups;
		this.cs = cs;
	}
	
	private void checkName(String name) {
		if (dependencies.containsKey(name)
			|| vars.containsKey(name)
			|| structs.containsKey(name)
			|| funcs.containsKey(name)) {
			throw new IllegalArgumentException("name already in use! name: '" + name + "'");
		}
	}
	
	public void addDependency(String name, String depend) {
		checkName(name);
		dependencies.put(name, new SimpleDependency(depend));
		int index = depend.lastIndexOf('.');
		if (index <= depend.lastIndexOf('/')) {
			throw new IllegalStateException("illegal dependency! (dependency: '" + depend + "')");
		}
		for (Path p : lockups) {
			Path resolved = p.resolve(depend);
			if ( !Files.exists(resolved)) {
				continue;
			}
			try {
				List <String> lines = Files.readAllLines(resolved, cs);
				Map <String, SimpleExportable> imps = readExports(lines);
				dependencies.put(depend, new SimpleDependency(name, depend, imps));
				return;
			} catch (IOException e) {
				throw new RuntimeException(e.toString(), e);
			}
		}
		throw new IllegalArgumentException("dependency could not be found! (dependnecy: '" + depend + "', lockups: " + Arrays.toString(lockups) + ")");
	}
	
	private Map <String, SimpleExportable> readExports(List <String> exps) {
		Map <String, SimpleExportable> imports = new HashMap <>();
		for (int i = 0; i < exps.size(); i ++ ) {
			SimpleExportable imp = SimpleExportable.fromExport(exps.get(i));
			if (imp instanceof SimpleFunction) {
				SimpleFunction sf = (SimpleFunction) imp;
				SimpleExportable old = imports.put(sf.name, sf);
				if (old != null) {
					throw new IllegalStateException("a export was doubled: old: " + old + " new: " + sf);
				}
			} else {
				throw new InternalError("the SimpleExportable which has been read has an unkown type: " + imp.getClass().getName() + " ('" + imp + "')");
			}
		}
		return imports;
	}
	
	public void addVariable(SimpleVariable vari, boolean export) {
		checkName(vari.name);
		vars.put(vari.name, vari);
		if (export) {
			this.exports.add(vari);
		}
	}
	
	public void addStructure(SimpleStructType struct) {
		SimpleStructType old = structs.put(struct.name, struct);
		if (old != null) {
			throw new IllegalStateException("structure already exist: name: " + struct.name);
		}
	}
	
	public void addFunction(SimpleFunction func) {
		checkName(func.name);
		SimpleFunction old = funcs.put(func.name, func);
		if (old != null) {
			throw new IllegalStateException("function already exist: name: " + func.name);
		}
		if (func.main) {
			if (this.main != null) {
				throw new IllegalStateException("there is already a main function!");
			}
			this.main = func;
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
	
	public Collection <SimpleValueConstPointer> dataValues() {
		return datas;
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
	
	@Override
	public void registerDataValue(SimpleValueConstPointer dataVal) {
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
		public SimplePool newSubPool(SimpleCommandBlock block) {
			return new SimpleSubPool(SimpleFuncPool.this, block);
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
		public void registerDataValue(SimpleValueConstPointer dataVal) {
			SimpleFile.this.registerDataValue(dataVal);
		}
		
	}
	
	public static class SimpleSubPool implements SimplePool {
		
		public final SimplePool         parent;
		public final SimpleCommandBlock block;
		
		public SimpleSubPool(SimplePool parent, SimpleCommandBlock block) {
			this.parent = parent;
			this.block = block;
		}
		
		@Override
		public SimpleStructType getStructure(String name) {
			return parent.getStructure(name);
		}
		
		@Override
		public SimplePool newSubPool(SimpleCommandBlock block) {
			return new SimpleSubPool(this, block);
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
		public void registerDataValue(SimpleValueConstPointer dataVal) {
			parent.registerDataValue(dataVal);
		}
		
	}
	
	@Override
	public SimplePool newSubPool(SimpleCommandBlock block) {
		throw new InternalError("this method should be only called on function pools not on the file pool!");
	}
	
	private static final SimpleType DEPENDENCY_TYPE = new SimpleStructType("--DEPENDENCY--", Collections.emptyList());
	
	private static class SimpleDependency extends SimpleVariable {
		
		private final String                         depend;
		private final Map <String, SimpleExportable> imps;
		
		public SimpleDependency(String depend) {
			super(DEPENDENCY_TYPE, "");
			this.depend = depend;
			this.imps = null;
		}
		
		public SimpleDependency(String name, String depend, Map <String, SimpleExportable> imps) {
			super(DEPENDENCY_TYPE, name);
			this.depend = depend;
			this.imps = imps;
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
