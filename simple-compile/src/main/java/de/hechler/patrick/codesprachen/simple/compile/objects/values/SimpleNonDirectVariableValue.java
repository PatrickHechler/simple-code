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
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_XX;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build2;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

@SuppressWarnings("javadoc")
public class SimpleNonDirectVariableValue extends SimpleValueNoConst {
	
	public final SimpleValue          val;
	public final SimpleOffsetVariable sv;
	
	public SimpleNonDirectVariableValue(SimpleValue val, SimpleOffsetVariable sv) {
		super(sv.type);
		if (!val.type().isStruct()) { throw new AssertionError("value is no structure: " + val.getClass() + " : " + val); }
		this.val = val;
		this.sv  = sv;
	}
	
	@Override
	public long loadValue(SimpleFile sf, int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
		pos = this.val.loadValue(sf, targetRegister, blockedRegisters, commands, pos, loader, sul);
		if (this.t.isPrimitive() || this.t.isPointer()) {
			// move target, [target + offset]
			Param from = build2(A_XX | B_NUM, targetRegister, sv.offset());
			pos = addMovCmd(t, commands, pos, from, build2(A_XX, targetRegister));
		} else if (this.t.isStruct() || this.t.isArray()) {
			// ADD target, offset |> structure and array types are loaded as pointer to structure/array
			Param reg = build2(A_XX, targetRegister);
			Command movCmd = new Command(Commands.CMD_ADD, reg, build2(A_NUM, sv.offset()));
			pos += movCmd.length();
			commands.add(movCmd);
		} else {
			throw new AssertionError(t.getClass() + " : " + t);
		}
		return pos;
	}
	
	@Override
	public SimpleValue mkPointer(SimplePool pool) {
		SimpleValue pntr = val.mkPointer(pool);
		if (pntr instanceof SimpleValueConst c) {
			return new SimpleVariablePointerConstValue(c.getNumber() + sv.offset());
		} else {
			return new SimpleVariablePointerValue(pntr);
		}
	}
	
	@Override
	public String toString() {
		return "(" + val + "):" + sv.name;
	}
	
	public class SimpleVariablePointerConstValue extends SimpleNumberValue {
		
		public SimpleVariablePointerConstValue(long value) {
			super(new SimpleTypePointer(SimpleNonDirectVariableValue.this.t), value);
		}

		@Override
		public String toString() {
			return "&( (" + val + "):" + sv.name + ')';
		}
		
	}
	
	public class SimpleVariablePointerValue extends SimpleValueNoConst {
		
		private final SimpleValue valPntr;
		
		public SimpleVariablePointerValue(SimpleValue valPntr) {
			super(new SimpleTypePointer(SimpleNonDirectVariableValue.this.t));
			this.valPntr = valPntr;
		}
		
		@Override
		public long loadValue(SimpleFile sf, int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			pos = valPntr.loadValue(sf, targetRegister, blockedRegisters, commands, pos, loader, sul);
			Command addCmd = new Command(Commands.CMD_ADD, build2(A_XX, targetRegister), build2(A_NUM, sv.offset()));
			pos += addCmd.length();
			commands.add(addCmd);
			return pos;
		}
		
		@Override
		public String toString() {
			return "&( (" + val + "):" + sv.name + ')';
		}
		
	}
	
}
