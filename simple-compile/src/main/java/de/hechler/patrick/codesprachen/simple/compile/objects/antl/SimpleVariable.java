package de.hechler.patrick.codesprachen.simple.compile.objects.antl;

import java.util.HashSet;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public class SimpleVariable implements SimpleExportable {
	
	/* @formatter:off
	 * addr == -1
	 * ?
	 *   reg == -1
	 *   ?
	 *     invalid
	 *   :
	 *     [reg + addr] is the value
	 *       (note that if this is a array or struct ret + addr is only the start address)
	 * :
	 *   reg == -1
	 *   ?
	 *     not yet initialized
	 *   :
	 *     reg holds the value
	 *       (note that if this is a array or struct only a pointer is in reg)
	 * @formatter:on
	 */
	public long addr = -1L;
	public int  reg  = -1;
	
	public final SimpleType type;
	public final String     name;
	
	public SimpleVariable(SimpleType type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public SimpleVariable(long addr, SimpleType type, String name) {
		this.addr = addr;
		this.type = type;
		this.name = name;
	}
	
	@Override
	public String toExportString() {
		StringBuilder build = new StringBuilder();
		build.append(Long.toHexString(addr)).append(E_NAME_START).append(name).append(E_VAR_START_TYPE);
		type.appendToExportStr(build, new HashSet <>());
		return build.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(type).append(' ').append(name);
		return b.toString();
	}
	
}
