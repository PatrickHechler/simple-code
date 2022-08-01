package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import java.nio.charset.StandardCharsets;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleTypeArray;

public class SimpleStringValue extends SimpleValueDataPointer {
	
	public SimpleStringValue(List <String> strs) {
		this(data(strs));
	}
	
	public SimpleStringValue(byte[] data) {
		super(type(data), data);
	}
	
	private static final SimpleType type(byte[] data) {
		return new SimpleTypeArray(SimpleType.UWORD, data.length >> 1);
	}
	
	private static final byte[] data(List <String> strs) {
		int len = 0;
		for (String str : strs) {
			len += str.length() << 1;
			if (len < 0) {
				throw new OutOfMemoryError();
			}
		}
		byte[] bytes = new byte[len + 2];
		int off = 0;
		for (String str : strs) {
			byte[] cpy = str.getBytes(StandardCharsets.UTF_16LE);
			System.arraycopy(cpy, 0, bytes, off, cpy.length);
			off += cpy.length;
		}
		assert off == bytes.length - 2;
		// bytes[bytes.length - 2] = 0;
		// bytes[bytes.length - 1] = 0;
		return bytes;
	}
	
	@Override
	public String toString() {
		return "\"" + new String(data, StandardCharsets.UTF_16LE).replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t").replace("\0", "\\0") + "\"";
	}
	
}
