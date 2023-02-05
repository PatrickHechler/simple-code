package de.hechler.patrick.codesprachen.simple.compile.objects;

import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.SimpleCompiler;

public class UsedData implements Cloneable {
	
	public int  regs        = SimpleCompiler.MIN_VAR_REGISTER;
	public int  maxregs     = -1;
	public long currentaddr = 0L;
	public long maxaddr     = -1L;
	
	@Override
	public UsedData clone() {
		try {
			return (UsedData) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
}
