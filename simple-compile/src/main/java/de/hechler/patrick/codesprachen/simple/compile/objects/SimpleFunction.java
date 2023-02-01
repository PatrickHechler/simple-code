package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleFuncPool;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunctionSymbol;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;

public class SimpleFunction extends SimpleFunctionSymbol implements SimpleExportable {
	
	public boolean                  addrVars;
	public int                      regVars = -1;
	public List<Command>            cmds    = null;
	public final boolean            main;
	public final SimpleCommandBlock body;
	public final SimpleFuncPool     pool;
	
	public SimpleFunction(boolean export, boolean main, String name, List<SimpleVariable> args,
			List<SimpleVariable> results, SimpleCommandBlock cmd, SimpleFuncPool pool) {
		this(-1L, export, main, name, new SimpleFuncType(args, results), cmd, pool);
	}
	
	public SimpleFunction(long address, String name, SimpleFuncType type) {
		this(address, true, false, name, type, null, null);
	}
	
	private SimpleFunction(long address, boolean export, boolean main, String name, SimpleFuncType type,
			SimpleCommandBlock cmd, SimpleFuncPool pool) {
		super(address, export, name, type);
		this.main = main;
		this.body = cmd;
		this.pool = pool;
	}
	
	@Override
	public boolean isExport() {
		return export;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public String toExportString() {
		if (!export) {
			throw new IllegalStateException("this is not marked as export!");
		}
		if (address == -1L) {
			throw new IllegalStateException("address is not initilized!");
		}
		StringBuilder b = new StringBuilder();
		b.append(FUNC);
		b.append(Long.toHexString(this.address).toUpperCase());
		b.append(FUNC);
		b.append(this.name);
		type.appendToExportStr(b);
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return type.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SimpleFunction other = (SimpleFunction) obj;
		return type.equals(other.type);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("func ");
		if (export) {
			b.append("exp ");
		}
		return b.append(name).append(type).append(';').toString();
	}
	
}
