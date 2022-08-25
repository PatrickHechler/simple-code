package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.HashSet;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public class SimpleVariable implements SimpleExportable {
	
	/* @formatter:off
	 * addr == -1
	 * ?
	 *   reg == -1
	 *   ?
	 *     not yet initialized
	 *   :
	 *     [reg + addr] is the value
	 *       (note that if this is a array or struct ret + addr is only the start address)
	 * :
	 *   reg == -1
	 *   ?
	 *     invalid
	 *   :
	 *     reg holds the value
	 *       (note that if this is a array or struct only a pointer is in reg)
	 * @formatter:on
	 */
	public long addr = -1L;
	public int  reg  = -1;
	
	public final SimpleType type;
	public final String     name;
	public final boolean    export;
	
	public SimpleVariable(SimpleType type, String name, boolean export) {
		this.type = type;
		this.name = name;
		this.export = export;
	}
	
	public SimpleVariable(long addr, SimpleType type, String name) {
		this.addr = addr;
		this.type = type;
		this.export = true;
		this.name = name;
	}
	
	@Override
	public boolean isExport() {
		return export;
	}
	
	@Override
	public String toExportString() {
		StringBuilder b = new StringBuilder();
		b.append(E_VAR_START);
		b.append(Long.toHexString(addr).toUpperCase());
		b.append(E_NAME_START);
		b.append(name);
		b.append(E_VAR_START_TYPE);
		type.appendToExportStr(b, new HashSet <>());
		return b.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(type);
		b.append(' ');
		b.append(name);
		return b.toString();
	}
	
}
