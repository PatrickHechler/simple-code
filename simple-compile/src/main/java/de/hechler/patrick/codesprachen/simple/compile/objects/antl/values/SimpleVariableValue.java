package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePointer;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.*;

public class SimpleVariableValue extends SimpleValueNoConst {
	
	public final SimpleVariable sv;
	
	public SimpleVariableValue(SimpleVariable sv) {
		super(sv.type);
		this.sv = sv;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		assert !blockedRegisters[targetRegister];
		blockedRegisters[targetRegister] = true;
		if (sv.reg == -1) {
			throw new InternalError("not yet initlized");
		}
		Param p1, p2;
		ParamBuilder build = new ParamBuilder();
		build.art = A_SR;
		build.v1 = targetRegister;
		p1 = build.build();
		build.v1 = sv.reg;
		Command addCmd;
		if (sv.addr == -1L) {
			p2 = build.build();
			// no need for complex move of small numbers, since this register is only for this number and thus everything should be correct
			addCmd = new Command(Commands.CMD_MOV, p1, p2);
		} else {
			if (sv.type.isArray() || sv.type.isStruct()) {
				Param p3;
				p2 = build.build();
				build.art = A_NUM;
				build.v1 = sv.addr;
				p3 = build.build();
				addCmd = new Command(Commands.CMD_MVAD, p1, p2, p3);
			} else {
				build.art = A_SR | B_NUM;
				build.v2 = sv.addr;
				p2 = build.build();
				addCmd = new Command(Commands.CMD_MOV, p1, p2);
				pos = addMovCmd(t, commands, pos, p1, p2, targetRegister);
			}
		}
		pos += addCmd.length();
		commands.add(addCmd);
		return pos;
	}
	
	@Override
	public SimpleValue mkPointer(SimplePool pool) {
		return new SimpleVariablePointerValue();
	}
	
	@Override
	public String toString() {
		return sv.name;
	}
	
	public class SimpleVariablePointerValue extends SimpleValueNoConst {
		
		public SimpleVariablePointerValue() {
			super(new SimpleTypePointer(SimpleVariableValue.this.t));
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			assert !blockedRegisters[targetRegister];
			blockedRegisters[targetRegister] = true;
			Param p1, p2, p3;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			build.v1 = targetRegister;
			p1 = build.build();
			build.v1 = sv.reg;
			p2 = build.build();
			build.art = A_NUM;
			build.v1 = sv.addr;
			p3 = build.build();
			Command addCmd = new Command(Commands.CMD_MVAD, p1, p2, p3);
			pos += addCmd.length();
			commands.add(addCmd);
			return pos;
		}
		
		@Override
		public String toString() {
			return "&" + sv.name;
		}
		
	}
	
}
