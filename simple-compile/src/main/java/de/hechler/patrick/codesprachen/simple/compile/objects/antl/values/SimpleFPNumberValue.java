package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;

public class SimpleFPNumberValue extends SimpleValueConst implements SimpleValue {
	
	public final double value;
	
	public SimpleFPNumberValue(double value) {
		super(SimpleType.FPNUM);
		this.value = value;
	}
	
	@Override
	public long loadValue(int targetRegister, boolean[] blockedRegisters, List <Command> commands, long pos) {
		return SimpleNumberValue.loadValue(64, Double.doubleToRawLongBits(value), targetRegister, blockedRegisters, commands, pos);
	}
	
	@Override
	public boolean implicitNumber() {
		return false;
	}
	
	@Override
	public boolean implicitFPNumber() {
		return true;
	}
	
	@Override
	public long getNumber() {
		throw new IllegalStateException();
	}
	
	@Override
	public int getNumberBits() {
		throw new IllegalStateException();
	}
	
	@Override
	public double getFPNumber() {
		return value;
	}
	
	@Override
	public String toString() {
		return Double.toString(value);
	}
	
}
