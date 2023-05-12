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
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

@SuppressWarnings("javadoc")
public class SimpleVariableValue extends SimpleValueNoConst {
	
	public final SimpleVariable sv;
	
	public SimpleVariableValue(SimpleVariable sv) {
		super(sv.type);
		this.sv = sv;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
		Param reg = blockRegister(targetRegister, blockedRegisters);
		if (this.sv instanceof SimpleFunctionVariable v) {
			if (loader != null) {
				long np = loader.loadVar(pos, targetRegister, commands, v);
				if (np != -1) {
					return np;
				}
			}
			if (this.t.isPrimitive() || this.t.isPointer()) {
				Param p;
				if (v.hasOffset()) {
					p = build(A_SR | B_NUM, v.reg(), v.offset());
				} else {
					p = build(A_SR, v.reg());
				}
				pos = addMovCmd(this.t, commands, pos, p, reg);
			} else if (this.t.isStruct() || this.t.isArray()) {
				if (v.hasOffset()) {
					Command movCmd = new Command(Commands.CMD_MOV, reg, build(A_SR | B_NUM, v.reg(), v.offset()));
					pos += movCmd.length();
					commands.add(movCmd);
				} else {
					long    addr   = PrimAsmPreDefines.REGISTER_MEMORY_START + (v.reg() << 3);
					Command movCmd = new Command(Commands.CMD_MOV, reg, build(A_NUM, addr));
					pos += movCmd.length();
					commands.add(movCmd);
				}
			} else {
				throw new AssertionError(this.t.getClass() + " : " + this.t);
			}
		} else if (this.sv instanceof SimpleOffsetVariable v) { // offset is from file-start
			if (this.t.isPrimitive() || this.t.isPointer()) {
				pos = addMovCmd(this.t, commands, pos, lambdaPos -> build(A_SR | B_NUM, PrimAsmConstants.IP, v.offset() - lambdaPos), reg);
			} else if (this.t.isStruct() || this.t.isArray()) {
				Command leaCmd = new Command(Commands.CMD_LEA, reg, build(A_NUM, v.offset() - pos));
				pos += leaCmd.length();
				commands.add(leaCmd);
			} else {
				throw new AssertionError(this.t.getClass() + " : " + this.t);
			}
		} else {
			throw new AssertionError(this.sv.getClass() + " : " + this.sv);
		}
		return pos;
	}
	
	@Override
	public SimpleValue mkPointer(@SuppressWarnings("unused") SimplePool pool) {
		wantPntr();
		return new SimpleVariablePointerValue();
	}
	
	private void wantPntr() {
		if (this.sv instanceof SimpleFunctionVariable v) {
			v.setWantsPointer();
		}
	}
	
	@Override
	public String toString() {
		return this.sv.name;
	}
	
	public class SimpleVariablePointerValue extends SimpleValueNoConst {
		
		public SimpleVariablePointerValue() {
			super(new SimpleTypePointer(SimpleVariableValue.this.t));
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			Param   reg = blockRegister(targetRegister, blockedRegisters);
			Command cmd;
			if (SimpleVariableValue.this.sv instanceof SimpleFunctionVariable v) {
				if (loader != null) {
					long np = loader.loadVarPntr(pos, targetRegister, commands, v);
					if (np != -1L) {
						return np;
					}
				}
				if (v.hasOffset()) {
					cmd = new Command(Commands.CMD_MVAD, reg, build(A_SR, v.reg()), build(A_NUM, v.offset()));
				} else {
					cmd = new Command(Commands.CMD_MOV, reg, build(A_NUM, PrimAsmPreDefines.REGISTER_MEMORY_START + (v.reg() << 3)));
				}
			} else if (SimpleVariableValue.this.sv instanceof SimpleOffsetVariable v) {
				cmd = new Command(Commands.CMD_LEA, reg, build(A_NUM, v.offset() - pos));
			} else {
				throw new AssertionError(SimpleVariableValue.this.sv.getClass() + " : " + SimpleVariableValue.this.sv);
			}
			pos += cmd.length();
			commands.add(cmd);
			return pos;
		}
		
		@Override
		public String toString() {
			return "&" + SimpleVariableValue.this.sv.name;
		}
		
	}
	
}
