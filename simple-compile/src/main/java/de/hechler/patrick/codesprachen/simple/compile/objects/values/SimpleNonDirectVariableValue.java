package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

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
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
		RegisterData rd = new RegisterData(fallbackRegister(targetRegister));
		pos = findRegister(blockedRegisters, commands, pos, rd, rd.reg(), sul);
		pos = val.loadValue(rd.reg(), blockedRegisters, commands, pos, loader, sul);
		Param reg = blockRegister(targetRegister, blockedRegisters);
		if (t.isPrimitive() || t.isPointer()) {
			Param from = build(A_SR | B_NUM, rd.reg(), sv.offset());
			pos = addMovCmd(t, commands, pos, from, reg);
		} else if (t.isStruct() || t.isArray()) {
			Command movCmd = new Command(Commands.CMD_MVAD, reg, build(A_SR, rd.reg()), build(A_NUM, sv.offset()));
			pos += movCmd.length();
			commands.add(movCmd);
		} else {
			throw new AssertionError(t.getClass() + " : " + t);
		}
		pos = releaseRegister(commands, pos, rd, blockedRegisters, sul);
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
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			pos = valPntr.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			Command addCmd = new Command(Commands.CMD_ADD, build(A_SR, targetRegister), build(A_NUM, sv.offset()));
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
