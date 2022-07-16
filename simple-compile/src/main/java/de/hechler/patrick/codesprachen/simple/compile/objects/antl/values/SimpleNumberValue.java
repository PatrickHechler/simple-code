package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;

public class SimpleNumberValue extends SimpleValueConst implements SimpleValue {
	
	public final int     bits;
	public final boolean signed;
	public final long    value;
	
	public SimpleNumberValue(int bits, long value) {
		this(bits, true, value);
	}
	
	public SimpleNumberValue(int bits, boolean signed, long value) {
		this.bits = bits;
		this.signed = signed;
		switch (bits) {
		case 64:
			this.value = value;
			break;
		case 32:
			this.value = value & 0xFFFFFFFF;
			break;
		case 16:
			this.value = value & 0xFFFF;
			break;
		case 8:
			this.value = value & 0xFF;
			break;
		default:
			throw new InternalError("unknown bits value: " + bits);
		}
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		return loadValue(bits, value, targetRegister, blockedRegisters, commands, pos);
	}
	
	public static long loadValue(int bits, long value, int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) throws InternalError {
		assert !blockedRegisters[targetRegister];
		blockedRegisters[targetRegister] = true;
		Param p1, p2;
		ParamBuilder build = new ParamBuilder();
		build.art = ParamBuilder.A_SR;
		build.v1 = targetRegister;
		p1 = build.build();
		build.art = ParamBuilder.B_NUM;
		build.v2 = value;
		p2 = build.build();
		Commands cmd;
		switch (bits) {
		case 64:
			cmd = Commands.CMD_MOV;
			break;
		case 32:
			cmd = Commands.CMD_MVDW;
			break;
		case 16:
			cmd = Commands.CMD_MVW;
			break;
		case 8:
			cmd = Commands.CMD_MVB;
			break;
		default:
			throw new InternalError();
		}
		Command addCmd = new Command(cmd, p1, p2);
		commands.add(addCmd);
		return pos + addCmd.length();
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
		if ( !signed) {
			return value;
		}
		switch (bits) {
		case 64:
			return value;
		case 32:
			return (int) value;
		case 16:
			return (short) value;
		case 8:
			return (byte) value;
		default:
			throw new InternalError();
		}
	}
	
	@Override
	public int getNumberBits() {
		return bits;
	}
	
	@Override
	public double getFPNumber() {
		return getNumber();
	}
	
	@Override
	public String toString() {
		return Long.toString(value);
	}
	
}
