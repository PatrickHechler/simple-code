package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_REG;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;

import java.util.LinkedList;
import java.util.List;
import java.util.function.LongFunction;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunctionSymbol;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePrimitive;

public abstract class SimpleValueNoConst implements SimpleValue {
	
	public static final int FORWARD_JMP_BASE_LEN = 8;
	
	static {
		if (new Command(Commands.CMD_JMP, build(A_NUM, 0), null).length() != FORWARD_JMP_BASE_LEN) { throw new AssertionError(); }
		if (new Command(Commands.CMD_JMPERR, build(A_NUM, 0), null).length() != FORWARD_JMP_BASE_LEN) { throw new AssertionError(); }
		if (new Command(Commands.CMD_JMPNE, build(A_NUM, 0), Param.createLabel("--")).length() != FORWARD_JMP_BASE_LEN) {
			throw new AssertionError();
		}
	}
	
	public final SimpleType t;
	
	public SimpleValueNoConst(SimpleType type) {
		this.t = type;
	}
	
	@Override
	public boolean isConstant() { return false; }
	
	@Override
	public SimpleType type() {
		return this.t;
	}
	
	public static Param blockRegister(int targetRegister, boolean[] blockedRegisters) throws AssertionError {
		if (blockedRegisters[targetRegister]) { throw new AssertionError(targetRegister + " : " + tbs(blockedRegisters, '-', '#')); }
		blockedRegisters[targetRegister] = true;
		return build(A_SR, targetRegister);
	}
	
