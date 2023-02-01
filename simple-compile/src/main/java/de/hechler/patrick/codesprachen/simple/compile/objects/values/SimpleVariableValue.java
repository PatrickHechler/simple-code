package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_REG;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

public class SimpleVariableValue extends SimpleValueNoConst {
	
	public final SimpleVariable sv;
	
	public SimpleVariableValue(SimpleVariable sv) {
		super(sv.type);
		this.sv = sv;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		assert !blockedRegisters[targetRegister] : targetRegister + " : " + tbs(blockedRegisters, '_', '#');
		blockedRegisters[targetRegister] = true;
		Param p1, p2;
		p1 = build(A_SR, targetRegister);
		assert sv.reg != -1;
		if (sv.addr == -1L) {
			p2 = build(A_SR, sv.reg);
		} else if (sv.addr == 0) {
			p2 = build(A_SR | B_REG, sv.reg);
		} else {
			p2 = build(A_SR | B_NUM, sv.reg, sv.addr);
		}
		return addMovCmd(t, commands, pos, p1, p2);
	}
	
	private static String tbs(boolean[] blockedRegisters, char f, char t) {
		char[] chars = new char[blockedRegisters.length];
		for (int i = 0; i < chars.length; i ++ ) {
			chars[i] = blockedRegisters[i] ? t : f;
		}
		return new String(chars);
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
			Param p1, p2;
			Command c;
			p1 = build(A_SR, targetRegister);
			if (sv.addr == -1L) {
				p2 = build(A_NUM, PrimAsmPreDefines.REGISTER_MEMORY_START + (sv.reg * 8));
				c = new Command(Commands.CMD_MOV, p1, p2);
			} else if (sv.addr == 0L) {
				p2 = build(A_SR, sv.reg);
				c = new Command(Commands.CMD_MOV, p1, p2);
			} else {
				Param p3;
				p2 = build(A_SR, sv.reg);
				p3 = build(A_NUM, sv.addr);
				c = new Command(Commands.CMD_MVAD, p1, p2, p3);
			}
			pos += c.length();
			commands.add(c);
			return pos;
		}
		
		@Override
		public String toString() {
			return "&" + sv.name;
		}
		
	}
	
}
