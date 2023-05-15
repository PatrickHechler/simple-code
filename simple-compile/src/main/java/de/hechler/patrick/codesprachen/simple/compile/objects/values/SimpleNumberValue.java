//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePrimitive;

public class SimpleNumberValue extends SimpleValueConst {
	
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
			this.value = value; // & 0xFFFFFFFFFFFFFFFFL; (not needed)
			break;
		case 32:
			this.value = value & 0xFFFFFFFFL;
			break;
		case 16:
			this.value = value & 0xFFFFL;
			break;
		case 8:
			this.value = value & 0xFFL;
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
	public long loadValue(SimpleFile sf, int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos, VarLoader loader, StackUseListener sul) {
		return loadValue(sf, getNumber(), targetRegister, blockedRegisters, commands, pos);
	}
	
	public static long loadValue(SimpleFile sf, long value, int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) throws InternalError {
		Param reg = SimpleValueNoConst.blockRegister(targetRegister, blockedRegisters);
		Command addCmd = new Command(Commands.CMD_MOV, reg, build(A_NUM, value));
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
