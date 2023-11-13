package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.ArrayType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.StructType;

public record DataVal(byte[] value, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue createString(List<String> value, ErrorContext ctx) {
		StringBuilder sb = new StringBuilder(value.stream().mapToInt(String::length).sum() + 1);
		for (String str : value) {
			sb.append(str);
		}
		sb.append('\0');
		byte[] val = sb.toString().getBytes(StandardCharsets.UTF_8);
		return new DataVal(val, stringType(val, ctx), ctx);
	}
	
	public static SimpleValue create(List<SimpleValue> value, ErrorContext ctx) {
		if ( value.isEmpty() ) return new DataVal(new byte[0],
			ArrayType.create(StructType.create(List.of(), StructType.FLAG_NOUSE, ctx), 0, ctx), ctx);
		SimpleValue val = value.get(0);
		SimpleType t = val.type();
		long len = t.size() * value.size();
		if ( len != (int) len ) throw new CompileError(ctx,
			"the maximum size of a constant array 2^31-1 bytes (at least for this compiler)");
		byte[] data = new byte[(int) len];
		return new DataVal(data, new ArrayType(t, value.size()), ctx);
	}
	
	private static SimpleType stringType(byte[] data, ErrorContext ctx) {
		return ArrayType.create(NativeType.UBYTE, data.length, ctx);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.type.hashCode();
		result = prime * result + Arrays.hashCode(this.value);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		DataVal other = (DataVal) obj;
		if ( !this.type.equals(other.type) ) return false;
		return Arrays.equals(this.value, other.value);
	}
	
	@Override
	public boolean isConstant() { return true; }
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public String toString() {
		if ( this.type instanceof ArrayType at ) {
			if ( at.target() == NativeType.UBYTE ) {
				return "\"" + new String(Arrays.copyOf(this.value, this.value.length - 1), StandardCharsets.UTF_8)
					.replace("\0", "\\0").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
					.replace("\"", "\\\"") + "\""; // note that \u0000 escape sequences are not supported in the
													// backwards translation
			}
			return fallbackToString();
		}
		return fallbackToString();
	}
	
	private String fallbackToString() {
		StringBuilder b = new StringBuilder();
		b.append("DataVal [value=");
		boolean first = true;
		for (byte valb : this.value) {
			if ( !first ) {
				b.append(", ");
			} else {
				first = false;
			}
			String str = Integer.toHexString(0xFF & valb);
			if ( str.length() == 1 ) b.append('0');
			b.append(str);
		}
		b.append(", type=");
		b.append(this.type);
		b.append("]");
		return b.toString();
	}
	
}
