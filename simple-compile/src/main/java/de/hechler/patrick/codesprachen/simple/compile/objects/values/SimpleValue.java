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

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.SimpleCompiler;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

public interface SimpleValue {
	
	/**
	 * the minimum register number which is allowed to be used when a value should load its value to a register
	 */
	public static final int MIN_REGISTER = SimpleCompiler.MIN_TMP_VAL_REG;
	public static final int MAX_REGISTER = SimpleCompiler.MAX_TMP_VAL_REG;
	
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

	public static final SimpleValue ZERO = new SimpleNumberValue(SimpleType.NUM, 0L);
	public static final SimpleValue ONE = new SimpleNumberValue(SimpleType.NUM, 1L);
	
	public static interface VarLoader {
		
		long loadVar(long pos, int targetRegister, List<Command> commands, SimpleFunctionVariable sv);
		
		long loadVarPntr(long pos, int targetRegister, List<Command> commands, SimpleFunctionVariable sv);
		
	}
	
	public static class StackUseListener {
		
		private boolean allowed = false;
		
		private long size = 0;
		
		public void setForbidden() {
			allowed = true;
		}
		
		public long size() {
			return size;
		}
		
		public void grow(long value) {
			if (!allowed) {
				throw new IllegalStateException("stack grow is forbidden!");
			}
			if (value <= 0L) {
				throw new AssertionError(value);
			}
			size += value;
		}
		
		public void shrink(long value) {
			if (value <= 0L) {
				throw new AssertionError(value);
			}
			size -= value;
		}
		
	}
	
	/**
	 * adds a command sequence which loads the runtime value of this {@link SimpleValue} to the
	 * {@code targetRegister}.<br>
	 * the {@code blockedRegisters} are not allowed to be used.<br>
	 * after this method returns the {@code blockedRegisters} will additionally contain the {@code targetRegister}.<br>
	 * {@code blockedRegisters} is not allowed to contain {@code targetRegister}
	 * <p>
	 * note that the compiler is allowed to specify a target register below {@link #MIN_REGISTER}<br>
	 * if the target register is below {@link #MIN_REGISTER} it is still not allowed to use any other register below
	 * {@link #MIN_REGISTER} except of the <code>targetRegister</code>
	 * <p>
	 * if this value represents an array or structure (or pointer) the address of the first/target element is loaded
	 * <p>
	 * the return value will be the new length of the binary in bytes
	 * <p>
	 * note that the list does not have to contain all commands (it may even be empty (even if pos is not zero))
	 * 
	 * @return the new size of the {@code commands} in bytes
	 * 
	 * @param targetRegister   the target register which will contain the runtime value<br>
	 *                         note that the compiler is allowed to specify a target register below
	 *                         {@link #MIN_REGISTER}<br>
	 *                         if the target register is below {@link #MIN_REGISTER} it is still not allowed to use any
	 *                         other register below {@link #MIN_REGISTER} except of the <code>targetRegister</code>
	 * @param blockedRegisters the registers wich are not allowed to be modified (the length must be 256)
	 * @param commands         the list of commands, where the commands should be added
	 * @param pos              the current length of current the binary in bytes
	 */
	long loadValue(int targetRegister, boolean[] blockedRegisters, List<Command> commands, long pos, VarLoader loader, StackUseListener sul);
	
	SimpleType type();
	
	boolean isConstant();
	
	SimpleValue addExpCond(SimplePool pool, SimpleValue val, SimpleValue val2);
	
	SimpleValue addExpLOr(SimplePool pool, SimpleValue val);
	
	SimpleValue addExpLAnd(SimplePool pool, SimpleValue val);
	
	SimpleValue addExpOr(SimplePool pool, SimpleValue val);
	
	SimpleValue addExpXor(SimplePool pool, SimpleValue val);
	
	SimpleValue addExpAnd(SimplePool pool, SimpleValue val);
	
	SimpleValue addExpEq(SimplePool pool, boolean equal, SimpleValue val);
	
	SimpleValue addExpRel(SimplePool pool, int type, SimpleValue val);
	
	SimpleValue addExpShift(SimplePool pool, int type, SimpleValue val);
	
	SimpleValue addExpAdd(SimplePool pool, boolean add, SimpleValue val);
	
	SimpleValue addExpMul(SimplePool pool, int type, SimpleValue val);
	
	SimpleValue addExpCast(SimplePool pool, SimpleType t);
	
	SimpleValue addExpUnary(SimplePool pool, int type);
	
	SimpleValue addExpDerefPointer(SimplePool pool);
	
	SimpleValue addExpArrayRef(SimplePool pool, SimpleValue val);
	
	SimpleValue addExpNameRef(SimplePool pool, String text);
	
	SimpleValue cast(SimpleType t);

	SimpleValue mkPointer(SimplePool pool);
	
}
