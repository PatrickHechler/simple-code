package de.hechler.patrick.codesprachen.simple.interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.ToLongBiFunction;

import de.hechler.patrick.codesprachen.simple.interpreter.fs.FSManager;
import de.hechler.patrick.codesprachen.simple.interpreter.fs.FSManagerImpl;
import de.hechler.patrick.codesprachen.simple.interpreter.java.ConstantValue;
import de.hechler.patrick.codesprachen.simple.interpreter.java.JavaCommand;
import de.hechler.patrick.codesprachen.simple.interpreter.java.JavaStdLib;
import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManager;
import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManagerImpl;
import de.hechler.patrick.codesprachen.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.BlockCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.StructType;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.AddressOfVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.BinaryOpVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.CastVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.CondVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.DataVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.NameVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;

public class SimpleInterpreter {
	
	private final Path[]                                        src;
	private final ToLongBiFunction<SimpleInterpreter, String[]> allocArgs;
	private final Path                                          defMain;
	private final MemoryManager                                 mm;
	private final FSManager                                     fsm;
	private final SimpleDependency                              stdlib;
	private final Map<Path, LoadedSF>                           files  = new HashMap<>();
	private final Map<SimpleFile, LoadedSF>                     loaded = new HashMap<>();
	
	public SimpleInterpreter(List<Path> src) {
		this(src, null, JavaStdLib::allocArgs);
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter, String[]> allocArgs) {
		this(src, stdlib, allocArgs, null, src.get(0));
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter, String[]> allocArgs, Path defMain, Path root) {
		this(src, stdlib, allocArgs, defMain, new MemoryManagerImpl(), root);
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter, String[]> allocArgs, Path defMain, MemoryManager memManager, Path root) {
		this(src, stdlib, allocArgs, defMain, memManager, new FSManagerImpl(root, memManager));
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter, String[]> allocArgs, Path defMain, MemoryManager memManager,
		FSManager fsManager) {
		this.src = src.toArray(new Path[src.size()]);
		this.allocArgs = allocArgs;
		this.defMain = defMain;
		this.mm = memManager;
		this.fsm = fsManager;
		this.stdlib = stdlib == null ? new JavaStdLib(this) : stdlib;
	}
	
	protected LoadedSF load(String p0, String rel0) {
		FileSystem fs = this.src[0].getFileSystem();
		Path p = fs.getPath(p0);
		if ( rel0 == null ) {
			return load(p, null);
		}
		Path rel = fs.getPath(rel0);
		return load(p, rel);
	}
	
	protected LoadedSF load(Path p, Path rel) {
		final Path origP = p;
		if ( !p.isAbsolute() ) {
			if ( rel != null ) {
				LoadedSF lsf = tryLoad(rel.resolve(p), p);
				if ( lsf != null ) return lsf;
			}
		} else {
			p = p.relativize(p.getRoot());
		}
		LoadedSF lsf = this.files.get(p);
		if ( lsf != null ) return lsf;
		lsf = this.files.get(p);
		if ( lsf != null ) return lsf;
		for (Path path : this.src) {
			p = path.resolve(p);
			if ( !Files.exists(p) ) {
				continue;
			}
			lsf = tryLoad(p, origP);
			if ( lsf != null ) return lsf;
		}
		throw new IllegalArgumentException("no file with the path: " + p + " found");
	}
	
	private LoadedSF tryLoad(Path p, Path oldP) {
		LoadedSF lsf;
		try {
			Path realP = p.toRealPath();
			Path p0 = realP;
			try (InputStream in = Files.newInputStream(p0)) {
				SimpleSourceFileParser parser =
					new SimpleSourceFileParser(in, oldP.toString(), (srcPath, runtimePath) -> {
						if ( runtimePath == null ) {
							if ( srcPath.endsWith(".sexp") ) {
								runtimePath = srcPath.substring(0, srcPath.length() - ".sexp".length());
							} else {
								runtimePath = srcPath;
							}
						}
						runtimePath += ".ssf"; // NOSONAR
						Path srcFile = realP.getFileSystem().getPath(runtimePath);
						Path relDir = realP.getParent();
						return load(srcFile, relDir).sf;
					});
				SimpleFile sf = new SimpleFile(oldP.toString());
				lsf = new LoadedSF(sf);
				sf.dependency(this.stdlib, "std", ErrorContext.NO_CONTEXT);
				parser.parse(sf);
				initilize(lsf);
				this.files.put(oldP, lsf);
				this.loaded.put(sf, lsf);
				return lsf;
			}
		} catch (@SuppressWarnings("unused") IOException e) {
		}
		return null;
	}
	
