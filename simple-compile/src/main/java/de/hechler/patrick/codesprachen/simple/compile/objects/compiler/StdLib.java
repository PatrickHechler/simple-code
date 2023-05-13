package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

@SuppressWarnings("javadoc")
public class StdLib {
	
	public static Map<String, SimpleFuncType> ALL_INTERRUPTS = allInts();
	
	// GENERATED-CODE-START
	private static Map<String, SimpleFuncType> allInts() {
		Map<String, SimpleFuncType> res = new HashMap<>();
		res.put("name", new SimpleFuncType(List.of(), List.of()));
		return Collections.unmodifiableMap(res);
	}
	// GENERATED-CODE-END
	
	private static SimpleOffsetVariable sv(SimpleType type, int deapth, String name) {
		if (deapth < 0) throw new AssertionError();
		while (deapth > 0) type = new SimpleTypePointer(type);
		return new SimpleVariable.SimpleOffsetVariable(type, name);
	}
	
}