	private static String tbs(boolean[] blockedRegisters, char free, char used) {
		char[] chars = new char[blockedRegisters.length];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = blockedRegisters[i] ? used : free;
		}
		return new String(chars);
	}
	
	/**
	 * moves a primitive/pointer value<br>
	 * if needed this method makes sure the value will have a 64-bit representation of the value (if the value itself needs less than 64-bit)
	 * <p>
	 * note that this method will when <code>from == to</code> is <code>true</code> only make sure the value will have a 64-bit representation of the value (if the
	 * value itself needs less than 64-bit) and do nothing more.<br>
	 * pointers and other 64-bit values will then lead to nothing be done here, which will lead to a return value of {@code pos}
	 * 
	 * @param type     the type to move (only primitive types and pointers are supported)
	 * @param commands the list where the commands should be added
	 * @param pos      the initial position
	 * @param from     the parameter for the source
	 * @param to       the parameter for the target
	 * 
	 * @return the ne wposition
	 */
	public static long addMovCmd(SimpleType type, List<Command> commands, long pos, Param from, Param to) {
		return addMovCmd(type, commands, pos, (Object) from, (Object) to);
	}
	
	public static long addMovCmd(SimpleType type, List<Command> commands, long pos, LongFunction<Param> from, Param to) {
		return addMovCmd(type, commands, pos, (Object) from, (Object) to);
	}
	
	public static long addMovCmd(SimpleType type, List<Command> commands, long pos, Param from, LongFunction<Param> to) {
		return addMovCmd(type, commands, pos, (Object) from, (Object) to);
	}
	
	public static long addMovCmd(SimpleType type, List<Command> commands, long pos, LongFunction<Param> from, LongFunction<Param> to) {
		return addMovCmd(type, commands, pos, (Object) from, (Object) to);
	}
	
	private static long addMovCmd(SimpleType type, List<Command> commands, long pos, Object from, Object to) {
		if (type.isPrimitive()) {
			if (from != to) {
				Commands movOp  = switch ((int) type.byteCount()) {
								case 8 -> Commands.CMD_MOV;
								case 4 -> Commands.CMD_MVDW;
								case 2 -> Commands.CMD_MVW;
								case 1 -> Commands.CMD_MVB;
								default -> throw new AssertionError("illebal byte count: " + type.byteCount() + " : " + type);
								};
				Command  movCmd = new Command(movOp, get(pos, to), get(pos, from));
				pos += movCmd.length();
				commands.add(movCmd);
			}
			if (type.byteCount() != 8L) {
				long and = switch ((int) type.byteCount()) {
				case 4 -> 0x00000000FFFFFFFFL;
				case 2 -> 0x000000000000FFFFL;
				case 1 -> 0x00000000000000FFL;
				default -> throw new AssertionError(type.byteCount() + " : " + type);
				};
				if (((SimpleTypePrimitive) type).signed()) {
					Command cmplCmd = new Command(Commands.CMD_CMPL, get(pos, to), build(A_NUM, and));
					Command jmpNBCmd;
					Command orCmd   = new Command(Commands.CMD_OR, get(pos, to), build(A_NUM, ~and));
					Command jmpCmd;
					Command andCmd  = new Command(Commands.CMD_AND, get(pos, to), build(A_NUM, and));
					long    jmpArg  = FORWARD_JMP_BASE_LEN + andCmd.length();
					jmpCmd = new Command(Commands.CMD_JMP, build(A_NUM, jmpArg), null);
					long jmpNBArg = jmpArg + jmpCmd.length() + orCmd.length();
					jmpNBCmd  = new Command(Commands.CMD_JMP, build(A_NUM, jmpNBArg), null);
					pos      += cmplCmd.length();
					pos      += jmpNBCmd.length();
					pos      += orCmd.length();
					pos      += jmpCmd.length();
					pos      += andCmd.length();
					commands.add(cmplCmd);
					commands.add(jmpNBCmd);
					commands.add(orCmd);
					commands.add(jmpCmd);
					commands.add(andCmd);
				} else {
					Command andCmd = new Command(Commands.CMD_AND, get(pos, to), build(A_NUM, and));
					pos += andCmd.length();
					commands.add(andCmd);
				}
			}
		} else if (type.isPointer()) {
			if (from == to) { return pos; }
			Command cmd = new Command(Commands.CMD_MOV, get(pos, from), get(pos, to));
			pos += cmd.length();
			commands.add(cmd);
		} else {
			throw new AssertionError("a non primitive and non pointer value type is not supported by this method: " + type);
		}
		return pos;
	}
	
	private static Param get(long pos, Object param) {
		if (param instanceof Param p) {
			return p;
		} else if (param instanceof LongFunction<?> f) {
			return (Param) f.apply(pos);
		} else {
			throw new AssertionError();
		}
	}
	
	public abstract static class CalculatingUnValue extends SimpleValueNoConst {
		
		protected final SimpleValue val;
		
		public CalculatingUnValue(SimpleType type, SimpleValue val) {
			super(type);
			this.val = val;
		}
		
	}
	
	private abstract static class CalculatingBIValue extends SimpleValueNoConst {
		
		protected final SimpleValue valA;
		protected final SimpleValue valB;
		
		public CalculatingBIValue(SimpleType type, SimpleValue valA, SimpleValue valB) {
			super(type);
			this.valA = valA;
			this.valB = valB;
		}
		
	}
	
	private static class ArrayIndexValue extends CalculatingBIValue {
		
		public ArrayIndexValue(SimpleValue valA, SimpleValue valB) {
			super(((SimpleTypePointer) valA).target, valA, valB);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			pos = valA.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			long mul = this.t.byteCount();
			if (mul < 1L) { throw new AssertionError("target type is too small! valA: " + valA.type()); }
			if (valB instanceof SimpleValueConst c) {
				long val = c.getNumber() * mul;
				if (this.t.isPrimitive()) {
					Commands mov = getMovCmd((int) mul);
					if (val != 0L) {
						Command mv = new Command(mov, build(A_SR, targetRegister), build(A_SR | B_NUM, targetRegister, val));
						pos += mv.length();
						commands.add(mv);
					} else { // zero index can be optimized (no need to add zero)
						Command mv = new Command(mov, build(A_SR, targetRegister), build(A_SR | B_REG, targetRegister));
						pos += mv.length();
						commands.add(mv);
					}
				} else if (val != 0L) {
					Command add = new Command(Commands.CMD_ADD, build(A_SR, targetRegister), build(A_NUM, val));
					pos += add.length();
					commands.add(add);
				}
			} else {
				RegisterData rd = new RegisterData(fallbackRegister(targetRegister));
				pos = findRegister(blockedRegisters, commands, pos, rd, rd.reg, sul);
				pos = valB.loadValue(rd.reg, blockedRegisters, commands, pos, loader, sul);
				if (mul != 1L) { // byte arrays don't need this
					Command mulCmd = new Command(Commands.CMD_MUL, build(A_SR, rd.reg), build(A_NUM, mul));
					pos += mulCmd.length();
					commands.add(mulCmd);
				}
				if (this.t.isPrimitive()) {
					Commands mov    = getMovCmd((int) mul);
					Command  movCmd = new Command(mov, build(A_SR, targetRegister), build(A_SR | B_SR, targetRegister, rd.reg));
					pos += movCmd.length();
					commands.add(movCmd);
				} else {
					Command add = new Command(Commands.CMD_ADD, build(A_SR, targetRegister), build(A_SR, rd.reg));
					pos += add.length();
					commands.add(add);
				}
				pos = releaseRegister(commands, pos, rd, blockedRegisters, sul);
			}
			if (this.t.isPrimitive() && mul != 8) {
				long andVal = switch ((int) mul) {
				case 1 -> 0x00000000000000FFL;
				case 2 -> 0x000000000000FFFFL;
				case 4 -> 0x00000000FFFFFFFFL;
				default -> throw new AssertionError(mul);
				};
				if (((SimpleTypePrimitive) this.t).signed()) {
					long    test = switch ((int) mul) {
									case 1 -> 0x0000000000000080L;
									case 2 -> 0x0000000000008000L;
									case 4 -> 0x0000000080000000L;
									default -> throw new AssertionError(mul);
									};
					Command cmp  = new Command(Commands.CMD_CMPL, build(A_SR, targetRegister), build(A_NUM, test));
					Command jmpNB;
					Command or   = new Command(Commands.CMD_OR, build(A_SR, targetRegister), build(A_NUM, ~andVal));
					Command jmp;
					Command and  = new Command(Commands.CMD_AND, build(A_SR, targetRegister), build(A_NUM, andVal));
					jmp    = new Command(Commands.CMD_JMP, build(A_NUM, FORWARD_JMP_BASE_LEN + and.length()), null);
					jmpNB  = new Command(Commands.CMD_JMPNB, build(A_NUM, FORWARD_JMP_BASE_LEN + or.length() + jmp.length() + and.length()), null);
					pos   += cmp.length();
					commands.add(cmp);
					pos += jmpNB.length();
					commands.add(jmpNB);
					pos += or.length();
					commands.add(or);
					pos += jmp.length();
					commands.add(jmp);
					pos += and.length();
					commands.add(and);
				} else {
					Command cmd = new Command(Commands.CMD_AND, build(A_SR, targetRegister), build(A_NUM, andVal));
					pos += cmd.length();
					commands.add(cmd);
				}
			}
			return pos;
		}
		
		private static Commands getMovCmd(int byteCount) throws AssertionError {
			return switch (byteCount) {
			case 8 -> Commands.CMD_MOV;
			case 4 -> Commands.CMD_MVDW;
			case 2 -> Commands.CMD_MVW;
			case 1 -> Commands.CMD_MVB;
			default -> throw new AssertionError("illegal byte count: " + byteCount);
			};
		}
		
		@Override
		public String toString() {
			return "(" + valA + ")[" + valB + ']';
		}
		
	}
	
	private static class SimpleUnOperatorValue extends CalculatingUnValue {
		
		private final Commands op;
		
		public SimpleUnOperatorValue(SimpleType type, SimpleValue val, Commands op) {
			super(type, val);
			this.op = op;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			pos = this.val.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			Command cmd = new Command(this.op, build(A_SR, targetRegister), null);
			pos += cmd.length();
			commands.add(cmd);
			return pos;
		}
		
		@Override
		public String toString() {
			switch (this.op) {
			case CMD_INC:
				return "(" + this.val + ") + 1";
			case CMD_DEC:
				return "(" + this.val + ") - 1";
			case CMD_NOT:
				return "~(" + this.val + ")";
			case CMD_NEG, CMD_NEGFP:
				return "-(" + this.val + ')';
				//$CASES-OMITTED$
			default:
				return this.op + " (" + this.val + ')';
			}
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
			this.op   = op;
			this.swap = swap;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			RegisterData rd = new RegisterData(fallbackRegister(targetRegister));
			blockedRegisters[targetRegister] = true;
			pos                              = findRegister(blockedRegisters, commands, pos, rd, rd.reg, sul);
			blockedRegisters[targetRegister] = false;
			int reg0 = swap ? rd.reg : targetRegister;
			int reg1 = swap ? targetRegister : rd.reg;
			pos = valA.loadValue(reg0, blockedRegisters, commands, pos, loader, sul);
			pos = valB.loadValue(reg1, blockedRegisters, commands, pos, loader, sul);
			Command cmd = new Command(op, build(A_SR, reg0), build(A_SR, reg1));
			pos += cmd.length();
			commands.add(cmd);
			pos = releaseRegister(commands, pos, rd, blockedRegisters, sul);
			return pos;
		}
		
		@Override
		public String toString() {
			return op + " (" + valA + ") , (" + valB + ')';
		}
		
	}
	
	public static class CastedNoConstValue extends SimpleValueNoConst {
		
		protected final SimpleValue val;
		
		public CastedNoConstValue(SimpleType type, SimpleValue val) {
			super(type);
			this.val = val;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			return val.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
		}
		
		@Override
		public String toString() {
			return "(" + this.t + ") (" + val.toString() + ')';
		}
		
	}
	
	private static class NumberCastValue extends CastedNoConstValue {
		
		public NumberCastValue(SimpleType type, SimpleValue val) {
			super(type, val);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			int     oldByteCount;
			boolean oldSigned;
			boolean oldPntr;
			int     newByteCount;
			boolean newSigned;
			boolean newPntr;
			if (super.val.type().isPointerOrArray()) {
				oldByteCount = 8;
				oldSigned    = false;
				oldPntr      = true;
			} else {
				oldByteCount = (int) super.val.type().byteCount();
				oldSigned    = ((SimpleTypePrimitive) super.val.type()).signed();
				oldPntr      = false;
			}
			if (super.val.type().isPointerOrArray()) {
				newByteCount = 8;
				newSigned    = false;
				newPntr      = true;
			} else {
				newByteCount = (int) super.val.type().byteCount();
				newSigned    = ((SimpleTypePrimitive) super.val.type()).signed();
				newPntr      = false;
			}
			pos = super.val.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			if (newByteCount == oldByteCount && (oldSigned == newSigned || oldPntr || newPntr)) { return pos; }
			if (newByteCount > oldByteCount && (!oldSigned || newSigned)) { return pos; }
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
				jmp     = new Command(Commands.CMD_JMP, build(A_NUM, jmpLen), null);
				jmpLen += jmp.length() + or.length();
				jmpnb   = new Command(Commands.CMD_JMPNB, build(A_NUM, jmpLen), null);
				pos    += jmpLen + jmpnb.length();
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
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			pos = super.val.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			Commands c;
			if (this.t == SimpleType.FPNUM) {
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
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			pos = super.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			Param   reg = build(A_SR, targetRegister);
			Command cmp = new Command(Commands.CMD_CMP, reg, build(A_NUM, this.t.isPointerOrArray() ? 0L : -1L));
			pos += cmp.length();
			commands.add(cmp);
			Command jmpne;
			Command xor = this.t.isPointerOrArray() ? new Command(Commands.CMD_MOV, reg, build(A_NUM, -1L)) : new Command(Commands.CMD_XOR, reg, reg);
			jmpne  = new Command(Commands.CMD_JMPNE, build(A_NUM, FORWARD_JMP_BASE_LEN + xor.length()), null);
			pos   += jmpne.length() + xor.length();
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
				case pt_num, pt_unum -> switch (this) { // @formatter:off
					case SimpleValueConst c -> new SimpleNumberValue(t, c.getNumber() == -1L ? 0L : c.getNumber());
					default -> new PntrCastedValue(t, this);
				};// @formatter:on
				case pt_bool, pt_inval -> throw new InternalError(p.name());
				default -> throw new InternalError(p.name());
				};
			} else {
				throw cantCastExep(t);
			}
		} else if (this.t.isPrimitive()) {
			SimpleTypePrimitive p = (SimpleTypePrimitive) this.t;
			return switch (p) {
			case pt_fpnum -> {
				if (!t.isPrimitive()) { throw cantCastExep(t); }
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
			case pt_bool, pt_inval -> throw new InternalError(p.name());
			default -> throw new InternalError(p.name());
			};
		} else {
			throw new InternalError("unknown type: " + this.t.getClass() + " : " + this.t);
		}
	}
	
	private IllegalArgumentException cantCastExep(SimpleType t) {
		return cantCastExep(t, this.t, null);
	}
	
	private static IllegalArgumentException cantCastExep(SimpleType other, SimpleType t, String msg) {
		return new IllegalArgumentException("can not cast from " + other + " to " + t + (msg == null ? "" : ": " + msg));
	}
	
	@Override
	public SimpleValue addExpCond(SimplePool pool, SimpleValue val, SimpleValue val2) {
		SimpleType type = findType(val, val2);
		if (this instanceof SimpleValueConst c) {
			SimpleValue trueVal  = val.cast(type);
			SimpleValue falseVal = val2.cast(type);
			if (this.t.isPointer()) {
				if (c.getNumber() > 0L) {
					return trueVal;
				} else {
					return falseVal;
				}
			} else if (!this.t.isPrimitive()) {
				throw new IllegalStateException("only primitive values and pointers can be used as boolean values: " + this.t);
			} else if (this.t == SimpleType.FPNUM) {
				if (c.getFPNumber() != 0.0D) {
					return trueVal;
				} else {
					return falseVal;
				}
			} else {
				if (c.getNumber() != 0L) {
					return trueVal;
				} else {
					return falseVal;
				}
			}
		}
		if (this.t.isPointer()) {
			return new CompareValue(type, this, ZERO, Commands.CMD_CMP, Commands.CMD_JMPGT, val, val2);
		} else if (!this.t.isPrimitive()) {
			throw new IllegalStateException("only primitive values and pointers can be used as boolean values: " + this.t);
		} else if (this.t == SimpleType.FPNUM) {
			return new CompareValue(type, this, ZERO, Commands.CMD_CMPFP, Commands.CMD_JMPNE, val, val2);
		} else { // Unsigned and singed equal check are the same
			return new CompareValue(type, this, ZERO, Commands.CMD_CMP, Commands.CMD_JMPNE, val, val2);
		}
	}
	
	@Override
	public SimpleValue addExpLOr(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return new SimpleBiOperatorValue(type, cast(type), val.cast(type), Commands.CMD_OR);
		} else {
			throw cantCastExep(val.type(), type, "bitwise or");
		}
	}
	
	@Override
	public SimpleValue addExpLAnd(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return new SimpleBiOperatorValue(type, cast(type), val.cast(type), Commands.CMD_AND);
		} else {
			throw cantCastExep(val.type(), type, "bitwise and");
		}
	}
	
	@Override
	public SimpleValue addExpOr(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return cast(type).addExpOrORaddExpAnd(val.cast(type), true);
		} else {
			throw cantCastExep(val.type(), type, "logical or");
		}
	}
	
	@Override
	public SimpleValue addExpXor(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return new SimpleBiOperatorValue(type, cast(type), val.cast(type), Commands.CMD_XOR);
		} else {
			throw cantCastExep(val.type(), type, "bitiwse xor");
		}
	}
	
	@Override
	public SimpleValue addExpAnd(SimplePool pool, SimpleValue val) {
		SimpleType type = findType(this, val);
		if (type.isPointer() || type.isPrimitive() && type != SimpleType.FPNUM) {
			return cast(type).addExpOrORaddExpAnd(cast(type), false);
		} else {
			throw cantCastExep(val.type(), type, "logical and");
		}
	}
	
	// already casted
	private SimpleValue addExpOrORaddExpAnd(SimpleValue val, boolean or) {
		return new LogicalAndOROrValue(this.t, this, val, or);
	}
	
	private static final long JMP_LEN = FORWARD_JMP_BASE_LEN;
	
	static {
		if (JMP_LEN != new Command(Commands.CMD_JMPEQ, Param.createLabel("--"), null).length()) { throw new InternalError(); }
		if (JMP_LEN != new Command(Commands.CMD_JMPEQ, build(A_NUM, 0L), null).length()) { throw new InternalError(); }
		if (JMP_LEN != new Command(Commands.CMD_JMPNE, Param.createLabel("--"), null).length()) { throw new InternalError(); }
		if (JMP_LEN != new Command(Commands.CMD_JMPNE, build(A_NUM, 0L), null).length()) { throw new InternalError(); }
	}
	
	private static class LogicalAndOROrValue extends CalculatingBIValue {
		
		private final boolean doOr;
		
		public LogicalAndOROrValue(SimpleType type, SimpleValue valA, SimpleValue valB, boolean doOr) {
			super(type, valA, valB);
			this.doOr = doOr;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			Param reg     = build(A_SR, targetRegister);
			Param zero    = build(A_NUM, 0L);
			Param notZero = build(A_NUM, 1L);
			pos = valA.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			Command cmp = new Command(Commands.CMD_CMP, reg, zero);
			pos += cmp.length();
			commands.add(cmp);
			Commands cond     = doOr ? Commands.CMD_JMPNE : Commands.CMD_JMPEQ;
			Command  jmpc1;
			long     jmpc1Pos = pos;
			pos += JMP_LEN;
			List<Command> sub = newList();
			blockedRegisters[targetRegister] = false;
			pos                              = valB.loadValue(targetRegister, blockedRegisters, sub, pos, loader, sul);
			Command jmpc2;
			long    jmpc2Pos = pos;
			pos += JMP_LEN;
			Command movNoJmp = new Command(Commands.CMD_MOV, reg, doOr ? zero : notZero);
			pos += movNoJmp.length();
			long    jmpAfterMovPos = pos;
			Command jmpAfterMov;
			pos   += JMP_LEN;
			jmpc1  = new Command(cond, build(A_NUM, jmpc1Pos - pos), null);
			assert JMP_LEN == jmpc1.length();
			commands.add(jmpc1);
			commands.addAll(sub);
			jmpc2 = new Command(cond, build(A_NUM, jmpc2Pos - pos), null);
			assert JMP_LEN == jmpc2.length();
			commands.add(jmpc2);
			commands.add(movNoJmp);
			Command movAfterJmp = new Command(Commands.CMD_MOV, reg, doOr ? notZero : zero);
			pos         += movAfterJmp.length();
			jmpAfterMov  = new Command(Commands.CMD_JMP, build(A_NUM, jmpAfterMovPos - pos), null);
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
		
		private static final SimpleValue TRUE_VALUE  = new SimpleNumberValue(SimpleType.NUM, 1L);
		private static final SimpleValue FALSE_VALUE = new SimpleNumberValue(SimpleType.NUM, 0L);
		
		private final Commands    compare;
		private final Commands    jmpOnTrue;
		private final SimpleValue trueValue;
		private final SimpleValue falseValue;
		
		public CompareValue(SimpleType type, SimpleValue valA, SimpleValue valB, Commands cmp, Commands jmpOnTrue) {
			this(type, valA, valB, cmp, jmpOnTrue, TRUE_VALUE, FALSE_VALUE);
		}
		
		public CompareValue(SimpleType type, SimpleValue valA, SimpleValue valB, Commands cmp, Commands jmpOnTrue, SimpleValue trueValue, SimpleValue falseValue) {
			super(type, valA, valB);
			this.compare    = cmp;
			this.jmpOnTrue  = jmpOnTrue;
			this.trueValue  = trueValue;
			this.falseValue = falseValue;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
			Param targetReg = build(A_SR, targetRegister);
			pos = valA.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
			RegisterData rd = new RegisterData(fallbackRegister(targetRegister));
			pos = findRegister(blockedRegisters, commands, pos, rd, rd.reg, sul);
			pos = valB.loadValue(rd.reg, blockedRegisters, commands, pos, loader, sul);
			Command cmpCmd = new Command(compare, targetReg, build(A_SR, rd.reg));
			pos += cmpCmd.length();
			long    jmpTruePos = pos;
			Command jmpTrueCmd;
			pos += JMP_LEN;
			List<Command> loadFalse = newList();
			blockedRegisters[targetRegister] = false;
			pos                              = falseValue.loadValue(targetRegister, blockedRegisters, loadFalse, pos, loader, sul);
			long    jmpEndPos = pos;
			Command jmpEndCmd;
			pos        += JMP_LEN;
			jmpTrueCmd  = new Command(jmpOnTrue, build(A_NUM, jmpTruePos - pos), null);
			List<Command> loadTrue = newList();
			blockedRegisters[targetRegister] = false;
			pos                              = trueValue.loadValue(targetRegister, blockedRegisters, loadTrue, pos, loader, sul);
			jmpEndCmd                        = new Command(Commands.CMD_JMP, build(A_NUM, jmpEndPos - pos), null);
			commands.add(cmpCmd);
			commands.add(jmpTrueCmd);
			commands.addAll(loadFalse);
			commands.add(jmpEndCmd);
			commands.addAll(loadTrue);
			return pos;
		}
		
		@Override
		public String toString() {
			switch (jmpOnTrue) {
			case CMD_JMPEQ:
				return "(" + valA + ") == (" + valB + ')';
			case CMD_JMPNE:
				return "(" + valA + ") != (" + valB + ')';
			case CMD_JMPGE:
				return "(" + valA + ") >= (" + valB + ')';
			case CMD_JMPGT:
				return "(" + valA + ") > (" + valB + ')';
			case CMD_JMPLE:
				return "(" + valA + ") <= (" + valB + ')';
			case CMD_JMPLT:
				return "(" + valA + ") < (" + valB + ')';
			// $CASES-OMITTED$
			default:
				return "(" + valA + ") <" + compare + "  " + jmpOnTrue + "> (" + valB + ')';
			}
		}
		
	}
	
	private SimpleValue compare(SimpleValue val, boolean equal, int type, boolean useFirst) {
		if (this instanceof SimpleValueConst mc && val instanceof SimpleValueConst oc) {
			boolean res;
			if (this.t.isPointer()) {
				res = constNumCmp(equal, type, useFirst, mc, oc);
			} else if (this.t.isPrimitive()) {
				res = switch ((SimpleTypePrimitive) this.t) {
				case pt_fpnum -> constFPNumCmp(equal, type, useFirst, mc, oc);
				case pt_unum, pt_num, pt_dword, pt_byte, pt_ubyte, pt_udword, pt_uword, pt_word -> constNumCmp(equal, type, useFirst, mc, oc);
				case pt_bool, pt_inval -> throw new InternalError(((SimpleTypePrimitive) this.t).name());
				default -> throw new InternalError(((SimpleTypePrimitive) this.t).name());
				};
			} else {
				throw cantCastExep(val.type(), this.t, "compare");
			}
			if (res) {
				return ONE;
			} else {
				return ZERO;
			}
		}
		Commands cmp;
		if (this.t == SimpleType.FPNUM) {
			cmp = Commands.CMD_CMPFP;
		} else if (this.t.isPointer()) {
			cmp = Commands.CMD_CMP;
		} else if (!this.t.isPrimitive()) {
			throw new AssertionError("illegal compare type: " + this.t);
		} else if (((SimpleTypePrimitive) this.t).signed()) {
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
		return new CompareValue(this.t, this, val, cmp, jmpTrue);
	}
	
	private static boolean constNumCmp(boolean equal, int type, boolean useFirst, SimpleValueConst mc, SimpleValueConst oc) throws AssertionError {
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
	
	private static boolean constFPNumCmp(boolean equal, int type, boolean useFirst, SimpleValueConst mc, SimpleValueConst oc) throws AssertionError {
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
		SimpleType type = findType(this.t, val.type());
		return cast(type).compare(val.cast(type), equal, -1, true);
	}
	
	@Override
	public SimpleValue addExpRel(SimplePool pool, int type, SimpleValue val) {
		SimpleType stype = findType(this.t, val.type());
		return cast(stype).compare(val.cast(stype), false, type, false);
	}
	
	public SimpleValue shift(int type, SimpleValue val) {
		if (!this.t.isPrimitive()) { throw new IllegalArgumentException("illegal shift type: " + this.t); }
		switch ((SimpleTypePrimitive) this.t) {
		case pt_num, pt_unum, pt_dword, pt_udword, pt_word, pt_uword, pt_ubyte, pt_byte:
			break;
		case pt_fpnum, pt_inval, pt_bool:
			throw new IllegalArgumentException("illegal shift type: " + this.t);
		default:
			throw new AssertionError("unknown type: " + ((SimpleTypePrimitive) this.t).name());
		}
		Commands op = switch (type) {
		case EXP_SHIFT_LEFT -> Commands.CMD_LSH;
		case EXP_SHIFT_ARITMETIC_RIGTH -> Commands.CMD_RASH;
		case EXP_SHIFT_LOGIC_RIGTH -> Commands.CMD_RLSH;
		default -> throw new AssertionError("illegal type: " + type);
		};
		if (type == EXP_SHIFT_LOGIC_RIGTH && this.t.isPrimitive()) {
			SimpleTypePrimitive pt = (SimpleTypePrimitive) this.t;
			if (pt.bits() != 64 && pt.signed()) {
				val = val.cast(SimpleTypePrimitive.get(pt.bits(), false));
			}
		}
		return new SimpleBiOperatorValue(this.t, this, val, op);
		
	}
	
	@Override
	public SimpleValue addExpShift(SimplePool pool, int type, SimpleValue val) {
		SimpleType stype = findType(this.t, val.type());
		return cast(stype).shift(type, val.cast(stype));
	}
	
	private SimpleValue add(boolean add, SimpleValue val) {
		if (!this.t.isPrimitive() && !this.t.isPointer()) { throw new IllegalArgumentException("illegal add type: " + this.t); }
		if (!val.type().isPrimitive() && !val.type().isPointer()) { throw new IllegalArgumentException("illegal add type: " + val.type()); }
		if (this.t.isPointer()) {
			return addPntr(add, this, val);
		} else {
			if (val.type().isPointer()) {
				return addPntr(add, val, this);
			} else if (this.t == SimpleType.FPNUM || val.type() == SimpleType.FPNUM) {
				SimpleValue me   = cast(SimpleType.FPNUM);
				SimpleValue val0 = val.cast(SimpleType.FPNUM);
				if (me instanceof SimpleValueConst mc && val0 instanceof SimpleValueConst oc) {
					double a = mc.getFPNumber();
					double b = oc.getFPNumber();
					if (!Double.isNaN(a) && !Double.isNaN(b)) { return new SimpleFPNumberValue(a + b); } // else will cause a runtime error
				}
				return new SimpleBiOperatorValue(this.t, me, val0, Commands.CMD_ADDFP);
			} else {
				SimpleTypePrimitive mpt      = (SimpleTypePrimitive) this.t;
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
					return new SimpleBiOperatorValue(this.t, me, val0, Commands.CMD_ADD);
				} else {
					return new SimpleBiOperatorValue(this.t, me, val0, Commands.CMD_UADD);
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
			if (mul != 1L) { throw new IllegalArgumentException("pointer targets size is too low: " + mul + " : " + pntr.type()); }
			if (pntr instanceof SimpleValueConst mc && val0 instanceof SimpleValueConst oc) {
				return new SimpleNumberValue(pntr.type(), mc.getNumber() + oc.getNumber());
			} else {
				return new SimpleBiOperatorValue(pntr.type(), pntr, val0, addOp);
			}
		} else if (val0 instanceof SimpleValueConst c) {
			return new SimpleBiOperatorValue(pntr.type(), pntr, new SimpleNumberValue(num.type(), c.getNumber() * mul), addOp);
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
		if (val.type().isPointer()) { throw new IllegalArgumentException("can't add two pointers: " + other + " " + val.type()); }
		if (val.type() == SimpleType.FPNUM) { throw new IllegalArgumentException("can't add a pointers with a fpnum: " + other + " " + val.type()); }
	}
	
	@Override
	public SimpleValue addExpAdd(SimplePool pool, boolean add, SimpleValue val) {
		return add(add, val);
	}
	
	private SimpleValue mul(int type, SimpleValue val) {
		if (!this.t.isPrimitive()) { throw new IllegalArgumentException("can only multiply/divide primitive values: " + this.t + " " + val.type()); }
		if (this instanceof SimpleValueConst mc && val instanceof SimpleValueConst oc) {
			SimpleValue res = constMul(type, mc, oc);
			if (res != null) { return res; } // else will cause a run time error
		}
		Commands op = switch (type) {
		case EXP_MULTIPLY -> {
			if (this.t == SimpleType.FPNUM) {
				yield Commands.CMD_MULFP;
			} else if (((SimpleTypePrimitive) this.t).signed()) {
				yield Commands.CMD_MUL;
			} else {
				yield Commands.CMD_UMUL;
			}
		}
		case EXP_MODULO, EXP_DIVIDE -> {
			if (this.t == SimpleType.FPNUM) {
				if (type == EXP_MODULO) { throw new IllegalArgumentException("modulo is not supported for floating point values"); }
				yield Commands.CMD_DIVFP;
			} else if (((SimpleTypePrimitive) this.t).signed()) {
				yield Commands.CMD_DIV;
			} else {
				yield Commands.CMD_UDIV;
			}
		}
		default -> throw new InternalError("unknown mul type: " + type);
		};
		return new SimpleBiOperatorValue(this.t, this, val, op, type == EXP_MODULO);
	}
	
	private SimpleValue constMul(int type, SimpleValueConst mc, SimpleValueConst oc) throws InternalError {
		return switch (type) {
		case EXP_MULTIPLY -> {
			if (this.t == SimpleType.FPNUM) {
				double a = mc.getFPNumber();
				double b = oc.getFPNumber();
				if (!Double.isNaN(a) && !Double.isNaN(b)) {
					yield new SimpleFPNumberValue(a * b);
				} else {
					yield null;
				}
			} else {
				yield new SimpleNumberValue(this.t, mc.getNumber() * oc.getNumber());
			}
		}
		case EXP_MODULO -> {
			if (this.t == SimpleType.FPNUM) {
				throw new IllegalArgumentException("modulo is not supported for floating point values");
			} else if (((SimpleTypePrimitive) this.t).signed()) {
				yield new SimpleNumberValue(this.t, mc.getNumber() % oc.getNumber());
			} else {
				yield new SimpleNumberValue(this.t, Long.remainderUnsigned(mc.getNumber(), oc.getNumber()));
			}
		}
		case EXP_DIVIDE -> {
			if (this.t == SimpleType.FPNUM) {
				double a = mc.getFPNumber();
				double b = oc.getFPNumber();
				if (!Double.isNaN(a) && !Double.isNaN(b)) {
					yield new SimpleFPNumberValue(a / b);
				} else {
					yield null;
				}
			} else if (((SimpleTypePrimitive) this.t).signed()) {
				yield new SimpleNumberValue(this.t, mc.getNumber() / oc.getNumber());
			} else {
				yield new SimpleNumberValue(this.t, Long.divideUnsigned(mc.getNumber(), oc.getNumber()));
			}
		}
		default -> throw new InternalError("unknown mul type: " + type);
		};
	}
	
	@Override
	public SimpleValue addExpMul(SimplePool pool, int type, SimpleValue val) {
		SimpleType stype = findType(this.t, val.type());
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
			if (this.t.isPrimitive() && this.t != SimpleType.FPNUM) {
				yield new SimpleUnOperatorValue(this.t, this, Commands.CMD_NOT);
			} else {
				throw new IllegalStateException("bitwise not only possible on numbers (no fp nums)!");
			}
		}
		case EXP_UNARY_BOOLEAN_NOT -> {
			if (this.t.isPointer()) { // negative pointers and the zero pointer become false
				yield new CompareValue(this.t, this, new SimpleNumberValue(this.t, 0L), Commands.CMD_CMP, Commands.CMD_JMPLE);
			} else if (!this.t.isPrimitive()) {
				throw new IllegalStateException("logical (non bitwise) not only possible on numbers (and fp nums) and pointers!");
			} else if (this.t == SimpleType.FPNUM) {
				yield new CompareValue(this.t, this, new SimpleFPNumberValue(0.0D), Commands.CMD_CMPFP, Commands.CMD_JMPEQ);
			} else {
				// singed and bit count does not matter here
				yield new CompareValue(this.t, this, new SimpleNumberValue(this.t, 0L), Commands.CMD_CMP, Commands.CMD_JMPEQ);
			}
		}
		case EXP_UNARY_MINUS -> {
			if (!this.t.isPrimitive()) {
				throw new IllegalStateException("unary plus/minus only possible on numbers (and fp nums)!");
			} else if (this.t == SimpleType.FPNUM) {
				yield new SimpleUnOperatorValue(this.t, this, Commands.CMD_NEGFP);
			} else {
				yield new SimpleUnOperatorValue(this.t, this, Commands.CMD_NEG);
			}
		}
		case EXP_UNARY_PLUS -> {
			if (this.t.isPrimitive()) {
				yield this;
			} else {
				throw new IllegalStateException("unary plus/minus only possible on numbers (and fp nums)!");
			}
		}
		case EXP_UNARY_NONE -> throw new InternalError("unary none is no valid type!");
		default -> throw new InternalError("unknown unary type: " + type);
		};
	}
	
	@Override
	public SimpleValue mkPointer(SimplePool pool) {
		throw new IllegalStateException("can not make a pointer to this value (this: " + this + ")");
	}
	
	@Override
	public SimpleValue addExpDerefPointer(SimplePool pool) {
		if (!this.t.isPointer()) { throw new IllegalStateException("only a pointer can be dereferenced! (me: " + this + ")"); }
		SimpleType targetType = ((SimpleTypePointer) this.t).target;
		if (targetType == SimpleTypePrimitive.pt_inval) {
			throw new IllegalStateException("this pointer has no valid target type! (me: " + this + ")");
		}
		if (targetType.isStruct() || targetType.isArray()) { return new CastedNoConstValue(targetType, this); }
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(targetType) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos, loader, sul);
				addMovCmd(this.t, commands, pos, build(A_SR, targetRegister), build(A_SR | B_REG, targetRegister));
				return pos;
			}
			
			@Override
			public String toString() {
				return "(" + me + ")#";
			}
			
		};
	}
	
	@Override
	public SimpleValue addExpArrayRef(SimplePool pool, final SimpleValue val) {
		if (!this.t.isPointerOrArray()) { throw new IllegalStateException("only a pointer and array can be indexed! (me: " + this + ")[" + val + "]"); }
		if (!val.type().isPrimitive()) { throw cantCastExep(val.type(), SimpleType.NUM, null); }
		if (val.type() == SimpleType.FPNUM) { throw new IllegalStateException("an fp number is no valid index! (me: " + this + ")[" + val + "]"); }
		SimpleType target = ((SimpleTypePointer) this.t).target;
		if (target == SimpleTypePrimitive.pt_inval) {
			throw new IllegalStateException("this pointer/array has no valid target type! (me: " + this + ")[" + val + "]");
		}
		return new ArrayIndexValue(this, val.cast(SimpleType.NUM));
	}
	
	@Override
	public SimpleValue addExpNameRef(SimplePool pool, String text) {
		if (!this.t.isStruct()) { // also returns true for function structures and dependencies
			throw new IllegalStateException("name referencing is only possible on (function) structures and dependencies!");
		}
		SimpleOffsetVariable target = null;
		if (this.t.isFunc()) {
			SimpleFuncType func = (SimpleFuncType) this.t;
			target = func.member(text);
		} else if (this.t.isStruct()) {
			SimpleStructType struct = (SimpleStructType) this.t;
			target = struct.member(text);
		} else if (this instanceof SimpleDirectVariableValue vv && vv.sv instanceof SimpleDependency dep) {
			SimpleExportable exp = dep.get(text);
			if (exp instanceof SimpleConstant c) {
				return new SimpleNumberValue(SimpleType.NUM, c.value());
			} else if (exp instanceof SimpleFunctionSymbol f) {
				// TODO change when in future function pointers are supported
				throw new IllegalArgumentException(dep.name + ':' + f.name + " is a function address, no value!");
			} else if (exp instanceof SimpleStructType s) {
				throw new IllegalArgumentException(dep.name + ':' + s.name + " is a structure, no value!");
			} else if (exp instanceof SimpleVariable v) {
				return new SimpleDirectVariableValue(v);
			} else {
				throw new AssertionError("unknown export: " + exp.getClass() + " :  " + exp);
			}
		} else {
			throw new AssertionError("unknown variable use type: " + getClass() + "  " + toString());
		}
		if (target == null) {
			throw new IllegalStateException("this structure does not has a member with the name '" + text + "' ((func-)struct: " + this.t + ") (me: " + this + ")");
		}
		return new SimpleNonDirectVariableValue(this, target);
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
	 * note that if the given types are not compatible, this method is not allowed to return a type, which is only compatible to one of the two argument
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
			SimpleType other = st1 == SimpleType.FPNUM ? st2 : st1;
			if (!other.isPrimitive()) { throw cantCastExep(other, SimpleType.FPNUM, null); }
			return SimpleType.FPNUM;
		} else if (st1.isStruct() || st2.isStruct()) {
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
			SimpleType other = st1.isPointerOrArray() ? st2 : st1;
			if (!other.isPointerOrArray()) {
				if (!other.isPrimitive()) { throw new InternalError("unknown Simple types: " + other.getClass() + "  " + other); }
				if (!((SimpleTypePrimitive) other).signed()) { return SimpleType.UNUM; }
			}
			return SimpleType.NUM;
		} else if (st1.isPrimitive() && st2.isPrimitive()) {
			SimpleTypePrimitive pt1    = (SimpleTypePrimitive) st1;
			SimpleTypePrimitive pt2    = (SimpleTypePrimitive) st2;
			boolean             signed = pt1.signed() || pt2.signed();
			int                 bits   = Math.max(pt1.bits(), pt2.bits());
			return SimpleTypePrimitive.get(bits, signed);
		} else {
			throw new InternalError("unknown Simple types: " + st1.getClass() + " " + st2.getClass() + "  " + st1 + " " + st2);
		}
	}
	
	@SuppressWarnings("static-method")
	protected List<Command> newList() {
		return new LinkedList<>();
	}
	
	protected static class RegisterData {
		
		private boolean pushPop = false;
		private int     reg;
		
		public RegisterData(int reg) {
			this.reg = reg;
		}
		
		public int reg() {
			return reg;
		}
		
	}
	
	protected static int fallbackRegister(int register) {
		if (register < MIN_REGISTER) {
			return MIN_REGISTER;
		} else if (register + 1 <= MAX_REGISTER) {
			return register + 1;
		} else if (register - 1 < MIN_REGISTER) {
			throw new AssertionError("WHY IS THIS HAPPENING TO ME?!");
		} else {
			return register - 1;
		}
	}
	
	protected static int fallbackRegister(int... register) {
		for (int reg = MIN_REGISTER; reg <= MAX_REGISTER; reg++) {
			boolean found = false;
			for (int i = 0; i < register.length; i++) {
				if (register[i] == reg) {
					found = true;
					break;
				}
			}
			if (found) { return reg; }
		}
		throw new IllegalStateException("there is no register available!");
	}
	
	protected static long findRegister(boolean[] blockedRegisters, List<Command> commands, long pos, RegisterData rd, int fallback, StackUseListener sul) {
		int startRegister = rd.reg;
		for (; rd.reg < 256 && blockedRegisters[rd.reg]; rd.reg++);
		if (rd.reg >= 256) {
			for (rd.reg = MIN_REGISTER; blockedRegisters[rd.reg] && rd.reg < startRegister; rd.reg++);
			if (rd.reg >= startRegister) {
				if (sul != null) {
					sul.grow(8);
				}
				rd.pushPop                 = true;
				blockedRegisters[fallback] = false;
				rd.reg                     = fallback;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1  = rd.reg;
				Param   p1     = build.build();
				Command addCmd = new Command(Commands.CMD_PUSH, p1, null);
				commands.add(addCmd);
				pos += addCmd.length();
			}
		}
		return pos;
	}
	
	protected static long releaseRegister(List<Command> commands, long pos, RegisterData rd, boolean[] blockedRegisters, StackUseListener sul) {
		if (rd.pushPop) {
			if (sul != null) {
				sul.shrink(8);
			}
			blockedRegisters[rd.reg] = true;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			build.v1  = rd.reg;
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
