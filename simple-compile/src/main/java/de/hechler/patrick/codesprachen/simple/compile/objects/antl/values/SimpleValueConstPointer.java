package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public abstract class SimpleValueConstPointer extends SimpleValueConst {
	
	/**
	 * this array is not allowed to be modified
	 */
	public final byte[] data;
	public final int    elementBits;
	public long         addr = -1L;
	
	public SimpleValueConstPointer(SimpleType mytype, byte[] data, int elementSize) {
		super(mytype);
		this.data = data;
		this.elementBits = elementSize;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		assert !blockedRegisters[targetRegister];
		blockedRegisters[targetRegister] = true;
		assert this.addr != -1L;
		Param p1, p2;
		ParamBuilder build = new ParamBuilder();
		build.art = ParamBuilder.A_SR;
		build.v1 = targetRegister;
		p1 = build.build();
		build.art = ParamBuilder.A_NUM;
		build.v1 = addr;
		p2 = build.build();
		Command addCmd = new Command(Commands.CMD_LEA, p1, p2);
		pos += addCmd.length();
		commands.add(addCmd);
		return pos;
	}
	
	@Override
	protected boolean isDataPointer() {
		return true;
	}
	
	@Override
	public boolean implicitNumber() {
		return true;
	}
	
	@Override
	public boolean implicitFPNumber() {
		return true;
	}
	
	@Override
	public long getNumber() {
		if (addr == -1L) {
			throw new IllegalStateException("address not initilized!");
		}
		return addr;
	}
	
	@Override
	public int getNumberBits() {
		return 64;
	}
	
	@Override
	public double getFPNumber() {
		if (addr == -1L) {
			throw new IllegalStateException("address not initilized!");
		}
		return addr;
	}
	
	@Override
	protected SimpleValue arrayDeref(long val) {
		if (val < 0 || val > (long) data.length - (long) elementBits) {
			throw new IndexOutOfBoundsException();
		}
		long value = 0L;
		int off = (int) val;
		switch (elementBits) {
		case 64:
			value |= (data[off + 7] & 0xFF) << 56;
			value |= (data[off + 6] & 0xFF) << 48;
			value |= (data[off + 5] & 0xFF) << 40;
			value |= (data[off + 4] & 0xFF) << 32;
		case 32:
			value |= (data[off + 3] & 0xFF) << 24;
			value |= (data[off + 2] & 0xFF) << 16;
		case 16:
			value |= (data[off + 1] & 0xFF) << 8;
		case 8:
			value |= (data[off] & 0xFF);
			break;
		default:
			throw new InternalError("unknown elementBits value: " + elementBits);
		}
		return new SimpleNumberValue(elementBits, value);
	}
	
}
