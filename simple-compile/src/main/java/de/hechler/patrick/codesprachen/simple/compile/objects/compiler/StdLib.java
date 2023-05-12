package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;

@SuppressWarnings("javadoc")
public class StdLib {
	
	public record Func(SimpleFuncType signature, int paramStartReg, int resultStartReg) {
		
		public Func {
			if (paramStartReg < PrimAsmConstants.X_ADD || resultStartReg < PrimAsmConstants.X_ADD) {
				throw new AssertionError();
			}
			long bc = signature.byteCount();
			if (paramStartReg < SimpleCompiler.MIN_COMPILER_REGISTER - bc || resultStartReg < SimpleCompiler.MIN_COMPILER_REGISTER - bc) {
				throw new AssertionError();
			}
		}
		
	}
	
	public static Map<String, Func> ALL_INTERRUPTS = allInts();
	
	
	// GENERATED-CODE-START
	private static Map<String, Func> allInts() {
		Map<String, Func> res = new HashMap<>();
		new SimpleFuncType(List.of(), List.of());
		return Collections.unmodifiableMap(res);
	}
	// GENERATED-CODE-END
	
}