	private void initilize(LoadedSF lsf) {
		SimpleFile sf = lsf.sf;
		Collection<SimpleVariable> av = sf.allVariables();
		for (SimpleVariable sv : av) {
			SimpleValue iv = sv.initialValue();
			if ( iv != null ) {
				lsf.value(this, sv.name(), calculate(lsf, iv));
			}
		}
		if ( sf.init() != null ) {
			exec(lsf, sf.init(), new ArrayList<>());
		}
	}
	
	public FSManager fsManager() {
		return this.fsm;
	}
	
	public MemoryManager memManager() {
		return this.mm;
	}
	
	public SimpleDependency stdlib() {
		return this.stdlib;
	}
	
	public int execute(Path main, String[] args) {
		if ( main == null ) {
			main = this.defMain;
			if ( main == null ) {
				throw new IllegalStateException("no main file set");
			}
		}
		SimpleFile mainFile = load(main, null).sf;
		SimpleFunction func = Objects.requireNonNull(mainFile.main(), "the main file has no main function");
		long addr = this.allocArgs.applyAsLong(this, args);
		List<ConstantValue> list = List.of(new ConstantValue.ScalarValue(NativeType.UNUM, args.length),
			new ConstantValue.ScalarValue(FuncType.MAIN_TYPE.argMembers().get(1).type(), addr));
		return (int) ( (ConstantValue.ScalarValue) execute(mainFile, func, list).get(0) ).value();
	}
	
	private void exec(LoadedSF lsf, SimpleFunction func, List<ConstantValue> list) {
		// TODO
		throw new UnsupportedOperationException("exec");
	}
	
	public List<ConstantValue> execute(SimpleFile sf, String funcName, List<ConstantValue> args) {
		SimpleFunction func = sf.function(funcName);
		FuncType t = func.type();
		checkType(t.argMembers(), args, false);
		return execute(sf, func, args);
	}
	
	private List<ConstantValue> execute(SimpleFile sf, SimpleFunction func, List<ConstantValue> args) {
		FuncType t = func.type();
		BlockCmd blk = func.block();
		int ccnt = blk.commandCount();
		if ( ccnt <= 0 ) {
			if ( ccnt == 0 ) {
				return List.of();
			}
			if ( blk.command(0) instanceof JavaCommand jc ) {
				List<ConstantValue> result = jc.func.execute(this, args);
				checkType(t.resMembers(), result, false);
				return result;
			}
		}
		List<ConstantValue> list = new ArrayList<>(args);
		exec(load(sf.binaryTarget, null), func, list);
		return list;
	}
	
	private static void checkType(List<SimpleVariable> members, List<ConstantValue> list, boolean result) {
		int s = members.size();
		if ( s != list.size() ) {
			throw new IllegalArgumentException(resOrArg(result) + " list has an illegal length");
		}
		for (int i = s; --i >= 0;) {
			if ( !members.get(i).type().equals(list.get(i).type()) ) {
				throw new IllegalArgumentException("the " + resOrArg(result) + " " + i + " has an illegal type");
			}
		}
	}
	
	private static String resOrArg(boolean result) {
		return result ? "result" : "argument";
	}
	
