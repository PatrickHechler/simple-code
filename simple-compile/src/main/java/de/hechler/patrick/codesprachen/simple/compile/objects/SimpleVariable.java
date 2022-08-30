package de.hechler.patrick.codesprachen.simple.compile.objects;

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
		return this.export;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public String toExportString() {
		if ( !export) {
			throw new IllegalStateException("this is not marked as export!");
		}
		StringBuilder b = new StringBuilder();
		b.append(VAR);
		b.append(Long.toHexString(this.addr).toUpperCase());
		b.append(VAR);
		b.append(this.name);
		b.append(NAME_TYPE_SEP);
		type.appendToExportStr(b);
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (export ? 1231 : 1237);
		result = prime * result + ( (name == null) ? 0 : name.hashCode());
		result = prime * result + ( (type == null) ? 0 : type.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SimpleVariable other = (SimpleVariable) obj;
		if (export != other.export) return false;
		if (name == null) {
			if (other.name != null) return false;
		} else if ( !name.equals(other.name)) return false;
		if (type == null) {
			if (other.type != null) return false;
		} else if ( !type.equals(other.type)) return false;
		return true;
	}
	
}
