package de.hechler.patrick.code.simple.interpreter.java;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import de.hechler.patrick.code.simple.interpreter.SimpleInterpreter;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.cmd.BlockCmd;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;

public class JavaDependency extends SimpleFile {
	
	public JavaDependency(String sourceFile) {
		super(sourceFile, sourceFile);
	}
	
	public void function(String name, Object invoker, Method method) {
		final Parameter[] parameters = method.getParameters();
		List<SimpleVariable> argsType = convert(parameters);
		List<SimpleVariable> resType;
		if ( method.getReturnType() == Void.TYPE ) {
			resType = List.of();
		} else {
			resType = List.of(new SimpleVariable(convert(method.getReturnType()), "result", null, 0));
		}
		FuncType ftype = FuncType.create(resType, argsType, FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT);
		function(name, ftype, (si, args) -> {
			Object[] arga = convert(args);
			if ( parameters.length > 0 && parameters[0].getType() == SimpleInterpreter.class ) {
				Object[] narga = new Object[arga.length + 1];
				narga[0] = si;
				System.arraycopy(arga, 0, narga, 1, arga.length);
				arga = narga;
			}
			try {
				if ( method.getReturnType() == Void.TYPE ) {
					method.invoke(invoker, arga);
					return List.of();
				}
				Object obj = method.invoke(invoker, arga);
				return List.of(convert(obj));
			} catch (Throwable t) {
				if ( t instanceof RuntimeException r ) {
					throw r;
				}
				if ( t instanceof Error e ) {
					throw e;
				}
				throw new IllegalStateException(t);
			}
		});
	}
	
	public void function(String name, Object invoker, MethodHandle method) {
		MethodType type = method.type();
		List<SimpleVariable> argsType = convert(type.parameterArray());
		List<SimpleVariable> resType;
		if ( type.returnType() == Void.TYPE ) {
			resType = List.of();
		} else {
			resType = List.of(new SimpleVariable(convert(type.returnType()), "result", null, 0));
		}
		FuncType ftype = FuncType.create(resType, argsType, FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT);
		function(name, ftype, (si, args) -> {
			Object[] arga = convert(args);
			if ( invoker == null ) {
				if ( type.parameterCount() > 0 && type.parameterType(0) == SimpleInterpreter.class ) {
					Object[] narga = new Object[arga.length + 1];
					narga[0] = si;
					System.arraycopy(arga, 0, narga, 1, arga.length);
					arga = narga;
				}
			} else if ( type.parameterCount() > 1 && type.parameterType(1) == SimpleInterpreter.class ) {
				Object[] narga = new Object[arga.length + 2];
				narga[0] = invoker;
				narga[1] = si;
				System.arraycopy(arga, 0, narga, 2, arga.length);
				arga = narga;
			} else {
				Object[] narga = new Object[arga.length + 1];
				narga[0] = invoker;
				System.arraycopy(arga, 0, narga, 1, arga.length);
				arga = narga;
			}
			try {
				if ( type.returnType() == Void.TYPE ) {
					method.invokeWithArguments(arga);
					return List.of();
				}
				Object obj = method.invokeWithArguments(arga);
				return List.of(convert(obj));
			} catch (Throwable t) {
				if ( t instanceof RuntimeException r ) {
					throw r;
				}
				if ( t instanceof Error e ) {
					throw e;
				}
				throw new IllegalStateException(t);
			}
		});
	}
	
	private static Object[] convert(List<ConstantValue> list) {
		return list.stream().map(c -> {
			switch ( (NativeType) c.type() ) {
			case NativeType.NUM:
				return Long.valueOf(( (ConstantValue.ScalarValue) c ).value());
			case NativeType.DWORD:
				return Integer.valueOf((int) ( (ConstantValue.ScalarValue) c ).value());
			case NativeType.WORD:
				return Short.valueOf((short) ( (ConstantValue.ScalarValue) c ).value());
			case NativeType.BYTE:
				return Byte.valueOf((byte) ( (ConstantValue.ScalarValue) c ).value());
			case NativeType.FPNUM:
				return Double.valueOf(( (ConstantValue.FPValue) c ).value());
			case NativeType.FPDWORD:
				return Float.valueOf((float) ( (ConstantValue.FPValue) c ).value());
			case NativeType.UWORD:
				return Character.valueOf((char) ( (ConstantValue.ScalarValue) c ).value());
			// $CASES-OMITTED$
			default:
				throw new IllegalArgumentException("can't convert " + c.type());
			}
		}).toArray();
	}
	
