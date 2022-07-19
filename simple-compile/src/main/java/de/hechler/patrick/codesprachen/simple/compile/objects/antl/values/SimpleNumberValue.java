package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePrimitive;

public class SimpleNumberValue extends SimpleValueConst implements SimpleValue {
	
	public final int     bits;
	public final boolean signed;
	public final long    value;
	
	public SimpleNumberValue(int bits, long value) {
		this(bits, true, value);
	}
	
	public SimpleNumberValue(int bits, boolean signed, long value) {
		this(type(bits, signed), bits, signed, value);
	}
	
	public SimpleNumberValue(SimpleType t, long value) {
		this(t, bits(t), signed(t), value);
	}
	
	private SimpleNumberValue(SimpleType t, int bits, boolean signed, long value) {
		super(t);
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
	
	private static final int bits(SimpleType t) {
		if (t.isPrimitive()) {
			return ((SimpleTypePrimitive) t).bits();
		} else if (t.isPointer()) {
			return 64;
		} else if (t.isStruct()) {
			throw new IllegalStateException("struct type can not be represented by a number value");
		} else {
			throw new IllegalStateException("type is not primitive, no pointer and no struct! t: '" + t + "'");
		}
	}
	
	private static final boolean signed(SimpleType t) {
		if (t.isPrimitive()) {
			return ((SimpleTypePrimitive) t).signed();
		} else if (t.isPointer()) {
			return true;
		} else if (t.isStruct()) {
			throw new IllegalStateException("struct type can not be represented by a number value");
		} else {
			throw new IllegalStateException("type is not primitive, no pointer and no struct! t: '" + t + "'");
		}
	}
	
	private static final SimpleType type(int bits, boolean signed) {
		switch (bits) {
		case 64:
			if (signed) {
				return SimpleType.NUM;
			} else {
				return SimpleType.UNUM;
			}
		case 32:
			if (signed) {
				return SimpleType.DWORD;
			} else {
				return SimpleType.UDWORD;
			}
		case 16:
			if (signed) {
				return SimpleType.WORD;
			} else {
				return SimpleType.UWORD;
			}
		case 8:
			if (signed) {
				return SimpleType.BYTE;
			} else {
				return SimpleType.UBYTE;
			}
		default:
			throw new InternalError();
		}
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		return loadValue(getNumber(), targetRegister, blockedRegisters, commands, pos);
	}
	
	public static long loadValue(long value, int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) throws InternalError {
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
		Command addCmd = new Command(Commands.CMD_MOV, p1, p2);
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
