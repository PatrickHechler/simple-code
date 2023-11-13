package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record AddressOfVal(SimpleValue a, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleValue a, ErrorContext ctx) {
		isConst(a, ctx);
		return new AddressOfVal(a, PointerType.create(a.type(), ctx), ctx);
	}
	
	@Override
	public boolean isConstant() {
		return isConst(a, ctx);
	}
	
	private static boolean isConst(SimpleValue val, ErrorContext ctx) {
		return switch ( val ) {
		case BinaryOpVal a -> throw new CompileError(ctx,
			"can not calculate the runtime address of a numeric constant or address of value: " + val);
		case CastVal a -> isConst(a.value(), ctx);
		case CondVal a -> throw new CompileError(ctx,
			"can not calculate the runtime address of a numeric constant or address of value: " + val);
		case DataVal _a -> true; // thats a lie
		case FunctionVal _a -> true; // here comes another lie
		case VariableVal _a -> true; // no more lies, I hope
		case AddressOfVal _a -> throw new CompileError(ctx,
			"can not calculate the runtime address of a numeric constant or address of value: " + val);
		case ScalarNumericVal _a -> throw new CompileError(ctx,
			"can not calculate the runtime address of a numeric constant or address of value: " + val);
		case FPNumericVal _a -> throw new CompileError(ctx,
			"can not calculate the runtime address of a numeric constant or address of value: " + val);
		case DependencyVal _a -> throw new CompileError(ctx,
			"can not calculate the runtime address of a numeric constant or address of value: " + val);
		case NameVal _a ->
			throw new AssertionError("NameVal is only allowed to be used in BinaryOperator :[NAME] things");
		default -> throw new AssertionError("unknown SimpleValue type: " + val.getClass().getName());
		};
	}
	
	@Override
	public SimpleValue simplify() {
		return this;
	}
	
	@Override
	public String toString() {
		return "( &" + this.a + " )";
	}
	
}