	private static List<SimpleVariable> convert(Class<?>[] types) {
		if ( types.length == 0 ) {
			return List.of();
		}
		final int off = types[0] == SimpleInterpreter.class ? 1 : 0;
		List<SimpleVariable> res = Arrays.asList(new SimpleVariable[types.length - off]);
		for (int i = off; i < types.length; i++) {
			res.set(i, new SimpleVariable(convert(types[off + i]), "arg" + i, null, 0));
		}
		return res;
	}
	
	private static List<SimpleVariable> convert(Parameter[] types) {
		if ( types.length == 0 ) {
			return List.of();
		}
		final int off = types[0].getType() == SimpleInterpreter.class ? 1 : 0;
		List<SimpleVariable> res = Arrays.asList(new SimpleVariable[types.length - off]);
		for (int i = off; i < types.length; i++) {
			String name;
			if ( types[off + i].isNamePresent() ) {
				name = types[off + i].getName();
			} else {
				name = "arg" + i;// do not count different when there is a initial interpreter arg
			}
			res.set(i, new SimpleVariable(convert(types[off + i].getType()), name, null, 0));
		}
		return res;
	}
	
	private static ConstantValue convert(Object obj) {
		switch ( obj ) {
		case Long l:
			return new ConstantValue.ScalarValue(NativeType.NUM, l.longValue());
		case Integer i:
			return new ConstantValue.ScalarValue(NativeType.DWORD, i.longValue());
		case Short s:
			return new ConstantValue.ScalarValue(NativeType.WORD, s.longValue());
		case Byte b:
			return new ConstantValue.ScalarValue(NativeType.BYTE, b.longValue());
		case Double d:
			return new ConstantValue.FPValue(NativeType.FPNUM, d.doubleValue());
		case Float f:
			return new ConstantValue.FPValue(NativeType.FPDWORD, f.doubleValue());
		case Character c:
			return new ConstantValue.ScalarValue(NativeType.UDWORD, c.charValue());
		default:
			throw new IllegalStateException("invalid result: " + obj.getClass().getName());
		}
	}
	
	private static SimpleType convert(Class<?> cls) {
		if ( cls.isPrimitive() ) {
			if ( cls == Long.TYPE ) {
				return NativeType.NUM;
			} else if ( cls == Integer.TYPE ) {
				return NativeType.DWORD;
			} else if ( cls == Short.TYPE ) {
				return NativeType.WORD;
			} else if ( cls == Byte.TYPE ) {
				return NativeType.BYTE;
			} else if ( cls == Double.TYPE ) {
				return NativeType.FPNUM;
			} else if ( cls == Float.TYPE ) {
				return NativeType.FPDWORD;
			} else if ( cls == Character.TYPE ) {
				return NativeType.UWORD;
			}
		} else if ( cls == Long.class ) {
			return NativeType.NUM;
		} else if ( cls == Integer.class ) {
			return NativeType.DWORD;
		} else if ( cls == Short.class ) {
			return NativeType.WORD;
		} else if ( cls == Byte.class ) {
			return NativeType.BYTE;
		} else if ( cls == Double.class ) {
			return NativeType.FPNUM;
		} else if ( cls == Float.class ) {
			return NativeType.FPDWORD;
		} else if ( cls == Character.class ) {
			return NativeType.UWORD;
		}
		throw new IllegalArgumentException("invalid type: " + cls.getName());
	}
	
	public void function(String name, FuncType type, JavaFunction func) {
		BlockCmd blk = new BlockCmd(SimpleScope.newFuncScope(this, type, ErrorContext.NO_CONTEXT));
		SimpleFunction sf = new SimpleFunction(this, name, type, blk);
		super.function(sf, ErrorContext.NO_CONTEXT);
		blk.addCmd(new JavaCommand(blk.currentAsParent(), func));
		blk.seal();
	}
	
}
