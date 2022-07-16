package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public abstract class SimpleValueNoConst implements SimpleValue {
	
	@Override
	public boolean isConstant() {
		return false;
	}
	
	@Override
	public SimpleValue addExpCond(SimpleValue val, SimpleValue val2) {
		return new SimpleConditionalValue(this, val, val2);
	}
	
	@Override
	public SimpleValue addExpLOr(SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpLAnd(SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpOr(SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpXor(SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpAnd(SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpEq(boolean equal, SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpRel(int type, SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpShift(int type, SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpAdd(boolean add, SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpMul(int type, SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpCast(SimpleType t) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpUnary(int type) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpDerefPointer() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SimpleValue addExpArrayRef(SimpleValue val) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public abstract String toString();
	
	public static class SimpleConditionalValue extends SimpleValueNoConst {
		
		private static long JMP_LEN = 16L;
		static {
			Command jmpeqCmd = new Command(Commands.CMD_JMPEQ, Param.createLabel("label"), null);
			long jmpeqLen = jmpeqCmd.length();
			if (JMP_LEN != jmpeqLen) {
				throw new AssertionError();
			}
			Command jmpCmd = new Command(Commands.CMD_JMP, Param.createLabel("label"), null);
			long jmpLen = jmpCmd.length();
			if (JMP_LEN != jmpLen) {
				throw new AssertionError();
			}
		}
		
		private final SimpleValue condition;
		private final SimpleValue positive;
		private final SimpleValue negative;
		
		public SimpleConditionalValue(SimpleValue cond, SimpleValue pos, SimpleValue neg) {
			this.condition = cond;
			this.positive = pos;
			this.negative = neg;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			pos = condition.loadValue(targetRegister, blockedRegisters, commands, pos);
			Param p1, p2;
			ParamBuilder build = new ParamBuilder();
			build.art = ParamBuilder.A_SR;
			build.v1 = targetRegister;
			p1 = build.build();
			build.art = ParamBuilder.A_NUM;
			build.v1 = 0;
			p2 = build.build();
			Command addCmd = new Command(Commands.CMD_CMP, p1, p2);
			pos += addCmd.length();
			commands.add(addCmd);
			pos += JMP_LEN;
			List <Command> sub = new ArrayList <>();
			blockedRegisters[targetRegister] = false;
			long np = positive.loadValue(targetRegister, blockedRegisters, commands, pos);
			long relative = np + JMP_LEN - pos;
			pos = np + JMP_LEN;
			build.art = ParamBuilder.A_NUM;
			build.v1 = relative;
			p1 = build.build();
			commands.add(new Command(Commands.CMD_JMPEQ, p1, null));
			commands.addAll(sub);
			sub.clear();
			blockedRegisters[targetRegister] = false;
			pos = negative.loadValue(targetRegister, blockedRegisters, commands, pos);
			relative = pos - np;
			build.art = ParamBuilder.A_NUM;
			build.v1 = relative;
			p1 = build.build();
			commands.add(new Command(Commands.CMD_JMP, p1, null));
			commands.addAll(sub);
			return pos;
		}
		
		@Override
		public boolean isConstNoDataPointer() {
			return false;
		}
		
		@Override
		public boolean isConstDataPointer() {
			return false;
		}
		
		@Override
		public String toString() {
			return "(" + condition + ") ? (" + positive + ") : (" + negative + ")";
		}
		
	}
	
	public static class SimpleBiFunctionValue extends SimpleValueNoConst {
		
		private final Commands    cmd;
		private final SimpleValue p1;
		private final SimpleValue p2;
		
		public SimpleBiFunctionValue(Commands cmd, SimpleValue p1, SimpleValue p2) {
			this.cmd = cmd;
			this.p1 = p1;
			this.p2 = p2;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			p1.loadValue(targetRegister, blockedRegisters, commands, pos);
			Param p1, p2;
			ParamBuilder build = new ParamBuilder();
			RegisterData rd = new RegisterData(targetRegister + 1);
			pos = findRegister(blockedRegisters, commands, pos, build, rd, fallbackRegister(targetRegister));
			// TODO
			
			if (rd.pushPop) {
				build.art = ParamBuilder.A_SR;
				build.v1 = rd.reg;
				p1 = build.build();
				Command addCmd = new Command(Commands.CMD_POP, p1, null);
				commands.add(addCmd);
				pos += addCmd.length();
			}
			return pos;
		}
		
		@Override
		public boolean isConstNoDataPointer() {
			return false;
		}
		
		@Override
		public boolean isConstDataPointer() {
			return false;
		}
		
		@Override
		public String toString() {
			return cmd.toString() + " (" + p1 + "), (" + p2 + ")";
		}
		
	}
	
	public static class RegisterData {
		
		public boolean pushPop = false;
		public int     reg;
		
		public RegisterData(int reg) {
			this.reg = reg;
		}
		
	}
	
	private static int fallbackRegister(int register) {
		if (register + 1 >= 256) {
			return register - 1;
		} else {
			return register + 1;
		}
	}
	
	private static long findRegister(boolean[] blockedRegisters, List <Command> commands, long pos, ParamBuilder build, RegisterData rd, int fallback) {
		int startRegister = rd.reg;
		for (; blockedRegisters[rd.reg] && rd.reg < 256; rd.reg ++ );
		if (rd.reg >= 256) {
			for (rd.reg = MIN_REGISTER; blockedRegisters[rd.reg] && rd.reg < startRegister; rd.reg ++ );
			if (rd.reg >= startRegister) {
				rd.pushPop = true;
				rd.reg = fallback;
				build.art = ParamBuilder.A_SR;
				build.v1 = rd.reg;
				Param p1 = build.build();
				Command addCmd = new Command(Commands.CMD_PUSH, p1, null);
				commands.add(addCmd);
				pos += addCmd.length();
			}
		}
		return pos;
	}
	
}
