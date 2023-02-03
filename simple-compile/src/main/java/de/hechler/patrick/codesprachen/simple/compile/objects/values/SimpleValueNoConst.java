package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.*;

import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePrimitive;

public abstract class SimpleValueNoConst implements SimpleValue {
	
	public static final int FORWARD_JMP_BASE_LEN = 8;
	
	static {
		if (new Command(Commands.CMD_JMP, build(A_NUM, 0), null).length() != FORWARD_JMP_BASE_LEN) {
			throw new AssertionError();
		}
		if (new Command(Commands.CMD_JMPERR, build(A_NUM, 0), null).length() != FORWARD_JMP_BASE_LEN) {
			throw new AssertionError();
		}
		if (new Command(Commands.CMD_JMPNE, build(A_NUM, 0), Param.createLabel("--"))
				.length() != FORWARD_JMP_BASE_LEN) {
			throw new AssertionError();
		}
	}
	
	public final SimpleType t;
	
	public SimpleValueNoConst(SimpleType type) {
		this.t = type;
	}
	
	@Override
	public boolean isConstant() {
		return false;
	}
	
	@Override
	public SimpleType type() {
		return t;
	}
	
	public abstract static class CalculatingUnValue extends SimpleValueNoConst {
		
		protected final SimpleValue val;
		
		public CalculatingUnValue(SimpleType type, SimpleValue val) {
			super(type);
			this.val = val;
		}
		
	}
	
	public abstract static class CalculatingBIValue extends SimpleValueNoConst {
		
		protected final SimpleValue valA;
		protected final SimpleValue valB;
		
		public CalculatingBIValue(SimpleType type, SimpleValue valA, SimpleValue valB) {
			super(type);
			this.valA = valA;
			this.valB = valB;
		}
		
	}
	
	private static class SimpleUnOperatorValue extends CalculatingUnValue {
		
		private final Commands op;
		
		public SimpleUnOperatorValue(SimpleType type, SimpleValue val, Commands op) {
			super(type, val);
			this.op = op;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			pos = val.loadValue(targetRegister, blockedRegisters, commands, pos);
			Command cmd = new Command(op, build(A_SR, targetRegister), null);
			pos += cmd.length();
			commands.add(cmd);
			return pos;
		}
		
		@Override
		public String toString() {
			return switch (op) {
			default -> op + " (" + val + ')';
			};
		}
		
	}
	
	private static class SimpleBiOperatorValue extends CalculatingBIValue {
		
		private final Commands op;
		private final boolean  swap;
		
		public SimpleBiOperatorValue(SimpleType type, SimpleValue valA, SimpleValue valB, Commands op) {
			this(type, valA, valB, op, false);
		}
		
		public SimpleBiOperatorValue(SimpleType type, SimpleValue valA, SimpleValue valB, Commands op, boolean swap) {
			super(type, valA, valB);
			this.op = op;
			this.swap = swap;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			RegisterData rd = new RegisterData(targetRegister + 1 >= 256 ? targetRegister - 1 : targetRegister + 1);
			blockedRegisters[targetRegister] = true;
			pos = findRegister(blockedRegisters, commands, pos, rd, rd.reg);
			blockedRegisters[targetRegister] = false;
			int reg0 = swap ? rd.reg : targetRegister;
			int reg1 = swap ? targetRegister : rd.reg;
			pos = valA.loadValue(reg0, blockedRegisters, commands, pos);
			pos = valB.loadValue(reg1, blockedRegisters, commands, pos);
			Command cmd = new Command(op, build(A_SR, reg0), build(A_SR, reg1));
			pos += cmd.length();
			commands.add(cmd);
			pos = releaseRegister(commands, pos, rd, blockedRegisters);
			return pos;
		}
		
		@Override
		public String toString() {
			return switch (op) {
			default -> op + " (" + valA + ") , (" + valB + ')';
			};
		}
		
	}
	
	public static class CastedNoConstValue extends SimpleValueNoConst {
		
		protected final SimpleValue val;
		
		public CastedNoConstValue(SimpleType type, SimpleValue val) {
			super(type);
			this.val = val;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			return val.loadValue(targetRegister, blockedRegisters, commands, pos);
		}
		
		@Override
		public String toString() {
			return "(" + t + ") (" + val.toString() + ')';
		}
		
	}
	
	private static class NumberCastValue extends CastedNoConstValue {
		
		public NumberCastValue(SimpleType type, SimpleValue val) {
			super(type, val);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			int     oldByteCount;
			boolean oldSigned;
			boolean oldPntr;
			int     newByteCount;
			boolean newSigned;
			boolean newPntr;
			if (super.val.type().isPointerOrArray()) {
				oldByteCount = 8;
				oldSigned = false;
				oldPntr = true;
			} else {
				oldByteCount = (int) super.val.type().byteCount();
				oldSigned = ((SimpleTypePrimitive) super.val.type()).signed();
				oldPntr = false;
			}
			if (super.val.type().isPointerOrArray()) {
				newByteCount = 8;
				newSigned = false;
				newPntr = true;
			} else {
				newByteCount = (int) super.val.type().byteCount();
				newSigned = ((SimpleTypePrimitive) super.val.type()).signed();
				newPntr = false;
			}
			pos = super.val.loadValue(targetRegister, blockedRegisters, commands, pos);
			if (newByteCount == oldByteCount && (oldSigned == newSigned || oldPntr || newPntr)) {
				return pos;
			}
			if (newByteCount > oldByteCount && (!oldSigned || newSigned)) {
				return pos;
			}
			int   minByteCount = Math.min(newByteCount, oldByteCount);
			long  newOr        = switch (minByteCount) {
								case 4 -> 0xFFFFFFFF00000000L;
								case 2 -> 0xFFFFFFFFFFFF0000L;
								case 1 -> 0xFFFFFFFFFFFFFF00L;
								default -> throw new InternalError("unknown primitive byte count: " + newByteCount);
								};
			Param myReg        = build(A_SR, targetRegister);
			if (newSigned) {
				long    newTest = 1L << ((minByteCount << 3) - 1);
				Command test    = new Command(Commands.CMD_CMPL, myReg, build(A_NUM, newTest));
				pos += test.length();
				commands.add(test);
				Command jmpnb;
				Command or     = new Command(Commands.CMD_OR, myReg, build(A_NUM, newOr));
				Command jmp;
				Command and    = new Command(Commands.CMD_AND, myReg, build(A_NUM, ~newOr));
				long    jmpLen = and.length();
				jmp = new Command(Commands.CMD_JMP, build(A_NUM, jmpLen), null);
				jmpLen += jmp.length() + or.length();
				jmpnb = new Command(Commands.CMD_JMPNB, build(A_NUM, jmpLen), null);
				pos += jmpLen + jmpnb.length();
				commands.add(jmpnb);
				commands.add(or);
				commands.add(jmp);
				commands.add(and);
			} else {
				Command and = new Command(Commands.CMD_AND, myReg, build(A_NUM, ~newOr));
				pos += and.length();
				commands.add(and);
			}
			return pos;
		}
		
	}
	
