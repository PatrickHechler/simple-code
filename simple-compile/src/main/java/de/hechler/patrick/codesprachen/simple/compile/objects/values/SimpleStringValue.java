package de.hechler.patrick.codesprachen.simple.compile.objects.values;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypeArray;

public class SimpleStringValue extends SimpleValueDataPointer {
	
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
	public SimpleStringValue(List<String> strs) {
		this(data(strs));
	}
	
	public SimpleStringValue(String str) {
		this(str.getBytes(CHARSET));
	}
	
	public SimpleStringValue(byte[] data) {
		super(new SimpleTypeArray(SimpleType.UBYTE, data.length), data);
	}
	
	private static final byte[] data(List<String> strs) {
		int len = 0;
		for (String str : strs) {
			len += str.length() << 1;
			if (len < 0) {
				throw new OutOfMemoryError("too large");
			}
		}
		byte[] bytes = new byte[len + 1];
		int    off   = 0;
		for (String str : strs) {
			byte[] cpy = str.getBytes(CHARSET);
			System.arraycopy(cpy, 0, bytes, off, cpy.length);
			off += cpy.length;
		}
		assert off == bytes.length - 1;
		bytes[bytes.length - 1] = 0;
		return bytes;
	}
	
	@Override
	public String toString() {
		return "\"" + new String(data, CHARSET).replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
				.replace("\t", "\\t").replace("\0", "\\0") + "\"";
	}
	
}
