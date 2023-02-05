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

public class SimpleDirectVariableValue extends SimpleValueNoConst {
	
	public final SimpleVariable sv;
	
	public SimpleDirectVariableValue(SimpleVariable sv) {
		super(sv.type);
		this.sv = sv;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
		Param reg = blockRegister(targetRegister, blockedRegisters);
		if (sv instanceof SimpleFunctionVariable v) {
			if (loader != null) {
				long np = loader.loadVar(pos, targetRegister, commands, v);
				if (np != -1) {
					return np;
				}
			}
			if (t.isPrimitive() || t.isPointer()) {
				Param p;
				if (v.hasOffset()) {
					p = build(A_SR | B_NUM, v.reg(), v.offset());
				} else {
					p = build(A_SR, v.reg());
				}
				pos = addMovCmd(t, commands, pos, p, reg);
			} else if (t.isStruct() || t.isArray()) {
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
				throw new AssertionError(t.getClass() + " : " + t);
			}
		} else if (sv instanceof SimpleOffsetVariable v) { // offset is from file-start
			if (t.isPrimitive() || t.isPointer()) {
				pos = addMovCmd(t, commands, pos, lambdaPos -> build(A_SR | B_NUM, PrimAsmConstants.IP, v.offset() - lambdaPos), reg);
			} else if (t.isStruct() || t.isArray()) {
				Command leaCmd = new Command(Commands.CMD_LEA, reg, build(A_NUM, v.offset() - pos));
				pos += leaCmd.length();
				commands.add(leaCmd);
			} else {
				throw new AssertionError(t.getClass() + " : " + t);
			}
		} else {
			throw new AssertionError(sv.getClass() + " : " + sv);
		}
		return pos;
	}
	
	@Override
	public SimpleValue mkPointer(SimplePool pool) {
		wantPntr();
		return new SimpleVariablePointerValue();
	}
	
	private void wantPntr() {
		if (sv instanceof SimpleFunctionVariable v) {
			v.setWantsPointer();
		}
	}
	
	@Override
	public String toString() {
		return sv.name;
	}
	
	public class SimpleVariablePointerValue extends SimpleValueNoConst {
		
		public SimpleVariablePointerValue() {
			super(new SimpleTypePointer(SimpleDirectVariableValue.this.t));
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			Param   reg = blockRegister(targetRegister, blockedRegisters);
			Command cmd;
			if (sv instanceof SimpleFunctionVariable v) {
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
			} else if (sv instanceof SimpleOffsetVariable v) {
				cmd = new Command(Commands.CMD_LEA, reg, build(A_NUM, v.offset() - pos));
			} else {
				throw new AssertionError(sv.getClass() + " : " + sv);
			}
			pos += cmd.length();
			commands.add(cmd);
			return pos;
		}
		
		@Override
		public String toString() {
			return "&" + sv.name;
		}
		
	}
	
}