	private static final class FPNumberCastValue extends CastedNoConstValue {
		
		public FPNumberCastValue(SimpleType type, SimpleValue val) {
			super(type, val);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			pos = super.val.loadValue(targetRegister, blockedRegisters, commands, pos);
			Commands c;
			if (t == SimpleType.FPNUM) {
				assert super.val.type() != SimpleType.FPNUM;
				c = Commands.CMD_NTFP;
			} else {
				assert super.val.type() == SimpleType.FPNUM;
				c = Commands.CMD_FPTN;
			}
			Command cmd = new Command(c, build(A_SR, targetRegister), null);
			pos += cmd.length();
			commands.add(cmd);
			return pos;
		}
		
	}
	
	private static final class PntrCastedValue extends CastedNoConstValue {
		
		public PntrCastedValue(SimpleType type, SimpleValue val) {
			super(type, val);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			pos = super.loadValue(targetRegister, blockedRegisters, commands, pos);
			Param   reg = build(A_SR, targetRegister);
			Command cmp = new Command(Commands.CMD_CMP, reg, build(A_NUM, t.isPointerOrArray() ? 0L : -1L));
			pos += cmp.length();
			commands.add(cmp);
			Command jmpne;
			Command xor = t.isPointerOrArray() ? new Command(Commands.CMD_MOV, reg, build(A_NUM, -1L))
					: new Command(Commands.CMD_XOR, reg, reg);
			jmpne = new Command(Commands.CMD_JMPNE, build(A_NUM, FORWARD_JMP_BASE_LEN + xor.length()), null);
			pos += jmpne.length() + xor.length();
			commands.add(jmpne);
			commands.add(xor);
			return pos;
		}
		
	}
	
	@Override
	public SimpleValueNoConst cast(SimpleType t) {
		if (this.t == t) {
			return this;
		} else if (t.equals(this.t)) {
			return new CastedNoConstValue(t, this);
		} else if (this.t.isPointerOrArray()) {
			if (t.isPointerOrArray()) {
				return new CastedNoConstValue(t, this);
			} else if (t.isPrimitive()) {
				SimpleTypePrimitive p = (SimpleTypePrimitive) t;
				return switch (p) {
				case pt_fpnum -> throw cantCastExep(t);
				case pt_byte, pt_dword, pt_ubyte, pt_udword, pt_uword, pt_word -> cast(SimpleType.NUM).cast(t);
				case pt_bool, pt_num, pt_unum -> switch (this) { // @formatter:off
					case SimpleValueConst c -> new SimpleNumberValue(t, c.getNumber() == -1L ? 0L : c.getNumber());
					default -> new PntrCastedValue(t, this);
				};// @formatter:on
				case pt_inval, default -> throw new InternalError(p.name());
				};
			} else {
				throw cantCastExep(t);
			}
		} else if (this.t.isPrimitive()) {
			SimpleTypePrimitive p = (SimpleTypePrimitive) this.t;
			return switch (p) {
			case pt_fpnum -> {
				if (!t.isPrimitive()) {
					throw cantCastExep(t);
				}
				yield (switch (this) {
				case SimpleValueConst c -> new SimpleNumberValue(SimpleType.NUM, (long) c.getFPNumber());
				default -> new FPNumberCastValue(SimpleType.NUM, this);
				}).cast(t);
			}
			case pt_num, pt_unum, pt_dword, pt_udword, pt_word, pt_uword, pt_byte, pt_ubyte -> {
				if (t.isPointerOrArray()) {
					yield new CastedNoConstValue(t, this);
				} else if (!t.isPrimitive()) {
					throw cantCastExep(t);
				} else if (t == SimpleType.FPNUM) {
					SimpleValue val = switch (p.bits()) {
					case 64 -> this;
					default -> cast(SimpleType.NUM);
					};
					yield switch (val) {
					case SimpleValueConst c -> new SimpleFPNumberValue(c.getFPNumber());
					default -> new FPNumberCastValue(t, val);
					};
				} else {
					yield new NumberCastValue(t, this);
				}
			}
			case pt_inval, default -> throw new InternalError(p.name());
			};
		} else {
			throw new InternalError("unknown type: " + this.t.getClass() + " : " + this.t);
		}
	}
	
	private IllegalArgumentException cantCastExep(SimpleType t) {
		return new IllegalArgumentException("can not cast from " + this.t + " to " + t);
	}
	
	private IllegalArgumentException cantCastExep(SimpleValue other, SimpleType t, String msg) {
		return new IllegalArgumentException(
				"can not cast from " + this.t + "  " + other.type() + " to " + t + ": " + msg);
	}
	
	@Override
	public SimpleValue addExpCond(SimplePool pool, SimpleValue val, SimpleValue val2) {
		SimpleType type = findType(val, val2);
		return new SimpleConditionalValue(type, cast(SimpleType.BOOL), val.cast(type), val2.cast(type));
	}
	
