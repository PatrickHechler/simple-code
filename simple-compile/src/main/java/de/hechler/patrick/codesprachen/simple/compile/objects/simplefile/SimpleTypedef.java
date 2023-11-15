package de.hechler.patrick.codesprachen.simple.compile.objects.simplefile;

import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record SimpleTypedef(String name, int flags, SimpleType type) {
	
	public static final int FLAG_EXPORT = 0x1000;
	public static final int FLAG_FROM_ME_DEP = 0x2000;
	
}
