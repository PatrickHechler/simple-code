package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypeArray;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePointer;

public abstract class SimpleValueDataPointer extends SimpleValueNoConst {
	
	/**
	 * this array is not allowed to be modified
	 */
	public final byte[] data;
	/**
	 * this value is only allowed to be set once (twice when the -1 at the start counts) by the compiler!
	 */
	public long         addr = -1L;
	
	public SimpleValueDataPointer(SimpleType mytype, byte[] data) {
		super(mytype);
		this.data = data;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		assert !blockedRegisters[targetRegister];
		blockedRegisters[targetRegister] = true;
		assert this.addr != -1L;
		Param p1, p2;
		ParamBuilder build = new ParamBuilder();
		build.art = ParamBuilder.A_SR;
		build.v1 = targetRegister;
		p1 = build.build();
		build.art = ParamBuilder.A_NUM;
		build.v1 = pos - addr;
		p2 = build.build();
		Command addCmd = new Command(Commands.CMD_LEA, p1, p2);
		pos += addCmd.length();
		commands.add(addCmd);
		return pos;
	}
	
	@Override
	protected SimpleValue mkPointer(SimplePool pool) {
		if (t.isArray()) {
			return new SimpleCastedArrayValue(new SimpleTypePointer( ((SimpleTypeArray) t).target));
		} else {
			assert t.isPointer();
			return super.mkPointer(pool);
		}
	}
	
	@Override
	public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
		if ( !t.isArray()) {
			assert t.isPointer();
			return super.addExpCast(pool, type);
		} else if ( !type.isArray()) {
			throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
		} else {
			return new SimpleCastedArrayValue(type);
		}
	}
	
	public class SimpleCastedArrayValue extends SimpleValueNoConst {
		
		public SimpleCastedArrayValue(SimpleType type) {
			super(type);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			return SimpleValueDataPointer.this.loadValue(targetRegister, blockedRegisters, commands, pos);
		}
		
		@Override
		protected SimpleValue mkPointer(SimplePool pool) {
			if (t.isArray()) {
				return new SimpleCastedArrayValue(new SimpleTypePointer( ((SimpleTypeArray) t).target));
			} else {
				assert t.isPointer();
				return super.mkPointer(pool);
			}
		}
		
		@Override
		public SimpleValue addExpCast(SimplePool pool, SimpleType type) {
			if ( !t.isArray()) {
				assert t.isPointer();
				return super.addExpCast(pool, type);
			} else if ( !type.isArray()) {
				throw new IllegalStateException("can not cast from '" + t + "' to '" + type + "'");
			} else {
				return new SimpleCastedArrayValue(type);
			}
		}
		
		@Override
		public String toString() {
			return "(" + t + ") (" + SimpleValueDataPointer.this + ")";
		}
		
	}
	
}
