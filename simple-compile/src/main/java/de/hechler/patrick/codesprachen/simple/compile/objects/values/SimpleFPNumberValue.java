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

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

public class SimpleFPNumberValue extends SimpleValueConst {
	
	public final double value;
	
	public SimpleFPNumberValue(double value) {
		super(SimpleType.FPNUM);
		this.value = value;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos, VarLoader loader, StackUseListener sul) {
		return SimpleNumberValue.loadValue(Double.doubleToRawLongBits(value), targetRegister, blockedRegisters, commands, pos);
	}
	
	@Override
	public boolean implicitNumber() {
		return false;
	}
	
	@Override
	public boolean implicitFPNumber() {
		return true;
	}
	
	@Override
	public long getNumber() {
		throw new IllegalStateException();
	}
	
	@Override
	public int getNumberBits() {
		throw new IllegalStateException();
	}
	
	@Override
	public double getFPNumber() {
		return value;
	}
	
	@Override
	public String toString() {
		return Double.toString(value);
	}
	
}
