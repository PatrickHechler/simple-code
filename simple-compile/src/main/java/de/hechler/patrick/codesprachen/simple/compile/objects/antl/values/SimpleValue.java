package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public interface SimpleValue {
	
	public static final int MIN_REGISTER = PrimAsmConstants.X_ADD;
	
	public static final int EXP_MULTIPLY              = 1;
	public static final int EXP_DIVIDE                = 2;
	public static final int EXP_GREATHER              = 3;
	public static final int EXP_GREATHER_EQUAL        = 4;
	public static final int EXP_MODULO                = 5;
	public static final int EXP_SHIFT_ARITMETIC_RIGTH = 6;
	public static final int EXP_SHIFT_LOGIC_RIGTH     = 7;
	public static final int EXP_SHIFT_LEFT            = 8;
	public static final int EXP_SMALLER_EQUAL         = 9;
	public static final int EXP_SMALLER               = 10;
	public static final int EXP_UNARY_NONE            = 11;
	public static final int EXP_UNARY_PLUS            = 12;
	public static final int EXP_UNARY_MINUS           = 13;
	public static final int EXP_UNARY_AND             = 14;
	public static final int EXP_UNARY_BITWISE_NOT     = 15;
	public static final int EXP_UNARY_BOOLEAN_NOT     = 16;
	
	/**
	 * adds a command sequence which loads the runtime value of this {@link SimpleValue} to the {@code targetRegister}.<br>
	 * the {@code blockedRegisters} are not allowed to be used.<br>
	 * after this method returns the {@code blockedRegisters} will additionally contain the {@code targetRegister}.<br>
	 * {@code blockedRegisters} is not allowed to contain {@code targetRegister}
	 * <p>
	 * if this value represents an array or pointer the address of the first/target element is loaded
	 * <p>
	 * the return value will be the new length of the binary in bytes
	 * <p>
	 * note that the list does not have to contain all commands (it may even be empty (even if pos is not zero))
	 * 
	 * @return the new size of the {@code commands} in bytes
	 * @param targetRegister
	 *                         the target register which will contain the runtime value
	 * @param blockedRegisters
	 *                         the registers wich are not allowed to be modified (the length must be 256)
	 * @param commands
	 *                         the list of commands, where the commands should be added
	 * @param pos
	 *                         the current length of current the binary in bytes
	 */
	long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos);
	
	boolean isConstant();
	
	boolean isConstNoDataPointer();
	
	boolean isConstDataPointer();
	
	SimpleValue addExpCond(SimpleValue val, SimpleValue val2);
	
	SimpleValue addExpLOr(SimpleValue val);
	
	SimpleValue addExpLAnd(SimpleValue val);
	
	SimpleValue addExpOr(SimpleValue val);
	
	SimpleValue addExpXor(SimpleValue val);
	
	SimpleValue addExpAnd(SimpleValue val);
	
	SimpleValue addExpEq(boolean equal, SimpleValue val);
	
	SimpleValue addExpRel(int type, SimpleValue val);
	
	SimpleValue addExpShift(int type, SimpleValue val);
	
	SimpleValue addExpAdd(boolean add, SimpleValue val);
	
	SimpleValue addExpMul(int type, SimpleValue val);
	
	SimpleValue addExpCast(SimpleType t);
	
	SimpleValue addExpUnary(int type);
	
	SimpleValue addExpDerefPointer();
	
	SimpleValue addExpArrayRef(SimpleValue val);
	
}
