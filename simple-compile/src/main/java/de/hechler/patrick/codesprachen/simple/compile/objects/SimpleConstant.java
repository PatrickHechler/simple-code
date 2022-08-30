package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.hechler.patrick.codesprachen.primitive.core.objects.PrimitiveConstant;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueConst;

public class SimpleConstant extends PrimitiveConstant implements SimpleExportable {
	
	private static final Path SIMPLE_CONSTANT_PATH = Paths.get("[SIMPLE_CONSTANT_PATH]");
	
	public final boolean export;
	
	public SimpleConstant(String name, long value, boolean export) {
		super(name, null, value, SIMPLE_CONSTANT_PATH, -1);
		this.export = export;
	}
	
	public static SimpleConstant create(String name, SimpleValue val, boolean export) {
		if ( !val.isConstant()) {
			throw new IllegalArgumentException("the constant '" + name + "' has no constant value: (" + val + ")");
		}
		SimpleValueConst cv = (SimpleValueConst) val;
		if ( !cv.implicitNumber()) {
			throw new IllegalArgumentException("the constant '" + name + "' has no constant (implicit) number value: (" + cv + ")");
		}
		return new SimpleConstant(name, cv.getNumber(), export);
	}
	
	@Override
	public boolean isExport() {
		return this.export;
	}
	
	@Override
	public String name() {
		return super.name;
	}
	
	@Override
	public String toExportString() {
		if (!export) {
			throw new IllegalStateException("this is not marked as export!");
		}
		StringBuilder b = new StringBuilder();
		b.append(CONST);
		b.append(this.name);
		b.append(CONST);
		b.append(Long.toHexString(this.value));
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return ((int) value) | ((int) value >> 32);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SimpleConstant other = (SimpleConstant) obj;
		if (name == null) {
			if (other.name != null) return false;
		} else if ( !name.equals(other.name)) return false;
		if (value != other.value) return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("const ");
		if (this.export) {
			b.append("exp ");
		}
		b.append(this.name);
		b.append(" = ");
		b.append(this.value);
		b.append(';');
		return b.toString();
	}
	
}
