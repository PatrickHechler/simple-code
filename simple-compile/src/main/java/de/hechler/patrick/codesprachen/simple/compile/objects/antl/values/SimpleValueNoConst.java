package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_REG;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_SR;

import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePrimitive;

public abstract class SimpleValueNoConst implements SimpleValue {
	
	public final SimpleType t;
	
	public SimpleValueNoConst(SimpleType type) {
		this.t = type;
	}
	
	@Override
	public boolean isConstant() {
		return false;
	}
	
	@Override
	@Deprecated(forRemoval = true)
	public boolean isConstData() {
		return false;
	}
	
	@Override
	@Deprecated(forRemoval = true)
	public boolean isConstNoData() {
		return false;
	}
	
	@Override
	public SimpleType type() {
		return t;
	}
	
	@Override
	public SimpleValue addExpCond(SimplePool pool, SimpleValue val, SimpleValue val2) {
		return new SimpleConditionalValue(findType(val, val2), this, val, val2);
	}
	
	@Override
	public SimpleValue addExpLOr(SimplePool pool, SimpleValue val) {
		return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_OR, this, val);
	}
	
	@Override
	public SimpleValue addExpLAnd(SimplePool pool, SimpleValue val) {
		return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_AND, this, val);
	}
	
	@Override
	public SimpleValue addExpOr(SimplePool pool, SimpleValue val) {
		return addExpOr_addExpAnd(pool, val, false);
	}
	
	@Override
	public SimpleValue addExpXor(SimplePool pool, SimpleValue val) {
		checkNumber(val);
		return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_XOR, this, val);
	}
	
	@Override
	public SimpleValue addExpAnd(SimplePool pool, SimpleValue val) {
		return addExpOr_addExpAnd(pool, val, true);
	}
	
	private SimpleValue addExpOr_addExpAnd(SimplePool pool, SimpleValue val, boolean and) {
		checkNumber(val);
		if (t == SimpleType.FPNUM ^ val.type() == SimpleType.FPNUM) {
			if (t == SimpleType.FPNUM) {
				return addExpAnd(pool, val.addExpCast(pool, SimpleType.FPNUM));
			} else {
				return addExpCast(pool, SimpleType.FPNUM).addExpAnd(pool, val);
			}
		}
		SimpleType type =
			t == SimpleType.FPNUM ? SimpleType.FPNUM : (t.isPointer() || val.type().isPointer() || ((SimpleTypePrimitive) t).signed() || ((SimpleTypePrimitive) val.type()).signed()) ? SimpleType.NUM : SimpleType.UNUM;
		return createBoolAndOrBoolOrValue(val, type, and);
	}
	
	private SimpleValue createBoolAndOrBoolOrValue(SimpleValue val, SimpleType type, boolean and) {
		final SimpleValueNoConst me = this;
		Commands jmpcond = and ? Commands.CMD_JMPEQ : Commands.CMD_JMPNE;
		return new SimpleValueNoConst(type) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				RegisterData rd = new RegisterData(targetRegister + 1);
				findRegister(blockedRegisters, commands, pos, rd, fallbackRegister(targetRegister));
				pos = val.loadValue(rd.reg, blockedRegisters, commands, pos);
				Command cmp1Cmd, jmpcond1Cmd, cmp2Cmd, jmpcond2Cmd, movACmd, jmpCmd, movBCmd;
				Param p1, p2;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				if (and) {
					build.v1 = 0L;
				} else {
					build.v1 = 1L;
				}
				p2 = build.build();
				movBCmd = new Command(Commands.CMD_MOV, p1, p2);
				long relative = movBCmd.length();
				if (and) {
					build.v1 = 1L;
				} else {
					build.v1 = 0L;
				}
				p2 = build.build();
				movACmd = new Command(Commands.CMD_MOV, p1, p2);
				build.v1 = relative;
				p1 = build.build();
				jmpCmd = new Command(Commands.CMD_JMP, p1, null);
				relative += jmpCmd.length() + movACmd.length();
				build.v1 = relative;
				p1 = build.build();
				jmpcond2Cmd = new Command(jmpcond, p1, null);
				relative += jmpcond2Cmd.length();
				build.v1 = 0;
				p2 = build.build();
				build.art = A_SR;
				build.v1 = rd.reg;
				p1 = build.build();
				cmp2Cmd = new Command(Commands.CMD_CMP, p1, p2);
				relative += cmp2Cmd.length();
				build.v1 = targetRegister;
				p1 = build.build();
				cmp1Cmd = new Command(Commands.CMD_CMP, p1, p2);
				build.art = A_NUM;
				build.v1 = relative;
				p1 = build.build();
				jmpcond1Cmd = new Command(jmpcond, p1, null);
				pos += cmp1Cmd.length() + jmpcond1Cmd.length() + relative;
				commands.add(cmp1Cmd);
				commands.add(jmpcond1Cmd);
				commands.add(cmp2Cmd);
				commands.add(jmpcond2Cmd);
				commands.add(movACmd);
				commands.add(jmpCmd);
				commands.add(movBCmd);
				releaseRegister(commands, pos, rd, blockedRegisters);
				return pos;
			}
			
			@Override
			public String toString() {
				return "(" + me + " && " + val + ")";
			}
			
		};
	}
	
	@Override
	public SimpleValue addExpEq(SimplePool pool, boolean equal, SimpleValue val) {
		if (t.isStruct() || val.type().isStruct()) {
			throw new IllegalStateException("structures can not be compared!");
		}
		if (t.isArray() || val.type().isArray()) {
			throw new IllegalStateException("arrays can not be compared!");
		}
		if (t == SimpleType.FPNUM ^ val.type() == SimpleType.FPNUM) {
			if (t == SimpleType.FPNUM) {
				return addExpEq(pool, equal, val.addExpCast(pool, SimpleType.FPNUM));
			} else {
				return addExpCast(pool, SimpleType.FPNUM).addExpEq(pool, equal, val);
			}
		}
		return createCompareValue(equal ? Commands.CMD_JMPEQ : Commands.CMD_JMPNE, val);
	}
	
	@Override
	public SimpleValue addExpRel(SimplePool pool, int type, SimpleValue val) {
		if (t.isStruct() || val.type().isStruct()) {
			throw new IllegalStateException("structures can not be compared!");
		}
		if (t.isArray() || val.type().isArray()) {
			throw new IllegalStateException("arrays can not be compared!");
		}
		if (t == SimpleType.FPNUM ^ val.type() == SimpleType.FPNUM) {
			if (t == SimpleType.FPNUM) {
				return addExpCast(pool, SimpleType.FPNUM).addExpRel(pool, type, val);
			} else {
				return addExpRel(pool, type, val.addExpCast(pool, SimpleType.FPNUM));
			}
		}
		switch (type) {
		case EXP_GREATHER:
			return createCompareValue(Commands.CMD_JMPGT, val);
		case EXP_GREATHER_EQUAL:
			return createCompareValue(Commands.CMD_JMPGE, val);
		case EXP_SMALLER_EQUAL:
			return createCompareValue(Commands.CMD_JMPLE, val);
		case EXP_SMALLER:
			return createCompareValue(Commands.CMD_JMPLT, val);
		default:
			throw new InternalError("unknown relative compare type: " + type);
		}
	}
	
	private SimpleValueNoConst createCompareValue(Commands jmp, SimpleValue val) {
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(SimpleType.NUM) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				me.loadValue(targetRegister, blockedRegisters, commands, pos);
				RegisterData rd = new RegisterData(targetRegister + 1);
				pos = findRegister(blockedRegisters, commands, pos, rd, fallbackRegister(targetRegister));
				val.loadValue(rd.reg, blockedRegisters, commands, pos);
				Command cmpCmd, jmpPosCmd, mov0Cmd, jmpCmd, mov1Cmd;
				Param p1, p2;
				ParamBuilder build = new ParamBuilder();
				long relative;
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				build.v1 = 1L;
				p2 = build.build();
				mov1Cmd = new Command(Commands.CMD_MOV, p1, p2);
				relative = mov1Cmd.length();
				build.v1 = 0L;
				p2 = build.build();
				mov0Cmd = new Command(Commands.CMD_MOV, p1, p2);
				build.v1 = relative;
				p1 = build.build();
				jmpCmd = new Command(Commands.CMD_JMP, p1, null);
				relative += mov0Cmd.length() + jmpCmd.length();
				build.v1 = relative;
				jmpPosCmd = new Command(jmp, p1, null);
				pos += relative + jmpPosCmd.length();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.v1 = rd.reg;
				p2 = build.build();
				cmpCmd = new Command(me.t == SimpleType.FPNUM ? Commands.CMD_CMPFP : Commands.CMD_CMP, p1, p2);
				pos += cmpCmd.length();
				commands.add(cmpCmd);
				commands.add(jmpPosCmd);
				commands.add(mov0Cmd);
				commands.add(jmpCmd);
				commands.add(mov1Cmd);
				pos = releaseRegister(commands, pos, rd, blockedRegisters);
				return pos;
			}
			
			@Override
			public String toString() {
				switch (jmp) {
				case CMD_JMPEQ:
					return "(" + me + " == " + val + ")";
				case CMD_JMPNE:
					return "(" + me + " != " + val + ")";
				case CMD_JMPGT:
					return "(" + me + " > " + val + ")";
				case CMD_JMPGE:
					return "(" + me + " >= " + val + ")";
				case CMD_JMPLE:
					return "(" + me + " <= " + val + ")";
				case CMD_JMPLT:
					return "(" + me + " < " + val + ")";
				default:
					throw new InternalError("unknown jmp command for compare value: " + jmp);
				}
			}
			
		};
	}
	
	@Override
	public SimpleValue addExpShift(SimplePool pool, int type, SimpleValue val) {
		switch (type) {
		case EXP_SHIFT_LEFT:
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_LSH, this, val);
		case EXP_SHIFT_ARITMETIC_RIGTH:
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_RASH, this, val);
		case EXP_SHIFT_LOGIC_RIGTH:
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_RLSH, this, val);
		default:
			throw new InternalError("unknown shift type: " + type);
		}
	}
	
	@Override
	public SimpleValue addExpAdd(SimplePool pool, boolean add, SimpleValue val) {
		if (add) {
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_ADD, this, val);
		} else {
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_SUB, this, val);
		}
	}
	
	@Override
	public SimpleValue addExpMul(SimplePool pool, int type, SimpleValue val) {
		switch (type) {
		case EXP_MULTIPLY:
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_MUL, this, val);
		case EXP_DIVIDE:
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_DIV, this, val);
		case EXP_MODULO:
			return new SimpleBiFunctionValue(findType(this, val), Commands.CMD_DIV, this, val);
		default:
			throw new InternalError("unknown mul type: " + type);
		}
	}
	
	@Override
	public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
		if (type == t) {
			return this;
		} else if (t.isStruct() ^ type.isStruct()) {
			throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
		} else if (type.isStruct() /* && t.isStruct() */) {
			if (t.byteCount() == type.byteCount()) {
				return castedValue(this, type);
			} else {
				throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
			}
		} else if (type.isPointerOrArray() ^ t.isPointerOrArray()) {
			if (t == SimpleType.FPNUM || type == SimpleType.FPNUM) {
				throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
			} else if (t.isPointerOrArray()) {
				return createPointerNumberCast(type, true);
			} else {
				return createPointerNumberCast(type, false);
			}
		} else if (type.isArray() /* && t.isArray() */) {
			return castedValue(this, type);
		} else if (type.isPointer() /* && t.isPointer() */) {
			return castedValue(this, type);
		} else if (t == SimpleType.FPNUM ^ type == SimpleType.FPNUM) {
			if (t == SimpleType.FPNUM) {
				return new SimpleUnFunctionValue(type, Commands.CMD_FPTN, this);
			} else {
				return new SimpleUnFunctionValue(type, Commands.CMD_NTFP, this);
			}
		} else if (t.isPrimitive() && type.isPrimitive()) {
			return castedValue(this, type);
		} else {
			throw new InternalError("unknown cast!");
		}
	}
	
	private SimpleValueNoConst createPointerNumberCast(SimpleType type, boolean pointerToNumber) {
		return new SimpleValueNoConst(type) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				pos = SimpleValueNoConst.this.loadValue(targetRegister, blockedRegisters, commands, pos);
				Command cmpCmd, jmpneCmd, mov0Or_1Cmd;
				Param p1, p2;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				if (pointerToNumber) {
					build.v1 = -1L;
				} else {
					build.v1 = 0L;
				}
				p2 = build.build();
				cmpCmd = new Command(Commands.CMD_CMP, p1, p2);
				pos += cmpCmd.length();
				if (pointerToNumber) {
					build.v1 = 0L;
				} else {
					build.v1 = -1L;
				}
				p2 = build.build();
				mov0Or_1Cmd = new Command(Commands.CMD_MOV, p1, p2);
				long mov0Len = mov0Or_1Cmd.length();
				pos += mov0Len;
				build.v1 = mov0Len;
				p1 = build.build();
				jmpneCmd = new Command(Commands.CMD_JMPNE, p1, null);
				pos += jmpneCmd.length();
				commands.add(cmpCmd);
				commands.add(jmpneCmd);
				commands.add(mov0Or_1Cmd);
				return pos;
			}
			
			@Override
			public String toString() {
				return "(" + type + ") (" + SimpleValueNoConst.this + ")";
			}
			
		};
	}
	
	private static SimpleValue castedValue(final SimpleValueNoConst value, SimpleType type) {
		return new SimpleValueNoConst(type) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				return value.loadValue(targetRegister, blockedRegisters, commands, pos);
			}
			
			@Override
			public String toString() {
				return "(" + t + ") (" + value.toString() + ")";
			}
			
		};
	}
	
	@Override
	public SimpleValue addExpUnary(SimplePool pool, int type) {
		boolean isNumber = t.isPrimitive() || t.isPointer();
		boolean isNoFPNumber = isNumber && t != SimpleType.FPNUM;
		switch (type) {
		case EXP_UNARY_AND:
			return mkPointer(pool);
		case EXP_UNARY_BITWISE_NOT:
			if (isNoFPNumber) {
				return new SimpleUnFunctionValue(t, Commands.CMD_NOT, this);
			} else {
				throw new IllegalStateException("plus/minus only possible on numbers (no fp nums)!");
			}
		case EXP_UNARY_BOOLEAN_NOT:
			if ( !isNumber) {
				throw new IllegalStateException("plus/minus only possible on numbers (and fp nums)!");
			}
			return createUnaryBooleanNotValue();
		case EXP_UNARY_MINUS:
			if (isNumber) {
				if (isNoFPNumber) {
					return new SimpleUnFunctionValue(t, Commands.CMD_NEG, this);
				} else {
					return new SimpleBiFunctionValue(t, Commands.CMD_MULFP, this, new SimpleFPNumberValue( -1.0D));
				}
			} else {
				throw new IllegalStateException("plus/minus only possible on numbers (and fp nums)!");
			}
		case EXP_UNARY_PLUS:
			if (isNumber) {
				return this;
			} else {
				throw new IllegalStateException("plus/minus only possible on numbers!");
			}
		case EXP_UNARY_NONE:
			throw new InternalError("unary none is no valid type!");
		default:
			throw new InternalError("unknown unary type: " + type);
		}
	}
	
	protected SimpleValue mkPointer(SimplePool pool) {
		throw new IllegalStateException("can not make a pointer to this value (this: " + this + ")");
	}
	
	private SimpleValueNoConst createUnaryBooleanNotValue() {
		return new SimpleValueNoConst(SimpleType.NUM) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				pos = SimpleValueNoConst.this.loadValue(targetRegister, blockedRegisters, commands, pos);
				Command cmpCmd, jmpneCmd, mov0Cmd, jmpCmd, mov1Cmd;
				Param p1, p2;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				build.v1 = 1;
				p2 = build.build();
				mov1Cmd = new Command(Commands.CMD_MOV, p1, p2);
				long cmdLen = mov1Cmd.length();
				pos += cmdLen;
				long relative = cmdLen;
				build.art = A_NUM;
				build.v1 = relative;
				p1 = build.build();
				jmpCmd = new Command(Commands.CMD_JMP, p1, null);
				cmdLen = jmpCmd.length();
				pos += cmdLen;
				relative += cmdLen;
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				build.v1 = 0;
				p2 = build.build();
				mov0Cmd = new Command(Commands.CMD_MOV, p1, p2);
				cmdLen = mov0Cmd.length();
				pos += cmdLen;
				relative += cmdLen;
				build.art = A_NUM;
				build.v1 = relative;
				p1 = build.build();
				jmpneCmd = new Command(Commands.CMD_JMPNE, p1, null);
				cmdLen = jmpneCmd.length();
				pos += cmdLen;
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				Commands cmd = Commands.CMD_CMP;
				if (SimpleValueNoConst.this.t == SimpleType.FPNUM) {
					build.v1 = Double.doubleToRawLongBits(0.0D);
					cmd = Commands.CMD_CMPFP;
				} else if (SimpleValueNoConst.this.t.isPointer()) {
					build.v1 = -1L;
				} else {
					build.v1 = 0L;
				}
				p2 = build.build();
				cmpCmd = new Command(cmd, p1, p2);
				cmdLen = cmpCmd.length();
				pos += cmdLen;
				commands.add(cmpCmd);
				commands.add(jmpneCmd);
				commands.add(mov0Cmd);
				commands.add(jmpCmd);
				commands.add(mov1Cmd);
				return pos;
			}
			
			@Override
			public String toString() {
				return "!(" + SimpleValueNoConst.this + ')';
			}
			
		};
	}
	
	@Override
	public SimpleValue addExpDerefPointer(SimplePool pool) {
		if ( !t.isPointer()) {
			throw new IllegalStateException("only a pointer can be dereferenced! (me: " + this + ")");
		}
		SimpleType target = ((SimpleTypePointer) t).target;
		if (target == SimpleTypePrimitive.pt_inval) {
			throw new IllegalStateException("this pointer has no valid target type! (me: " + this + ")");
		}
		if (target.isStruct() || target.isArray()) {
			return castedValue(this, target);
		}
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(target) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				Param p1, p2;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_SR | B_REG;
				p2 = build.build();
				addMovCmd(t, commands, pos, p1, p2, targetRegister);
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
		if ( !t.isPointerOrArray()) {
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
			throw new IllegalStateException("this pointer/array has no valid target type! (me: " + this + ")[" + val + "]");
		}
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(target) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				Param p1, p2;
				ParamBuilder build = new ParamBuilder();
				RegisterData rd = null;
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				if (val.isConstNoData()) {
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
				pos = addMovCmd(t, commands, pos, p1, p2, targetRegister);
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
	
	protected long addMovCmd(SimpleType type, List <Command> commands, long pos, Param param1, Param param2, int targetRegister) {
		int bits = 64;
		boolean signed = true;
		Commands mov = Commands.CMD_MOV;
		Command bcpCmd = null, jmpnbCmd = null, orCmd = null, jmpCmd = null, andCmd = null;
		// only cmpCmd and andCmd when not signed
		if (type.isPrimitive()) {
			bits = ((SimpleTypePrimitive) type).bits();
			signed = ((SimpleTypePrimitive) type).signed();
			if (bits != 64) {
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
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = targetRegister;
				p1 = build.build();
				build.art = A_NUM;
				build.v1 = cmpbits;
				p2 = build.build();
				bcpCmd = new Command(Commands.CMD_BCP, p1, p2);
				pos += bcpCmd.length();
				build.v1 = andbits;
				p2 = build.build();
				andCmd = new Command(Commands.CMD_AND, p1, p2);
				long andLen = andCmd.length();
				if (signed) {
					long relative = andLen;
					build.v1 = ~andbits;
					p2 = build.build();
					orCmd = new Command(Commands.CMD_OR, p1, p2);
					build.v1 = relative;
					p1 = build.build();
					jmpCmd = new Command(Commands.CMD_JMP, p1, null);
					relative += orCmd.length() + jmpCmd.length();
					build.v1 = relative;
					jmpnbCmd = new Command(Commands.CMD_JMPNB, p1, null);
					pos += relative;
				} else {
					pos += andLen;
				}
			}
		}
		Command addCmd = new Command(mov, param1, param2);
		pos += addCmd.length();
		commands.add(addCmd);
		if (bcpCmd != null) {
			commands.add(bcpCmd);
			if (signed) {
				commands.add(jmpnbCmd);
				commands.add(orCmd);
				commands.add(jmpCmd);
			}
			commands.add(andCmd);
		}
		return pos;
	}
	
	@Override
	public SimpleValue addExpNameRef(SimplePool pool, String text) {
		if ( !t.isStruct()) {
			throw new IllegalStateException("name referencing is only possible on files!");
		}
		SimpleStructType struct = (SimpleStructType) t;
		int off = 0;
		SimpleVariable target = null;
		for (SimpleVariable sv : struct.members) {
			if (sv.name.equals(text)) {
				target = sv;
				break;
			}
			off += sv.type.byteCount();
		}
		if (target == null) {
			throw new IllegalStateException("this structure does not has a member with the name '" + text + "' (struct: " + t + ") (me: " + this + ")");
		}
		final int offset = off;
		final SimpleValueNoConst me = this;
		return new SimpleValueNoConst(target.type) {
			
			@Override
			public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
				pos = me.loadValue(targetRegister, blockedRegisters, commands, pos);
				Param p1, p2;
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
					addMovCmd(t, commands, pos, p1, p2, targetRegister);
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
	
	private static SimpleType findType(SimpleValue val, SimpleValue val2) {
		return findType(val.type(), val2.type());
	}
	
	private static SimpleType findType(SimpleType st1, SimpleType st2) {
		if (st1.equals(st2)) {
			return st1;
		} else if (st1 == SimpleType.FPNUM || st2 == SimpleType.FPNUM) {
			return SimpleType.FPNUM;
		} else if (st1.isStruct() || st2.isStruct()) {
			throw new IllegalStateException("there is no fallback type for a structures with a diffrent type");
		} else if (st1.isPointerOrArray() && st2.isPointerOrArray()) {
			SimpleType t;
			try {
				t = findType( ((SimpleTypePointer) st1).target, ((SimpleTypePointer) st2).target);
			} catch (IllegalStateException e) {
				t = SimpleTypePrimitive.pt_inval;
			}
			return new SimpleTypePointer(t);
		} else if (st1.isPointerOrArray() || st2.isPointerOrArray()) {
			return SimpleType.NUM;
		} else {
			assert st1.isPrimitive();
			assert st2.isPrimitive();
			assert st1 != SimpleTypePrimitive.pt_inval;
			assert st2 != SimpleTypePrimitive.pt_inval;
			SimpleTypePrimitive pt1 = (SimpleTypePrimitive) st1,
				pt2 = (SimpleTypePrimitive) st2;
			boolean signed = pt1.signed() || pt2.signed();
			int bits = Math.max(pt1.bits(), pt2.bits());
			return SimpleTypePrimitive.get(bits, signed);
		}
	}
	
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
		
		public SimpleConditionalValue(SimpleType type, SimpleValue cond, SimpleValue pos, SimpleValue neg) {
			super(type);
			this.condition = cond;
			this.positive = pos;
			this.negative = neg;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			pos = condition.loadValue(targetRegister, blockedRegisters, commands, pos);
			Param p1, p2;
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
			List <Command> sub = newList();
			blockedRegisters[targetRegister] = false;
			long np = positive.loadValue(targetRegister, blockedRegisters, commands, pos);
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
		public boolean isConstNoData() {
			return false;
		}
		
		@Override
		public boolean isConstData() {
			return false;
		}
		
		@Override
		public String toString() {
			return "(" + condition + ") ? (" + positive + ") : (" + negative + ")";
		}
		
	}
	
	protected List <Command> newList() {
		return new LinkedList <>();
	}
	
	private void checkNumber(SimpleValue val) {
		if (t.isStruct() || val.type().isStruct()) {
			throw new IllegalStateException("a structure is no number!");
		}
		if (t.isArray() || val.type().isArray()) {
			throw new IllegalStateException("an arrays is no number!");
		}
	}
	
	public static class SimpleUnFunctionValue extends SimpleValueNoConst {
		
		private final Commands    cmd;
		private final SimpleValue param1;
		
		public SimpleUnFunctionValue(SimpleType type, Commands cmd, SimpleValue param1) {
			super(type);
			this.cmd = cmd;
			this.param1 = param1;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			pos = param1.loadValue(targetRegister, blockedRegisters, commands, pos);
			Param p1;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			build.v1 = targetRegister;
			p1 = build.build();
			Command addCmd = new Command(cmd, p1, null);
			pos += addCmd.length();
			commands.add(addCmd);
			return pos;
		}
		
		@Override
		public String toString() {
			return cmd + " (" + param1 + ')';
		}
		
	}
	
	public static class SimpleBiFunctionValue extends SimpleValueNoConst {
		
		private final Commands    cmd;
		private final SimpleValue param1;
		private final SimpleValue param2;
		private final boolean     p2IsTarget;
		
		public SimpleBiFunctionValue(SimpleType type, Commands cmd, SimpleValue p1, SimpleValue p2) {
			this(type, cmd, p1, p2, false);
		}
		
		public SimpleBiFunctionValue(SimpleType type, Commands cmd, SimpleValue p1, SimpleValue p2, boolean p2IsTarget) {
			super(type);
			this.cmd = cmd;
			this.param1 = p1;
			this.param2 = p2;
			this.p2IsTarget = p2IsTarget;
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			RegisterData rdp2 = new RegisterData(targetRegister + 1);
			pos = findRegister(blockedRegisters, commands, pos, rdp2, fallbackRegister(targetRegister));
			Param p1, p2;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			if (p2IsTarget) {
				param2.loadValue(targetRegister, blockedRegisters, commands, pos);
				param1.loadValue(rdp2.reg, blockedRegisters, commands, pos);
				build.v1 = targetRegister;
				p1 = build.build();
				build.v1 = rdp2.reg;
				p2 = build.build();
			} else {
				param1.loadValue(targetRegister, blockedRegisters, commands, pos);
				param2.loadValue(rdp2.reg, blockedRegisters, commands, pos);
				build.v1 = rdp2.reg;
				p1 = build.build();
				build.v1 = targetRegister;
				p2 = build.build();
			}
			Command addCmd = new Command(cmd, p1, p2);
			pos += addCmd.length();
			commands.add(addCmd);
			pos = releaseRegister(commands, pos, rdp2, blockedRegisters);
			return pos;
		}
		
		@Override
		public boolean isConstNoData() {
			return false;
		}
		
		@Override
		public boolean isConstData() {
			return false;
		}
		
		@Override
		public String toString() {
			return cmd.toString() + " (" + param1 + "), (" + param2 + ")";
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
	
	private static long findRegister(boolean[] blockedRegisters, List <Command> commands, long pos, RegisterData rd, int fallback) {
		int startRegister = rd.reg;
		for (; rd.reg < 256 && blockedRegisters[rd.reg]; rd.reg ++ );
		if (rd.reg >= 256) {
			for (rd.reg = MIN_REGISTER; blockedRegisters[rd.reg] && rd.reg < startRegister; rd.reg ++ );
			if (rd.reg >= startRegister) {
				rd.pushPop = true;
				blockedRegisters[fallback] = false;
				rd.reg = fallback;
				ParamBuilder build = new ParamBuilder();
				build.art = A_SR;
				build.v1 = rd.reg;
				Param p1 = build.build();
				Command addCmd = new Command(Commands.CMD_PUSH, p1, null);
				commands.add(addCmd);
				pos += addCmd.length();
			}
		}
		return pos;
	}
	
	private static long releaseRegister(List <Command> commands, long pos, RegisterData rd, boolean[] blockedRegisters) {
		if (rd.pushPop) {
			blockedRegisters[rd.reg] = true;
			ParamBuilder build = new ParamBuilder();
			build.art = A_SR;
			build.v1 = rd.reg;
			Param p1 = build.build();
			Command addCmd = new Command(Commands.CMD_POP, p1, null);
			commands.add(addCmd);
			pos += addCmd.length();
		} else {
			blockedRegisters[rd.reg] = false;
		}
		return pos;
	}
	
}
