package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.HashSet;
import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleFuncType;

public class SimpleFunction implements SimpleExportable {
	
	public boolean                  addrVars;
	public int                      regVars = -1;
	public long                     address = -1L;
	public List <Command>           cmds    = null;
	public final boolean            export;
	public final boolean            main;
	public final String             name;
	public final SimpleFuncType     type;
	public final SimpleCommandBlock body;
	
	public SimpleFunction(boolean export, boolean main, String name, List <SimpleVariable> args,
		List <SimpleVariable> results, SimpleCommandBlock cmd) {
		this( -1L, export, main, name, args, results, cmd);
	}
	
	public SimpleFunction(long address, String name, List <SimpleVariable> args, List <SimpleVariable> results) {
		this(address, true, false, name, args, results, null);
	}
	
	public SimpleFunction(long address, boolean export, boolean main, String name, List <SimpleVariable> args,
		List <SimpleVariable> results, SimpleCommandBlock cmd) {
		this.address = address;
		this.export = export;
		this.main = main;
		this.name = name;
		this.type = new SimpleFuncType(args, results);
		this.body = cmd;
	}
	
	@Override
	public boolean isExport() {
		return export;
	}
	
	@Override
	public String toExportString() {
		if (address == -1L) {
			throw new IllegalStateException("address is not initilized!");
		}
		StringBuilder build = new StringBuilder();
		build.append(E_FUNC_START).append(Long.toHexString(address).toUpperCase()).append(E_NAME_START).append(name);
		type.appendToExportStr(build, new HashSet <>());
		return build.toString();
	}
	
	@Override
	public int hashCode() {
		return type.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleFunction other = (SimpleFunction) obj;
		if ( !type.equals(other.type))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("func ");
		b.append(name);
		b.append(type);
		return b.toString();
	}
	
}
