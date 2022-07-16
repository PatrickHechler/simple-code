package de.hechler.patrick.codesprachen.simple.compile.objects.antl.values;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class SimpleStringValue extends SimpleValueConstPointer {
	
	public SimpleStringValue(List <String> strs) {
		super(data(strs), 16);
	}
	
	private static final byte[] data(List <String> strs) {
		int len = 0;
		for (String str : strs) {
			len += str.length() << 1;
			if (len < 0) {
				throw new OutOfMemoryError();
			}
		}
		byte[] bytes = new byte[len];
		int off = 0;
		for (String str : strs) {
			byte[] cpy = str.getBytes(StandardCharsets.UTF_16LE);
			System.arraycopy(cpy, 0, bytes, off, cpy.length);
			off += cpy.length;
		}
		assert off == bytes.length;
		return bytes;
	}
	
	@Override
	public String toString() {
		return "\"" + new String(data, StandardCharsets.UTF_16LE).replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t").replace("\0", "\\0") + "\"";
	}
	
}
