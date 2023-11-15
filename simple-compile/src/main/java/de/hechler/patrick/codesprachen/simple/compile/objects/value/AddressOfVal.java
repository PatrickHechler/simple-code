package de.hechler.patrick.codesprachen.simple.compile.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;

public record AddressOfVal(SimpleValue a, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	private static final String NO_RUNTIME_ADDRESS =
		"can not calculate the runtime address of a numeric constant or address of value: ";
	
	public static SimpleValue create(SimpleValue a, ErrorContext ctx) {
		isConst(a, ctx);
		return new AddressOfVal(a, PointerType.create(a.type(), ctx), ctx);
	}
	
	@Override
	public boolean isConstant() {
		return isConst(this.a, this.ctx);
	}
	
	private static boolean isConst(SimpleValue val, ErrorContext ctx) {
		return switch ( val ) {
		case CastVal a -> isConst(a.value(), ctx);
		case DataVal _a -> true; // thats a lie
		case FunctionVal _a -> true; // here comes another lie
		case VariableVal _a -> true; // no more lies, I hope
		case CondVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case BinaryOpVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case AddressOfVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case ScalarNumericVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case FPNumericVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case DependencyVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case NameVal _a ->
			throw new AssertionError("NameVal is only allowed to be used in BinaryOperator :[NAME] things");
		default -> throw new AssertionError("unknown SimpleValue type: " + val.getClass().getName());
		};
	}
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public SimpleValue simplify(@SuppressWarnings("unused") UnaryOperator<SimpleValue> op) { return this; }
	
	@Override
	public String toString() {
		return "( &" + this.a + " )";
	}
	
}
