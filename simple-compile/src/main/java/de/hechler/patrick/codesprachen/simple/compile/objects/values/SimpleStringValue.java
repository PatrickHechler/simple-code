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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypeArray;

@SuppressWarnings("javadoc")
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
		if (strs.size() == 1) return strs.get(0).getBytes(CHARSET);
		int len = 0;
		for (String str : strs) {
			len += str.length();
			if (len < 0) {
				throw new OutOfMemoryError("too large");
			}
		}
		byte[] bytes = new byte[len + 1];
		int    off   = 0;
		for (String str : strs) {
			byte[] cpy = str.getBytes(CHARSET);
			if (cpy.length != str.length()) {
				int grow = cpy.length - str.length();
				byte[] nb = new byte[bytes.length + grow];
				System.arraycopy(bytes, 0, nb, 0, off);
				bytes = nb;
			}
			System.arraycopy(cpy, 0, bytes, off, cpy.length);
			off += cpy.length;
		}
		assert off == bytes.length - 1;
		bytes[bytes.length - 1] = 0;
		return bytes;
	}
	
	@Override
	public String toString() {
		return "\"" + new String(this.data, CHARSET).replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
				.replace("\t", "\\t").replace("\0", "\\0") + "\"";
	}
	
}
