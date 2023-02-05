package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypeArray;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

public abstract class SimpleValueDataPointer extends SimpleValueNoConst {
	
	/**
	 * this array is not allowed to be modified
	 */
	public final byte[] data;
	/**
	 * this value is only allowed to be set once (twice when the -1 at the start counts) by the compiler!
	 */
	private long         addr = -1L;
	
	public SimpleValueDataPointer(SimpleType mytype, byte[] data) {
		super(mytype);
		this.data = data;
	}
	
	public void init(long addr) {
		if (this.addr != -1L) {
			throw new AssertionError("already initilized");
		}
		this.addr = addr;
	}
	
	public long addr() {
		if (this.addr == -1L) {
			throw new AssertionError("not yet initilized");
		}
		return this.addr;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos, VarLoader loader) {
		Param reg = blockRegister(targetRegister, blockedRegisters);
		if (this.addr == -1L) {
			throw new AssertionError("not yet initilized");
		}
		Command leaCmd = new Command(Commands.CMD_LEA, reg, build(A_NUM, addr - pos));
		pos += leaCmd.length();
		commands.add(leaCmd);
		return pos;
	}
	
	@Override
	public SimpleValue mkPointer(SimplePool pool) {
		if (t.isArray()) {
			return new SimpleCastedDataValue(new SimpleTypePointer( ((SimpleTypeArray) t).target));
		} else if (!t.isPointer()) {
			return super.mkPointer(pool);
		} else {
			throw new AssertionError();
		}
	}
	
	@Override
	public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
		if ( !t.isArray()) {
			assert t.isPointer();
			if (type.isPointer()) {
				return new SimpleCastedDataValue(type);
			} else {
				return super.addExpCast(pool, type);
			}
		} else if ( !type.isArray()) {
			throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
		} else {
			return new SimpleCastedDataValue(type);
		}
	}
	
	public class SimpleCastedDataValue extends SimpleValueNoConst {
		
		public SimpleCastedDataValue(SimpleType type) {
			super(type);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos, VarLoader loader) {
			return SimpleValueDataPointer.this.loadValue(targetRegister, blockedRegisters, commands, pos, loader);
		}
		
		@Override
		public SimpleValue mkPointer(SimplePool pool) {
			if (t.isArray()) {
				return new SimpleCastedDataValue(new SimpleTypePointer( ((SimpleTypeArray) t).target));
			} else {
				assert t.isPointer();
				return super.mkPointer(pool);
			}
		}
		
		@Override
		public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
			if ( !t.isArray()) {
				assert t.isPointer();
				if (type.isPointer()) {
					return new SimpleCastedDataValue(type);
				} else {
					return super.addExpCast(pool, type);
				}
			} else if ( !type.isArray()) {
				throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
			} else {
				return new SimpleCastedDataValue(type);
			}
		}
		
		@Override
		public String toString() {
			return "(" + t + ") (" + SimpleValueDataPointer.this + ")";
		}
		
	}
	
}