	private ConstantValue calculate(ValueScope scope, SimpleValue val) {// NOSONAR
		switch ( val ) {
		case AddressOfVal ao:// NOSONAR
			throw new UnsupportedOperationException("not yet done");
		case BinaryOpVal bo:
			switch ( bo.a().type() ) {// NOSONAR
			case NativeType.FPNUM, NativeType.FPDWORD:
				switch ( bo.op() ) {
				case CMP_NEQ: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return ( ca != cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_EQ: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return ( ca == cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_GE: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return ( ca >= cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_GT: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return ( ca > cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_LE: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return ( ca <= cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_LT: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return ( ca < cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_NEQ: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return !( ca == cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO; // NOSONAR
				}
				case CMP_NAN_EQ: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return !( ca != cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO; // NOSONAR
				}
				case CMP_NAN_GE: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return !( ca < cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO; // NOSONAR
				}
				case CMP_NAN_GT: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return !( ca <= cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO; // NOSONAR
				}
				case CMP_NAN_LE: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return !( ca > cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO; // NOSONAR
				}
				case CMP_NAN_LT: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return !( ca >= cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO; // NOSONAR
				}
				case MATH_ADD: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.FPValue(bo.type(), ca + cb);
				}
				case MATH_DIV: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.FPValue(bo.type(), ca / cb);
				}
				case MATH_MOD: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.FPValue(bo.type(), ca % cb);
				}
				case MATH_MUL: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.FPValue(bo.type(), ca * cb);
				}
				case MATH_SUB: {
					double ca = ( (ConstantValue.FPValue) calculate(scope, bo.a()) ).value();
					double cb = ( (ConstantValue.FPValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.FPValue(bo.type(), ca - cb);
				}
				// $CASES-OMITTED$
				default:
					throw new AssertionError("unknown/invalid binary operator: " + bo.op().name());
				}
			default:
				switch ( bo.op() ) {
				case ARR_PNTR_INDEX: {
					ConstantValue ca = calculate(scope, bo.a());
					ConstantValue cb = calculate(scope, bo.b());
					long off = ( (ConstantValue.ScalarValue) cb ).value();
					if ( ca.type().size() != 1L ) {
						off *= ca.type().size();
					}
					if ( ca.type() instanceof ArrayType ) {
						return deref(bo.type(), ( (ConstantValue.DataValue) ca ).address() + off);
					}
					return deref(bo.type(), ( (ConstantValue.ScalarValue) ca ).value() + off);
				}
				case DEREF_BY_NAME: {
					ConstantValue ca = calculate(scope, bo.a());
					String name = ( (NameVal) bo.b() ).name();
					long off = switch ( bo.a().type() ) {
					case FuncType ft -> ft.offset(name);
					case StructType st -> st.offset(name);
					default -> throw new AssertionError("Unexpected value: " + bo.a().type());
					};
					if ( ca.type().size() != 1L ) {
						off *= ca.type().size();
					}
					return deref(bo.type(), ( (ConstantValue.DataValue) ca ).address() + off);
				}
				case BIT_AND: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca & cb);
				}
				case BIT_OR: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca | cb);
				}
				case BIT_XOR: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca ^ cb);
				}
				case BOOL_AND: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					if ( ca == 0L ) return ConstantValue.ScalarValue.ZERO;
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( cb != 0L ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case BOOL_OR: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					if ( ca != 0L ) return ConstantValue.ScalarValue.ONE;
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( cb != 0L ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_NEQ, CMP_NEQ: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( ca != cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_EQ, CMP_EQ: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( ca == cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_GE, CMP_GE: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( ca >= cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_GT, CMP_GT: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( ca > cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_LE, CMP_LE: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( ca <= cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case CMP_NAN_LT, CMP_LT: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return ( ca < cb ) ? ConstantValue.ScalarValue.ONE : ConstantValue.ScalarValue.ZERO;
				}
				case MATH_ADD: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca + cb);
				}
				case MATH_DIV: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca / cb);
				}
				case MATH_MOD: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca % cb);
				}
				case MATH_MUL: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca * cb);
				}
				case MATH_SUB: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca - cb);
				}
				case SHIFT_LEFT: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca << cb);
				}
				case SHIFT_LOGIC_RIGTH: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca >>> cb);
				}
				case SHIFT_ARITMETIC_RIGTH: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					return new ConstantValue.ScalarValue(bo.type(), ca >> cb);
				}
				default:
					throw new AssertionError("unknown binary operator: " + bo.op().name());
				}
			}
		case CastVal c: { // NOSONAR
			ConstantValue v = calculate(scope, c.value());
			switch ( v ) {
			case ConstantValue.ScalarValue s:
				switch ( c.type() ) {
				case NativeType.FPNUM, NativeType.FPDWORD:
					return new ConstantValue.FPValue(c.type(), s.value());
				case NativeType n:
					return new ConstantValue.ScalarValue(n, s.value());
				case PointerType p:
					return new ConstantValue.ScalarValue(p, s.value());
				case FuncType f:
					return new ConstantValue.ScalarValue(f, s.value());
				default:
					throw new AssertionError("unknown/illegal cast type: " + c.type());
				}
			case ConstantValue.DataValue d:
				return new ConstantValue.DataValue(c.type(), d.address());
			case ConstantValue.FPValue f:
				switch ( c.type() ) {
				case NativeType.FPNUM, NativeType.FPDWORD:
					return new ConstantValue.FPValue(c.type(), f.value());
				case NativeType n:
					return new ConstantValue.ScalarValue(n, (long) f.value());
				default:
					throw new AssertionError("unknown/illegal cast type: " + c.type());
				}
			}
		}
		case CondVal c: {
			ConstantValue v = calculate(scope, c.condition());
			if ( v instanceof ConstantValue.ScalarValue s ? s.value() != 0L
				: ( (ConstantValue.DataValue) v ).address() != 0L ) {
				return calculate(scope, c.trueValue());
			}
			return calculate(scope, c.falseValue());
		}
		case DataVal d:
			return scope.value(this, d);
		default:
			throw new AssertionError("unknown value class: " + val.getClass());
		}
	}
	
	private ConstantValue deref(SimpleType type, long address) {
		MemoryManager mem = this.mm;
		switch ( type ) {
		case NativeType.FPNUM: {
			long val = mem.get64(address);
			return new ConstantValue.FPValue(type, Double.longBitsToDouble(val));
		}
		case NativeType.FPDWORD: {
			int val = mem.get32(address);
			return new ConstantValue.FPValue(type, Float.intBitsToFloat(val));
		}
		case NativeType n when n.size() == 8: {
			long val = mem.get64(address);
			return new ConstantValue.ScalarValue(n, val);
		}
		case NativeType n when n.size() == 4: {
			long val = mem.get32(address);
			return new ConstantValue.ScalarValue(n, val);
		}
		case NativeType n when n.size() == 2: {
			long val = mem.get16(address);
			return new ConstantValue.ScalarValue(n, val);
		}
		case NativeType n when n.size() == 1: {
			long val = mem.get8(address);
			return new ConstantValue.ScalarValue(n, val);
		}
		case PointerType p: {
			long val = mem.get64(address);
			return new ConstantValue.ScalarValue(p, val);
		}
		case FuncType f when ( f.flags() & FuncType.FLAG_FUNC_ADDRESS ) != 0: {
			long val = mem.get64(address);
			return new ConstantValue.ScalarValue(f, val);
		}
		case FuncType f: {
			return new ConstantValue.DataValue(f, address);
		}
		case StructType s: {
			return new ConstantValue.DataValue(s, address);
		}
		case ArrayType a: {
			return new ConstantValue.DataValue(a, address);
		}
		default:
			throw new AssertionError("unknown type class: " + type.getClass());
		}
	}
	
	private void put(long address, ConstantValue value) {
		MemoryManager mem = this.mm;
		if ( value instanceof ConstantValue.DataValue d ) {
			mem.copy(d.address(), address, d.type().size());
			return;
		}
		if ( value instanceof ConstantValue.ScalarValue s ) {
			long val = s.value();
			switch ( (int) s.type().size() ) {
			case 1:
				mem.set8(address, (int) val);
				return;
			case 2:
				mem.set16(address, (int) val);
				return;
			case 4:
				mem.set32(address, (int) val);
				return;
			case 8:
				mem.set64(address, val);
				return;
			default:
				throw new AssertionError(s.type());
			}
		} else if ( value.type() == NativeType.FPNUM ) {
			long val = Double.doubleToRawLongBits(( (ConstantValue.FPValue) value ).value());
			mem.set64(address, val);
		} else {
			int val = Float.floatToRawIntBits((float) ( (ConstantValue.FPValue) value ).value());
			mem.set32(address, val);
		}
	}
	
	private long rwaddr;
	private long roaddr;
	
	private record LoadedSF(SimpleFile sf, Map<String, LoadedValue> addrs, Map<byte[], Long> rodata)
		implements ValueScope {
		
		record LoadedValue(SimpleType type, long address) {}
		
		public LoadedSF(SimpleFile sf) {
			this(sf, new HashMap<>(), new TreeMap<>((a, b) -> {
				final int len = a.length;
				if ( len != b.length ) {
					return Integer.compare(a.length, b.length);
				}
				if ( a == b ) return 0;
				for (int i = len; --i >= 0;) {
					if ( a[i] != b[i] ) {
						return Byte.compare(a[i], b[i]);
					}
				}
				return 0;
			}));
		}
		
		@Override
		public ConstantValue value(SimpleInterpreter si, String name) {
			LoadedValue res = this.addrs.get(name);
			if ( res == null ) {
				res = allocValue(si, name);
			}
			return si.deref(res.type, res.address);
		}
		
		@Override
		public long addressOf(SimpleInterpreter si, String name) {
			LoadedValue res = this.addrs.get(name);
			if ( res == null ) {
				res = allocValue(si, name);
			}
			return res.address;
		}
		
		@Override
		public void value(SimpleInterpreter si, String name, ConstantValue value) {
			LoadedValue res = this.addrs.get(name);
			if ( res == null ) {
				res = allocValue(si, name);
			}
			si.put(res.address, value);
		}
		
		private LoadedValue allocValue(SimpleInterpreter si, String name) {
			LoadedValue res;
			SimpleVariable sv = this.sf.variable(name);
			if ( sv == null ) {
				throw new IllegalArgumentException(noVal(name));
			}
			long rwaddr = allocData(si, sv.type().align(), sv.type().size(), MemoryManager.FLAG_READ_ONLY, si.rwaddr);
			si.rwaddr = rwaddr + sv.type().size();
			res = new LoadedValue(sv.type(), rwaddr);
			this.addrs.put(name, res);
			return res;
		}
		
		private static String noVal(String name) {
			return "no value with name " + name + " found";
		}
		
		@Override
		public ConstantValue value(SimpleInterpreter si, DataVal data) {
			DataVal orig = data.orig() == null ? data : data.orig();
			byte[] bytes = orig.value();
			Long addr = this.rodata.get(bytes);
			if ( addr != null ) {
				return new ConstantValue.DataValue(data.type(), addr.longValue() + data.off());
			}
			long roaddr = allocData(si, data.type().align(), bytes.length, MemoryManager.FLAG_READ_ONLY, si.roaddr);
			si.roaddr = roaddr + bytes.length;
			MemoryManager mem = si.mm;
			addr = Long.valueOf(roaddr);
			this.rodata.put(bytes, addr);
			ByteBuffer bbuf = ByteBuffer.wrap(bytes);
			mem._privilegedSetRO(roaddr, bbuf);
			return new ConstantValue.DataValue(data.type(), roaddr + data.off());
		}
		
		private static long allocData(SimpleInterpreter si, int align, long length, int flags, long roaddr) {
			MemoryManager mem = si.mm;
			final long pageSize = mem.pageSize();
			final long mask = Math.min(mem.pageSize(), align) - 1L;
			if ( ( roaddr & mask ) != 0 ) {
				roaddr = ( roaddr & ~mask ) + align;
			}
			if ( pageSize >= ( roaddr & ( pageSize - 1L ) ) + length ) {
				roaddr = mem.allocate(length, 0L, flags);
			}
			return roaddr;
		}
		
	}
	
	interface ValueScope {
		
		ConstantValue value(SimpleInterpreter si, String name);
		
		ConstantValue value(SimpleInterpreter si, DataVal data);
		
		void value(SimpleInterpreter si, String name, ConstantValue value);
		
		long addressOf(SimpleInterpreter si, String name);
		
	}
	
}
