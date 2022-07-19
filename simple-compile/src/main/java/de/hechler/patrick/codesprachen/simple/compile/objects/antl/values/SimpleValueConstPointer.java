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

public abstract class SimpleValueConstPointer extends SimpleValueConst {
	
	/**
	 * this array is not allowed to be modified
	 */
	public final byte[] data;
	/**
	 * this value is only allowed to be set once (twice when the -1 at the start counts)!
	 */
	public long         addr = -1L;
	
	public SimpleValueConstPointer(SimpleType mytype, byte[] data) {
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
		build.v1 = addr;
		p2 = build.build();
		Command addCmd = new Command(Commands.CMD_LEA, p1, p2);
		pos += addCmd.length();
		commands.add(addCmd);
		return pos;
	}
	
	@Override
	protected boolean isDataPointerOrArray() {
		return true;
	}
	
	@Override
	public boolean implicitNumber() {
		return t.isPointer();
	}
	
	@Override
	public boolean implicitFPNumber() {
		return t.isPointer();
	}
	
	@Override
	public long getNumber() {
		if (t.isArray()) {
			throw new IllegalStateException("this is an array!");
		}
		if (addr == -1L) {
			throw new IllegalStateException("address not initilized!");
		}
		return addr;
	}
	
	@Override
	public int getNumberBits() {
		if (t.isArray()) {
			throw new IllegalStateException("this is an array!");
		}
		return 64;
	}
	
	@Override
	public double getFPNumber() {
		if (addr == -1L) {
			throw new IllegalStateException("address not initilized!");
		}
		return addr;
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
	protected SimpleValue arrayDeref(long val) {
		return arrayDeref(t, val);
	}
	
	private SimpleValue arrayDeref(SimpleType myType, long val) {
		SimpleType target = ((SimpleTypePointer) myType).target;
		int elementByteCount = target.byteCount();
		if (val < 0 || val > (long) data.length - elementByteCount) {
			throw new IndexOutOfBoundsException();
		}
		if ( !target.isPrimitive() && !target.isPointer()) {
			throw new UnsupportedOperationException();
		}
		long value = 0L;
		int off = (int) val;
		switch (elementByteCount) {
		case 8:
			value |= (data[off + 7] & 0xFF) << 56;
			value |= (data[off + 6] & 0xFF) << 48;
			value |= (data[off + 5] & 0xFF) << 40;
			value |= (data[off + 4] & 0xFF) << 32;
		case 4:
			value |= (data[off + 3] & 0xFF) << 24;
			value |= (data[off + 2] & 0xFF) << 16;
		case 2:
			value |= (data[off + 1] & 0xFF) << 8;
		case 1:
			value |= (data[off] & 0xFF);
			break;
		default:
			throw new InternalError("elements are of a known type with unknown size! (elementByteCount: " + elementByteCount + " elementType: " + target + " myType: " + myType + " this: " + this + ")");
		}
		return new SimpleNumberValue(target, value);
	}
	
	public class SimpleCastedArrayValue extends SimpleValueConst {
		
		public SimpleCastedArrayValue(SimpleType type) {
			super(type);
		}
		
		@Override
		public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
			return SimpleValueConstPointer.this.loadValue(targetRegister, blockedRegisters, commands, pos);
		}
		
		@Override
		public boolean implicitNumber() {
			return SimpleValueConstPointer.this.implicitNumber();
		}
		
		@Override
		public boolean implicitFPNumber() {
			return SimpleValueConstPointer.this.implicitNumber();
		}
		
		@Override
		public long getNumber() {
			return SimpleValueConstPointer.this.getNumber();
		}
		
		@Override
		public int getNumberBits() {
			return SimpleValueConstPointer.this.getNumberBits();
		}
		
		@Override
		public double getFPNumber() {
			return SimpleValueConstPointer.this.getFPNumber();
		}
		
		@Override
		public String toString() {
			return "(" + t + ") (" + SimpleValueConstPointer.this + ")";
		}
		
		@Override
		protected SimpleValue arrayDeref(long val) {
			return SimpleValueConstPointer.this.arrayDeref(t, val);
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
		
	}
	
}
