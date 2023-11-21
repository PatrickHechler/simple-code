package de.hechler.patrick.codesprachen.simple.interpreter;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongBiFunction;

import de.hechler.patrick.codesprachen.simple.interpreter.SimpleInterpreter.LoadedSF.LoadedValue;
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
	
	protected LoadedSF load(Path p, Path rel) throws IOException {
		if ( !p.isAbsolute() ) {
			if ( rel == null ) p = p.relativize(p.getRoot());
			else p = p.resolve(rel);
		}
		Path oldP = p;
		LoadedSF lsf = this.files.get(p);
		if ( lsf != null ) return lsf;
		lsf = this.files.get(p);
		if ( lsf != null ) return lsf;
		for (Path path : this.src) {
			p = path.resolve(p);
			if ( !Files.exists(p) ) {
				continue;
			}
			final Path realP = p.toRealPath();
			p = realP;
			try (InputStream in = Files.newInputStream(p)) {
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
						try {
							Path srcFile = realP.getFileSystem().getPath(runtimePath);
							Path relDir = realP.getParent();
							return load(srcFile, relDir).sf;
						} catch (IOException e) {
							throw new IOError(e);
						}
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
		}
		throw new FileNotFoundException(p.toString());
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
	
	public int execute(Path main, String[] args) throws IOException {
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
		return (int) ( (ConstantValue.ScalarValue) execute(func, list).get(0) ).value();
	}
	
	private void exec(LoadedSF lsf, SimpleFunction func, List<ConstantValue> list) {
		
	}
	
	public List<ConstantValue> execute(SimpleFile sf, String funcName, List<ConstantValue> args) {
		SimpleFunction func = sf.function(funcName);
		FuncType t = func.type();
		checkType(t.argMembers(), args, false);
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
	
	private ConstantValue calculate(ValueScope scope, SimpleValue val) {
		switch ( val ) {
		case AddressOfVal ao:
			throw new UnsupportedOperationException("not yet done");
		case BinaryOpVal bo:
			switch ( bo.op() ) {
			case ARR_PNTR_INDEX: {
				ConstantValue ca = calculate(scope, bo.a());
				ConstantValue cb = calculate(scope, bo.b());
				long off = ( (ConstantValue.ScalarValue) cb ).value();
				if ( ca.type().size() != 1L ) {
					off *= ca.type().size();
				}
				switch ( ca.type() ) {
				case ArrayType at:// NOSONAR
					return deref(bo.type(), ( (ConstantValue.DataValue) ca ).address() + off);
				case PointerType pt:// NOSONAR
					return new ConstantValue.ScalarValue(bo.type(), ( (ConstantValue.ScalarValue) ca ).value() + off);
				default:
					throw new AssertionError("illegal array/pointer value");
				}
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
				if ( bo.type() instanceof ArrayType ) {
					return new ConstantValue.DataValue(bo.type(), ( (ConstantValue.DataValue) ca ).address() + off);
				}
				return new ConstantValue.ScalarValue(bo.type(), ( (ConstantValue.ScalarValue) ca ).value() + off);
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
				if ( ca == 0L ) return new ConstantValue.ScalarValue(bo.type(), 0L);
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( cb != 0L ) ? 1L : 0L);
			}
			case BOOL_OR: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				if ( ca != 0L ) return new ConstantValue.ScalarValue(bo.type(), 1L);
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( cb != 0L ) ? 1L : 0L);
			}
			case CMP_NAN_NEQ, CMP_NEQ: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( ca != cb ) ? 1L : 0L);
			}
			case CMP_NAN_EQ, CMP_EQ: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( ca == cb ) ? 1L : 0L);
			}
			case CMP_NAN_GE, CMP_GE: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( ca >= cb ) ? 1L : 0L);
			}
			case CMP_NAN_GT, CMP_GT: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( ca > cb ) ? 1L : 0L);
			}
			case CMP_NAN_LE, CMP_LE: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( ca <= cb ) ? 1L : 0L);
			}
			case CMP_NAN_LT, CMP_LT: {
				long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
				long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
				return new ConstantValue.ScalarValue(bo.type(), ( ca < cb ) ? 1L : 0L);
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
	}
	
	public ConstantValue deref(SimpleType type, long address) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void put(long address, ConstantValue value) {
		// TODO Auto-generated method stub
	}
	
	record LoadedSF(SimpleFile sf, Map<String, LoadedValue> addrs) implements ValueScope {
		
		record LoadedValue(SimpleType type, long address) {}
		
		public LoadedSF(SimpleFile sf) {
			this(sf, new HashMap<>());
		}
		
		@Override
		public ConstantValue value(SimpleInterpreter si, String name) {
			LoadedValue res = this.addrs.get(name);
			if ( res == null ) {
				throw new IllegalArgumentException(noVal(name));
			}
			return si.deref(res.type, res.address);
		}
		
		@Override
		public void value(SimpleInterpreter si, String name, ConstantValue value) {
			LoadedValue res = this.addrs.get(name);
			if ( res == null ) {
				throw new IllegalArgumentException(noVal(name));
			}
			si.put(res.address, value);
		}
		
		@Override
		public long addressOf(@SuppressWarnings("unused") SimpleInterpreter si, String name) {
			LoadedValue res = this.addrs.get(name);
			if ( res == null ) {
				throw new IllegalArgumentException(noVal(name));
			}
			return res.address;
		}
		
		private static String noVal(String name) {
			return "no value with name " + name + " found";
		}
		
	}
	
	interface ValueScope {
		
		ConstantValue value(SimpleInterpreter si, String name);
		
		void value(SimpleInterpreter si, String name, ConstantValue value);
		
		long addressOf(SimpleInterpreter si, String name);
		
	}
	
}