	@Override
	public SimpleValue addExpLOr(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return new SimpleBiOperatorValue(type, cast(type), val.cast(type), Commands.CMD_OR);
		} else {
			throw cantCastExep(val, type, "bitwise or");
		}
	}
	
	@Override
	public SimpleValue addExpLAnd(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return new SimpleBiOperatorValue(type, cast(type), val.cast(type), Commands.CMD_AND);
		} else {
			throw cantCastExep(val, type, "bitwise and");
		}
	}
	
	@Override
	public SimpleValue addExpOr(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return cast(type).addExpOrORaddExpAnd(val.cast(type), true);
		} else {
			throw cantCastExep(val, type, "logical or");
		}
	}
	
	@Override
	public SimpleValue addExpXor(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return new SimpleBiOperatorValue(type, cast(type), val.cast(type), Commands.CMD_XOR);
		} else {
			throw cantCastExep(val, type, "bitiwse xor");
		}
	}
	
	@Override
	public SimpleValue addExpAnd(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return cast(type).addExpOrORaddExpAnd(cast(type), false);
		} else {
			throw cantCastExep(val, type, "logical and");
		}
	}
	
	// already casted
	private SimpleValue addExpOrORaddExpAnd(SimpleValue val, boolean or) {
		return new LogicalAndOROrValue(t, this, val, or);
	}
	
	private static final long JMP_LEN = FORWARD_JMP_BASE_LEN;
	
	static {
		if (JMP_LEN != new Command(Commands.CMD_JMPEQ, Param.createLabel("--"), null).length()) {
			throw new InternalError();
		}
		if (JMP_LEN != new Command(Commands.CMD_JMPEQ, build(A_NUM, 0L), null).length()) {
			throw new InternalError();
		}
		if (JMP_LEN != new Command(Commands.CMD_JMPNE, Param.createLabel("--"), null).length()) {
			throw new InternalError();
		}
		if (JMP_LEN != new Command(Commands.CMD_JMPNE, build(A_NUM, 0L), null).length()) {
			throw new InternalError();
		}
	}
	
	private static class LogicalAndOROrValue extends CalculatingBIValue {
		
		private final boolean doOr;
		
		public LogicalAndOROrValue(SimpleType type, SimpleValue valA, SimpleValue valB, boolean doOr) {
			super(type, valA, valB);
			this.doOr = doOr;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			Param reg     = build(A_SR, targetRegister);
			Param zero    = build(A_NUM, 0L);
			Param notZero = build(A_NUM, 1L);
			pos = valA.loadValue(targetRegister, blockedRegisters, commands, pos);
			Command cmp = new Command(Commands.CMD_CMP, reg, zero);
			pos += cmp.length();
			commands.add(cmp);
			Commands cond     = doOr ? Commands.CMD_JMPNE : Commands.CMD_JMPEQ;
			Command  jmpc1;
			long     jmpc1Pos = pos;
			pos += JMP_LEN;
			List<Command> sub = newList();
			blockedRegisters[targetRegister] = false;
			pos = valB.loadValue(targetRegister, blockedRegisters, sub, pos);
			Command jmpc2;
			long    jmpc2Pos = pos;
			pos += JMP_LEN;
			Command movNoJmp = new Command(Commands.CMD_MOV, reg, doOr ? zero : notZero);
			pos += movNoJmp.length();
			long    jmpAfterMovPos = pos;
			Command jmpAfterMov;
			pos += JMP_LEN;
			jmpc1 = new Command(cond, build(A_NUM, jmpc1Pos - pos), null);
			assert JMP_LEN == jmpc1.length();
			commands.add(jmpc1);
			commands.addAll(sub);
			jmpc2 = new Command(cond, build(A_NUM, jmpc2Pos - pos), null);
			assert JMP_LEN == jmpc2.length();
			commands.add(jmpc2);
			commands.add(movNoJmp);
			Command movAfterJmp = new Command(Commands.CMD_MOV, reg, doOr ? notZero : zero);
			pos += movAfterJmp.length();
			jmpAfterMov = new Command(Commands.CMD_JMP, build(A_NUM, jmpAfterMovPos - pos), null);
			assert JMP_LEN == jmpAfterMov.length();
			commands.add(jmpAfterMov);
			commands.add(movAfterJmp);
			return pos;
		}
		
		@Override
		public String toString() {
			return "(" + valA + ") " + (doOr ? "||" : "&&") + " (" + valB + ')';
		}
		
	}
	
	private static class CompareValue extends CalculatingBIValue {
		
		private final Commands compare;
		private final Commands jmpOnTrue;
		
		public CompareValue(SimpleType type, SimpleValue valA, SimpleValue valB, Commands cmp, Commands jmpOnTrue) {
			super(type, valA, valB);
			this.compare = cmp;
			this.jmpOnTrue = jmpOnTrue;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			Param targetReg = build(A_SR, targetRegister);
			pos = valA.loadValue(targetRegister, blockedRegisters, commands, pos);
			RegisterData rd = new RegisterData(targetRegister + 1);
			pos = findRegister(blockedRegisters, commands, pos, rd,
					targetRegister == 255 ? targetRegister - 1 : targetRegister + 1);
			pos = valB.loadValue(rd.reg, blockedRegisters, commands, pos);
			Command cmp = new Command(compare, targetReg, build(A_SR, rd.reg));
			pos += cmp.length();
			commands.add(cmp);
			Command jmpTrue;
			Command xor    = new Command(Commands.CMD_XOR, targetReg, targetReg);
			Command jmpFalse;
			Command movOne = new Command(Commands.CMD_MOV, targetReg, build(A_NUM, 1L));
			jmpFalse = new Command(Commands.CMD_JMP, build(A_NUM, FORWARD_JMP_BASE_LEN + movOne.length()), null);
			jmpTrue = new Command(jmpOnTrue, build(A_NUM, FORWARD_JMP_BASE_LEN + xor.length() + jmpFalse.length()),
					null);
			pos += jmpTrue.length();
			commands.add(jmpTrue);
			pos += xor.length();
			commands.add(xor);
			pos += jmpFalse.length();
			commands.add(jmpFalse);
			pos += movOne.length();
			commands.add(movOne);
			pos = releaseRegister(commands, pos, rd, blockedRegisters);
			return pos;
			
		}
		
		@Override
		public String toString() {
			return "(" + valA + ") " + switch (jmpOnTrue) {
			case CMD_JMPEQ -> "==";
			case CMD_JMPNE -> "!=";
			case CMD_JMPGE -> ">=";
			case CMD_JMPGT -> ">";
			case CMD_JMPLE -> "<=";
			case CMD_JMPLT -> "<";
			default -> "<" + compare + "  " + jmpOnTrue + ">";
			} + " (" + valB + ')';
		}
		
	}
	
	private SimpleValue compare(SimpleValue val, boolean equal, int type, boolean useFirst) {
		if (this instanceof SimpleValueConst mc && val instanceof SimpleValueConst oc) {
			boolean res;
			if (t.isPointer()) {
				res = constNumCmp(equal, type, useFirst, mc, oc);
			} else if (t.isPrimitive()) {
				res = switch ((SimpleTypePrimitive) t) {
				case pt_fpnum -> constFPNumCmp(equal, type, useFirst, mc, oc);
				case pt_unum, pt_num, pt_bool, pt_dword, pt_byte, pt_ubyte, pt_udword, pt_uword, pt_word ->
					constNumCmp(equal, type, useFirst, mc, oc);
				case pt_inval, default -> throw new InternalError(((SimpleTypePrimitive) t).name());
				};
			} else {
				throw cantCastExep(val, t, "compare");
			}
			return new SimpleNumberValue(SimpleType.BOOL, res ? 1L : 0L);
		}
		Commands cmp;
		if (t == SimpleType.FPNUM) {
			cmp = Commands.CMD_CMPFP;
		} else if (t.isPointer()) {
			cmp = Commands.CMD_CMP;
		} else if (!t.isPrimitive()) {
			throw new AssertionError("illegal compare type: " + t);
		} else if (((SimpleTypePrimitive) t).signed()) {
			cmp = Commands.CMD_CMP;
		} else {
			cmp = Commands.CMD_CMPU;
		}
		Commands jmpTrue;
		if (useFirst) {
			jmpTrue = equal ? Commands.CMD_JMPEQ : Commands.CMD_JMPNE;
		} else {
			jmpTrue = switch (type) {
			case EXP_GREATHER -> Commands.CMD_JMPGT;
			case EXP_GREATHER_EQUAL -> Commands.CMD_JMPGE;
			case EXP_SMALLER_EQUAL -> Commands.CMD_JMPLE;
			case EXP_SMALLER -> Commands.CMD_JMPLT;
			default -> throw new AssertionError("illegal compare type: " + type);
			};
		}
		return new CompareValue(t, this, val, cmp, jmpTrue);
	}
	
	private static boolean constNumCmp(boolean equal, int type, boolean useFirst, SimpleValueConst mc,
			SimpleValueConst oc) throws AssertionError {
		if (useFirst) {
			if (equal) {
				return mc.getNumber() == oc.getNumber();
			} else {
				return mc.getNumber() != oc.getNumber();
			}
		} else {
			return switch (type) {
			case EXP_GREATHER -> mc.getNumber() > oc.getNumber();
			case EXP_GREATHER_EQUAL -> mc.getNumber() >= oc.getNumber();
			case EXP_SMALLER_EQUAL -> mc.getNumber() <= oc.getNumber();
			case EXP_SMALLER -> mc.getNumber() < oc.getNumber();
			default -> throw new AssertionError("illegal compare type: " + type);
			};
		}
	}
	
	private static boolean constFPNumCmp(boolean equal, int type, boolean useFirst, SimpleValueConst mc,
			SimpleValueConst oc) throws AssertionError {
		if (useFirst) {
			if (equal) {
				return mc.getFPNumber() == oc.getFPNumber();
			} else {
				return mc.getFPNumber() != oc.getFPNumber();
			}
		} else {
			return switch (type) {
			case EXP_GREATHER -> mc.getFPNumber() > oc.getFPNumber();
			case EXP_GREATHER_EQUAL -> mc.getFPNumber() >= oc.getFPNumber();
			case EXP_SMALLER_EQUAL -> mc.getFPNumber() <= oc.getFPNumber();
			case EXP_SMALLER -> mc.getFPNumber() < oc.getFPNumber();
			default -> throw new AssertionError("illegal compare type: " + type);
			};
		}
	}
	
	@Override
	public SimpleValue addExpEq(SimplePool pool, boolean equal, SimpleValue val) {
		SimpleType type = findType(t, val.type());
		return cast(type).compare(val.cast(type), equal, -1, true);
	}
	
	@Override
	public SimpleValue addExpRel(SimplePool pool, int type, SimpleValue val) {
		SimpleType stype = findType(t, val.type());
		return cast(stype).compare(val.cast(stype), false, type, false);
	}
	
	public SimpleValue shift(int type, SimpleValue val) {
		if (!t.isPrimitive()) {
			throw new IllegalArgumentException("illegal shift type: " + t);
		}
		switch ((SimpleTypePrimitive) t) {
		case pt_num, pt_unum, pt_bool, pt_dword, pt_udword, pt_word, pt_uword, pt_ubyte, pt_byte:
			break;
		case pt_fpnum, pt_inval:
			throw new IllegalArgumentException("illegal shift type: " + t);
		default:
			throw new AssertionError("unknown type: " + ((SimpleTypePrimitive) t).name());
		}
		Commands op = switch (type) {
		case EXP_SHIFT_LEFT -> Commands.CMD_LSH;
		case EXP_SHIFT_ARITMETIC_RIGTH -> Commands.CMD_RASH;
		case EXP_SHIFT_LOGIC_RIGTH -> Commands.CMD_RLSH;
		default -> throw new AssertionError("illegal type: " + type);
		};
		return new SimpleBiOperatorValue(t, this, val, op);
		
	}
	
	@Override
	public SimpleValue addExpShift(SimplePool pool, int type, SimpleValue val) {
		SimpleType stype = findType(t, val.type());
		return cast(stype).shift(type, val.cast(stype));
	}
	
	private SimpleValue add(boolean add, SimpleValue val) {
		if (!t.isPrimitive() && !t.isPointer()) {
			throw new IllegalArgumentException("illegal add type: " + t);
		}
		if (!val.type().isPrimitive() && !val.type().isPointer()) {
			throw new IllegalArgumentException("illegal add type: " + val.type());
		}
		if (t.isPointer()) {
			return addPntr(add, this, val);
		} else {
			if (val.type().isPointer()) {
				return addPntr(add, val, this);
			} else if (t == SimpleType.FPNUM || val.type() == SimpleType.FPNUM) {
				SimpleValue me   = cast(SimpleType.FPNUM);
				SimpleValue val0 = val.cast(SimpleType.FPNUM);
				if (me instanceof SimpleValueConst mc && val0 instanceof SimpleValueConst oc) {
					double a = mc.getFPNumber();
					double b = oc.getFPNumber();
					if (!Double.isNaN(a) && !Double.isNaN(b)) {
						return new SimpleFPNumberValue(a + b);
					} // else will cause a runtime error
				}
				return new SimpleBiOperatorValue(t, me, val0, Commands.CMD_ADDFP);
			} else {
				SimpleTypePrimitive mpt      = (SimpleTypePrimitive) t;
				SimpleTypePrimitive opt      = (SimpleTypePrimitive) val.type();
				int                 bits     = Math.max(mpt.bits(), opt.bits());
				boolean             isSigned = mpt.signed() || opt.signed();
				SimpleType          type     = SimpleTypePrimitive.get(bits, isSigned);
				SimpleValue         me       = cast(type);
				SimpleValue         val0     = val.cast(type);
				if (me instanceof SimpleValueConst mc && val0 instanceof SimpleValueConst oc) {
					return new SimpleNumberValue(type, mc.getNumber() + oc.getNumber());
				}
				if (isSigned) {
					return new SimpleBiOperatorValue(t, me, val0, Commands.CMD_ADD);
				} else {
					return new SimpleBiOperatorValue(t, me, val0, Commands.CMD_UADD);
				}
			}
		}
	}
	
	private static SimpleValue addPntr(boolean add, SimpleValue pntr, SimpleValue num) {
		noPntrAndNoFP(num, pntr);
		boolean     otherSigned = ((SimpleTypePrimitive) num.type()).signed();
		SimpleValue val0        = otherSigned ? num.cast(SimpleType.NUM) : num.cast(SimpleType.UNUM);
		Commands    addOp       = addOp(add, otherSigned);
		long        mul         = ((SimpleTypePointer) pntr.type()).target.byteCount();
		if (mul <= 1L) {
			if (mul != 1L) {
				throw new IllegalArgumentException("pointer targets size is too low: " + mul + " : " + pntr.type());
			}
			if (pntr instanceof SimpleValueConst mc && val0 instanceof SimpleValueConst oc) {
				return new SimpleNumberValue(pntr.type(), mc.getNumber() + oc.getNumber());
			} else {
				return new SimpleBiOperatorValue(pntr.type(), pntr, val0, addOp);
			}
		} else if (val0 instanceof SimpleValueConst c) {
			return new SimpleBiOperatorValue(pntr.type(), pntr, new SimpleNumberValue(num.type(), c.getNumber() * mul),
					addOp);
		} else {
			SimpleValue mulVal = new SimpleNumberValue(SimpleType.UNUM, mul);
			SimpleValue val1;
			if (otherSigned) {
				val1 = new SimpleBiOperatorValue(SimpleType.NUM, val0, mulVal, Commands.CMD_MUL);
			} else {
				val1 = new SimpleBiOperatorValue(SimpleType.NUM, val0, mulVal, Commands.CMD_UMUL);
			}
			return new SimpleBiOperatorValue(pntr.type(), pntr, val1, addOp);
		}
	}
	
	private static Commands addOp(boolean add, boolean signed) {
		Commands addOp;
		if (add) {
			if (signed) {
				addOp = Commands.CMD_ADD;
			} else {
				addOp = Commands.CMD_UADD;
			}
		} else if (signed) {
			addOp = Commands.CMD_SUB;
		} else {
			addOp = Commands.CMD_USUB;
		}
		return addOp;
	}
	
	private static void noPntrAndNoFP(SimpleValue val, SimpleValue other) {
		if (val.type().isPointer()) {
			throw new IllegalArgumentException("can't add two pointers: " + other + " " + val.type());
		}
		if (val.type() == SimpleType.FPNUM) {
			throw new IllegalArgumentException("can't add a pointers with a fpnum: " + other + " " + val.type());
		}
	}
	
	@Override
	public SimpleValue addExpAdd(SimplePool pool, boolean add, SimpleValue val) {
		return add(add, val);
	}
	
	private SimpleValue mul(int type, SimpleValue val) {
		if (!t.isPrimitive()) {
			throw new IllegalArgumentException("can only multiply/divide primitive values: " + t + " " + val.type());
		}
		if (this instanceof SimpleValueConst mc && val instanceof SimpleValueConst oc) {
			SimpleValue res = constMul(type, mc, oc);
			if (res != null) {
				return res;
			} // else will cause a run time error
		}
		Commands op = switch (type) {
		case EXP_MULTIPLY -> {
			if (t == SimpleType.FPNUM) {
				yield Commands.CMD_MULFP;
			} else if (((SimpleTypePrimitive) t).signed()) {
				yield Commands.CMD_MUL;
			} else {
				yield Commands.CMD_UMUL;
			}
		}
		case EXP_MODULO, EXP_DIVIDE -> {
			if (t == SimpleType.FPNUM) {
				if (type == EXP_MODULO) {
					throw new IllegalArgumentException("modulo is not supported for floating point values");
				}
				yield Commands.CMD_DIVFP;
			} else if (((SimpleTypePrimitive) t).signed()) {
				yield Commands.CMD_DIV;
			} else {
				yield Commands.CMD_UDIV;
			}
		}
		default -> throw new InternalError("unknown mul type: " + type);
		};
		return new SimpleBiOperatorValue(t, this, val, op, type == EXP_MODULO);
	}

	private SimpleValue constMul(int type, SimpleValueConst mc, SimpleValueConst oc) throws InternalError {
		return switch (type) {
		case EXP_MULTIPLY -> {
			if (t == SimpleType.FPNUM) {
				double a = mc.getFPNumber();
				double b = oc.getFPNumber();
				if (!Double.isNaN(a) && !Double.isNaN(b)) {
					yield new SimpleFPNumberValue(a * b);
				} else {
					yield null;
				}
			} else {
				yield new SimpleNumberValue(t, mc.getNumber() * oc.getNumber());
			}
		}
		case EXP_MODULO -> {
			if (t == SimpleType.FPNUM) {
				throw new IllegalArgumentException("modulo is not supported for floating point values");
			} else if (((SimpleTypePrimitive) t).signed()) {
				yield new SimpleNumberValue(t, mc.getNumber() % oc.getNumber());
			} else {
				yield new SimpleNumberValue(t, Long.remainderUnsigned(mc.getNumber(), oc.getNumber()));
			}
		}
		case EXP_DIVIDE -> {
			if (t == SimpleType.FPNUM) {
				double a = mc.getFPNumber();
				double b = oc.getFPNumber();
				if (!Double.isNaN(a) && !Double.isNaN(b)) {
					yield new SimpleFPNumberValue(a / b);
				} else {
					yield null;
				}
			} else if (((SimpleTypePrimitive) t).signed()) {
				yield new SimpleNumberValue(t, mc.getNumber() / oc.getNumber());
			} else {
				yield new SimpleNumberValue(t, Long.divideUnsigned(mc.getNumber(), oc.getNumber()));
			}
		}
		default -> throw new InternalError("unknown mul type: " + type);
		};
	}
	
	@Override
	public SimpleValue addExpMul(SimplePool pool, int type, SimpleValue val) {
		SimpleType stype = findType(t, val.type());
		return cast(stype).mul(type, val.cast(stype));
	}
	
	@Override
	public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
		return cast(type);
	}
	
	@Override
	public SimpleValue addExpUnary(SimplePool pool, int type) {
		return switch (type) {
		case EXP_UNARY_AND -> mkPointer(pool);
		case EXP_UNARY_BITWISE_NOT -> {
			if (t.isPrimitive() && t != SimpleType.FPNUM) {
				yield new SimpleUnOperatorValue(t, this, Commands.CMD_NOT);
			} else {
				throw new IllegalStateException("bitwise not only possible on numbers (no fp nums)!");
			}
		}
		case EXP_UNARY_BOOLEAN_NOT -> {
			if (t.isPointer()) { // negative pointers and the zero pointer become false
				yield new CompareValue(t, this, new SimpleNumberValue(t, 0L), Commands.CMD_CMP, Commands.CMD_JMPLE);
			} else if (!t.isPrimitive()) {
				throw new IllegalStateException(
						"logical (non bitwise) not only possible on numbers (and fp nums) and pointers!");
			} else if (t == SimpleType.FPNUM) {
				yield new CompareValue(t, this, new SimpleFPNumberValue(0.0D), Commands.CMD_CMPFP, Commands.CMD_JMPEQ);
			} else {
				// singed and bit count does not matter here
				yield new CompareValue(t, this, new SimpleNumberValue(t, 0L), Commands.CMD_CMP, Commands.CMD_JMPEQ);
			}
		}
		case EXP_UNARY_MINUS -> {
			if (!t.isPrimitive()) {
				throw new IllegalStateException("unary plus/minus only possible on numbers (and fp nums)!");
			} else if (t == SimpleType.FPNUM) {
				yield new SimpleUnOperatorValue(t, this, Commands.CMD_NEGFP);
			} else {
				yield new SimpleUnOperatorValue(t, this, Commands.CMD_NEG);
			}
		}
		case EXP_UNARY_PLUS -> {
			if (t.isPrimitive()) {
				yield this;
			} else {
				throw new IllegalStateException("unary plus/minus only possible on numbers (and fp nums)!");
			}
		}
		case EXP_UNARY_NONE -> throw new InternalError("unary none is no valid type!");
		default -> throw new InternalError("unknown unary type: " + type);
		};
	}
	
	protected SimpleValue mkPointer(SimplePool pool) {
		throw new IllegalStateException("can not make a pointer to this value (this: " + this + ")");
	}
	
	// TODO continue here
	
	@Override
	public SimpleValue addExpDerefPointer(SimplePool pool) {
		if (!t.isPointer()) {
			throw new IllegalStateException("only a pointer can be dereferenced! (me: " + this + ")");
		}
		SimpleType targetType = ((SimpleTypePointer) t).target;
		if (targetType == SimpleTypePrimitive.pt_inval) {
			throw new IllegalStateException("this pointer has no valid target type! (me: " + this + ")");
		}
		if (targetType.isStruct() || targetType.isArray()) {
			return new CastedNoConstValue(targetType, this);
		}
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(targetType) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				Param        p1, p2;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_SR | B_REG;
				p2 = build.build();
				addMovCmd(t, commands, pos, p1, p2);
				return pos;
			}
			
			@Override
			public String toString() {
				return "(" + me + ")*";
			}
			
		};
	}
	
	@Override
	public SimpleValue addExpArrayRef(SimplePool pool, final SimpleValue val) {
		if (!t.isPointerOrArray()) {
			throw new IllegalStateException("only a pointer and array can be indexed! (me: " + this + ")[" + val + "]");
		}
		if (val.type().isStruct()) {
			throw new IllegalStateException("a structutre is no valid index! (me: " + this + ")[" + val + "]");
		}
		if (val.type().isArray()) {
			throw new IllegalStateException("an array is no valid index! (me: " + this + ")[" + val + "]");
		}
		if (val.type() == SimpleType.FPNUM) {
			throw new IllegalStateException("an fp number is no valid index! (me: " + this + ")[" + val + "]");
		}
		SimpleType target = ((SimpleTypePointer) t).target;
		if (target == SimpleTypePrimitive.pt_inval) {
			throw new IllegalStateException(
					"this pointer/array has no valid target type! (me: " + this + ")[" + val + "]");
		}
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(target) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				Param        p1, p2;
				ParamBuilder build = new ParamBuilder();
				RegisterData rd    = null;
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				if (val.isConstant()) {
					build.art = A_SR | B_NUM;
					build.v2 = ((SimpleValueConst) val).getNumber() * val.type().byteCount();
					p2 = build.build();
				} else {
					rd = new RegisterData(targetRegister + 1);
					findRegister(blockedRegisters, commands, pos, rd, fallbackRegister(targetRegister));
					val.loadValue(rd.reg, blockedRegisters, commands, pos);
					build.art = A_SR | B_SR;
					build.v2 = rd.reg;
					p2 = build.build();
				}
				pos = addMovCmd(t, commands, pos, p1, p2);
				if (rd != null) {
					releaseRegister(commands, pos, rd, blockedRegisters);
				}
				return pos;
			}
			
			@Override
			public String toString() {
				return "(" + me + ")[" + val + "]";
			}
			
		};
	}
	
	public static long addMovCmd(SimpleType type, List<Command> commands, long pos, Param param1, Param param2) {
		int      bits   = 64;
		boolean  signed = true;
		Commands mov    = Commands.CMD_MOV;
		Command  bcpCmd = null, jmpnbCmd = null, orCmd = null, jmpCmd = null, andCmd = null;
		// only cmpCmd and andCmd when not signed
		if (type.isPrimitive()) {
			bits = ((SimpleTypePrimitive) type).bits();
			if (bits != 64) {
				signed = ((SimpleTypePrimitive) type).signed();
				long andbits, cmpbits;
				switch (bits) {
				case 32:
					mov = Commands.CMD_MVDW;
					andbits = 0x00000000FFFFFFFFL;
					cmpbits = 0x0000000080000000L;
					break;
				case 16:
					mov = Commands.CMD_MVW;
					andbits = 0x000000000000FFFFL;
					cmpbits = 0x0000000000008000L;
					break;
				case 8:
					mov = Commands.CMD_MVB;
					andbits = 0x00000000000000FFL;
					cmpbits = 0x0000000000000080L;
					break;
				default:
					throw new InternalError("unknown bit count: " + bits);
				}
				Param p1, p2;
				// make commands in revers order, so the jump length is known
				p1 = param1;
				p2 = build(A_NUM, andbits);
				andCmd = new Command(Commands.CMD_AND, p1, p2);
				if (signed) {
					long relLen = andCmd.length();
					p1 = build(A_NUM, relLen);
					jmpCmd = new Command(Commands.CMD_JMP, p1, null);
					relLen += jmpCmd.length();
					p1 = param1;
					p2 = build(A_NUM, ~andbits);
					orCmd = new Command(Commands.CMD_OR, p1, p2);
					relLen += orCmd.length();
					p1 = build(A_NUM, relLen);
					jmpnbCmd = new Command(Commands.CMD_JMPNB, p1, null);
					p1 = param1;
					p2 = build(A_NUM, cmpbits);
					bcpCmd = new Command(Commands.CMD_CMPL, p1, p2);
				}
			}
		}
		Command c = new Command(mov, param1, param2);
		pos += c.length();
		commands.add(c);
		if (andCmd != null) {
			if (signed) {
				commands.add(bcpCmd);
				commands.add(jmpnbCmd);
				commands.add(orCmd);
				commands.add(jmpCmd);
				pos += bcpCmd.length() + jmpnbCmd.length() + orCmd.length() + jmpCmd.length();
			}
			commands.add(andCmd);
			pos += andCmd.length();
		}
		return pos;
	}
	
	@Override
	public SimpleValue addExpNameRef(SimplePool pool, String text) {
		if (!t.isStruct()) {
			throw new IllegalStateException("name referencing is only possible on files!");
		}
		SimpleVariable target = null;
		int            off    = 0;
		if (t.isFunc()) {
			SimpleFuncType func = (SimpleFuncType) t;
			for (SimpleVariable sv : func.arguments) {
				if (sv.name.equals(text)) {
					target = sv;
					break;
				}
				off += sv.type.byteCount();
			}
			if (target == null) {
				off = 0;
				for (SimpleVariable sv : func.arguments) {
					if (sv.name.equals(text)) {
						target = sv;
						break;
					}
					off += sv.type.byteCount();
				}
			}
		} else {
			SimpleStructType struct = (SimpleStructType) t;
			for (SimpleVariable sv : struct.members) {
				if (sv.name.equals(text)) {
					target = sv;
					break;
				}
				off += sv.type.byteCount();
			}
		}
		if (target == null) {
			throw new IllegalStateException("this structure does not has a member with the name '" + text
					+ "' ((func-)struct: " + t + ") (me: " + this + ")");
		}
		final int                offset = off;
		final SimpleValueNoConst me     = this;
		return new SimpleValueNoConst(target.type) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				Param        p1, p2;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				if (t.isStruct() || t.isArray()) {
					build.art = A_NUM;
					build.v2 = offset;
					p2 = build.build();
					Command addCmd = new Command(Commands.CMD_ADD, p1, p2);
					pos += addCmd.length();
					commands.add(addCmd);
				} else {
					build.art = A_SR | B_NUM;
					build.v2 = offset;
					p2 = build.build();
					addMovCmd(t, commands, pos, p1, p2);
				}
				return pos;
			}
			
			@Override
			public String toString() {
				return "(" + me + "):" + text;
			}
			
		};
	}
	
	@Override
	public abstract String toString();
	
	/**
	 * just invokes {@link #findType(SimpleType, SimpleType)} with the types of the two values
	 * 
	 * @param val  the first value
	 * @param val2 the second value
	 * 
	 * @return the type which is most compatible to both arguments
	 */
	private static SimpleType findType(SimpleValue val, SimpleValue val2) {
		return findType(val.type(), val2.type());
	}
	
	/**
	 * returns the type, which is at most compatible with both types. <br>
	 * note that if the given types are not compatible, this method is allowed to return a type, which is only
	 * compatible to one of the two argument
	 * 
	 * @param st1 the first type
	 * @param st2 the second type
	 * 
	 * @return the type which is most compatible to both arguments
	 */
	private static SimpleType findType(SimpleType st1, SimpleType st2) {
		if (st1.equals(st2)) {
			return st2;
		} else if (st1 == SimpleType.FPNUM || st2 == SimpleType.FPNUM) {
			return SimpleType.FPNUM;
		} else if (st1.isStruct() || st2.isStruct() || st1.isFunc() || st2.isFunc()) {
			throw new IllegalStateException("there is no fallback type for a (func-)structs with a diffrent type");
		} else if (st1.isPointerOrArray() && st2.isPointerOrArray()) {
			SimpleType t;
			try {
				t = findType(((SimpleTypePointer) st1).target, ((SimpleTypePointer) st2).target);
			} catch (IllegalStateException e) {
				t = SimpleTypePrimitive.pt_inval;
			}
			return new SimpleTypePointer(t);
		} else if (st1.isPointerOrArray() || st2.isPointerOrArray()) {
			return SimpleType.NUM;
		} else if (st1.isPrimitive() && st2.isPrimitive()) {
			assert st1 != SimpleTypePrimitive.pt_inval;
			assert st2 != SimpleTypePrimitive.pt_inval;
			SimpleTypePrimitive pt1    = (SimpleTypePrimitive) st1, pt2 = (SimpleTypePrimitive) st2;
			boolean             signed = pt1.signed() || pt2.signed();
			int                 bits   = Math.max(pt1.bits(), pt2.bits());
			return SimpleTypePrimitive.get(bits, signed);
		} else {
			throw new InternalError(
					"unknown Simple types: " + st1.getClass() + " " + st2.getClass() + "  " + st1 + " " + st2);
		}
	}
	
	public static class SimpleConditionalValue extends SimpleValueNoConst {
		
		static {
			Command jmpeqCmd = new Command(Commands.CMD_JMPEQ, Param.createLabel("label"), null);
			long    jmpeqLen = jmpeqCmd.length();
			if (JMP_LEN != jmpeqLen) {
				throw new AssertionError();
			}
			Command jmpCmd = new Command(Commands.CMD_JMP, Param.createLabel("label"), null);
			long    jmpLen = jmpCmd.length();
			if (JMP_LEN != jmpLen) {
				throw new AssertionError();
			}
		}
		
		private final SimpleValue condition;
		private final SimpleValue positive;
		private final SimpleValue negative;
		
		public SimpleConditionalValue(SimpleType type, SimpleValue cond, SimpleValue pos, SimpleValue neg) {
			super(type);
			this.condition = cond;
			this.positive = pos;
			this.negative = neg;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos) {
			pos = condition.loadValue(targetRegister, blockedRegisters, commands, pos);
			Param        p1, p2;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			build.v1 = targetRegister;
			p1 = build.build();
			build.art = A_NUM;
			build.v1 = 0;
			p2 = build.build();
			Command addCmd = new Command(Commands.CMD_CMP, p1, p2);
			pos += addCmd.length();
			commands.add(addCmd);
			pos += JMP_LEN;
			List<Command> sub = newList();
			blockedRegisters[targetRegister] = false;
			long np       = positive.loadValue(targetRegister, blockedRegisters, commands, pos);
			long relative = np + JMP_LEN - pos;
			pos = np + JMP_LEN;
			build.art = A_NUM;
			build.v1 = relative;
			p1 = build.build();
			commands.add(new Command(Commands.CMD_JMPEQ, p1, null));
			commands.addAll(sub);
			sub.clear();
			blockedRegisters[targetRegister] = false;
			pos = negative.loadValue(targetRegister, blockedRegisters, commands, pos);
			relative = pos - np;
			build.art = A_NUM;
			build.v1 = relative;
			p1 = build.build();
			commands.add(new Command(Commands.CMD_JMP, p1, null));
			commands.addAll(sub);
			return pos;
		}
		
		@Override
		public String toString() {
			return "(" + condition + ") ? (" + positive + ") : (" + negative + ")";
		}
		
	}
	
	@SuppressWarnings("static-method")
	protected List<Command> newList() {
		return new LinkedList<>();
	}
	
	private void checkAnyNumber(SimpleValue val) {
		if (!t.isPrimitive()) {
			throw new IllegalStateException("can't cast implicitly from " + t + " to an number");
		}
		if (!val.type().isPrimitive()) {
			throw new IllegalStateException("can't cast implicitly from " + t + " to an number");
		}
	}
	
	private void checkNumber(SimpleValue val, boolean allowFP) {
		checkAnyNumber(val);
		if (!allowFP) {
			if (t == SimpleType.FPNUM) {
				throw new IllegalStateException("can't cast implicitly from fpnum to no fp-number");
			}
			if (val.type() == SimpleType.FPNUM) {
				throw new IllegalStateException("can't cast implicitly from fpnum to no fp-number");
			}
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
		if (register < MIN_REGISTER) {
			return MIN_REGISTER;
		} else if (register + 1 < 256) {
			return register + 1;
		} else {
			return register - 1;
		}
	}
	
	private static long findRegister(boolean[] blockedRegisters, List<Command> commands, long pos, RegisterData rd,
			int fallback) {
		int startRegister = rd.reg;
		for (; rd.reg < 256 && blockedRegisters[rd.reg]; rd.reg++);
		if (rd.reg >= 256) {
			for (rd.reg = MIN_REGISTER; blockedRegisters[rd.reg] && rd.reg < startRegister; rd.reg++);
			if (rd.reg >= startRegister) {
				rd.pushPop = true;
				blockedRegisters[fallback] = false;
				rd.reg = fallback;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = rd.reg;
				Param   p1     = build.build();
				Command addCmd = new Command(Commands.CMD_PUSH, p1, null);
				commands.add(addCmd);
				pos += addCmd.length();
			}
		}
		return pos;
	}
	
	private static long releaseRegister(List<Command> commands, long pos, RegisterData rd, boolean[] blockedRegisters) {
		if (rd.pushPop) {
			blockedRegisters[rd.reg] = true;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			build.v1 = rd.reg;
			Param   p1     = build.build();
			Command addCmd = new Command(Commands.CMD_POP, p1, null);
			commands.add(addCmd);
			pos += addCmd.length();
		} else {
			blockedRegisters[rd.reg] = false;
		}
		return pos;
	}
	
}
