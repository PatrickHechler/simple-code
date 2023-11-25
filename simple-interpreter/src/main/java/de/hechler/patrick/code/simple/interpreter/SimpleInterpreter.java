package de.hechler.patrick.code.simple.interpreter;

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

import de.hechler.patrick.code.simple.interpreter.fs.FSManager;
import de.hechler.patrick.code.simple.interpreter.fs.FSManagerImpl;
import de.hechler.patrick.code.simple.interpreter.java.ConstantValue;
import de.hechler.patrick.code.simple.interpreter.java.JavaCommand;
import de.hechler.patrick.code.simple.interpreter.java.JavaStdLib;
import de.hechler.patrick.code.simple.interpreter.java.ConstantValue.DataValue;
import de.hechler.patrick.code.simple.interpreter.memory.MemoryManager;
import de.hechler.patrick.code.simple.interpreter.memory.MemoryManagerImpl;
import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.cmd.AssignCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.BlockCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.FuncCallCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.IfCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.SimpleCommand;
import de.hechler.patrick.code.simple.parser.objects.cmd.StructFuncCallCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.VarDeclCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.WhileCmd;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;
import de.hechler.patrick.code.simple.parser.objects.value.AddressOfVal;
import de.hechler.patrick.code.simple.parser.objects.value.BinaryOpVal;
import de.hechler.patrick.code.simple.parser.objects.value.CastVal;
import de.hechler.patrick.code.simple.parser.objects.value.CondVal;
import de.hechler.patrick.code.simple.parser.objects.value.DataVal;
import de.hechler.patrick.code.simple.parser.objects.value.FPNumericVal;
import de.hechler.patrick.code.simple.parser.objects.value.FunctionVal;
import de.hechler.patrick.code.simple.parser.objects.value.NameVal;
import de.hechler.patrick.code.simple.parser.objects.value.ScalarNumericVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;
import de.hechler.patrick.code.simple.parser.objects.value.BinaryOpVal.BinaryOp;

public class SimpleInterpreter {
	
	private long roaddr;
	private long stack;
	
	private final Path[]                   src;
	private final Path                     defMain;
	private final MemoryManager            mm;
	private final FSManager                fsm;
	private final SimpleFile               stdlib;
	private final Map<Path,LoadedSF>       files  = new HashMap<>();
	private final Map<SimpleFile,LoadedSF> loaded = new HashMap<>();
	
	public SimpleInterpreter(List<Path> src) {
		this(src, null);
	}
	
	public SimpleInterpreter(List<Path> src, SimpleFile stdlib) {
		this(src, stdlib, null, src.get(0));
	}
	
	public SimpleInterpreter(List<Path> src, SimpleFile stdlib, Path defMain, Path root) {
		this(src, stdlib, defMain, new MemoryManagerImpl(), root);
	}
	
	public SimpleInterpreter(List<Path> src, SimpleFile stdlib, Path defMain, MemoryManager memManager, Path root) {
		this(src, stdlib, defMain, memManager, new FSManagerImpl(root, memManager));
	}
	
	public SimpleInterpreter(List<Path> src, SimpleFile stdlib, Path defMain, MemoryManager memManager,
		FSManager fsManager) {
		this.src = src.toArray(new Path[src.size()]);
		this.defMain = defMain;
		this.mm = memManager;
		this.fsm = fsManager;
		this.stdlib = stdlib == null ? new JavaStdLib(this) : stdlib;
		final long pageSize = memManager.pageSize();
		this.stack = pageSize
			+ memManager.allocate(1L, 0x7FFFFFFFFFFFFFFFL & ~( pageSize - 1 ), MemoryManager.FLAG_GROW_DOWN);
		putWithDependencies(this.stdlib);
	}
	
	private void putWithDependencies(SimpleFile putSf) {
		this.loaded.put(putSf, new LoadedSF(putSf));
		for (SimpleDependency sd : putSf.dependencies()) {
			putWithDependencies((SimpleFile) sd);
		}
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
			p = p.getRoot().relativize(p);
		}
		LoadedSF lsf = this.files.get(p);
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
			try ( InputStream in = Files.newInputStream(p0) ) {
				SimpleSourceFileParser parser = new SimpleSourceFileParser(in, oldP.toString(),
					(srcPath, runtimePath) -> {
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
		} catch ( @SuppressWarnings("unused") IOException e ) {}
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
	
	public SimpleFile stdlib() {
		return this.stdlib;
	}
	
	public int execute(Path main, String[] args) {
		if ( main == null ) {
			main = this.defMain;
			if ( main == null ) {
				throw new IllegalStateException("no main file set");
			}
		}
		try {
			SimpleFile mainFile = load(main, null).sf;
			SimpleFunction func = Objects.requireNonNull(mainFile.main(), "the main file has no main function");
			long addr = JavaStdLib.allocArgs(this, args);
			List<ConstantValue> list = List.of(new ConstantValue.ScalarValue(NativeType.UNUM, args.length),
				new ConstantValue.ScalarValue(FuncType.MAIN_TYPE.argMembers().get(1).type(), addr));
			List<ConstantValue> resultList = execute(mainFile, func, list);
			ConstantValue.ScalarValue exitNum = (ConstantValue.ScalarValue) resultList.get(0);
			return (int) exitNum.value();
		} catch ( ExitError e ) {
			return e.exitnum;
		}
	}
	
	public static class ExitError extends RuntimeException {
		
		private static final long serialVersionUID = -8419388530355564771L;
		
		public final int exitnum;
		
		public ExitError(String message, Throwable cause, int exitnum) {
			super(message, cause);
			this.exitnum = exitnum;
		}
		
		public ExitError(String message, int exitnum) {
			super(message);
			this.exitnum = exitnum;
		}
		
		public ExitError(Throwable cause, int exitnum) {
			super(cause);
			this.exitnum = exitnum;
		}
		
		public ExitError(int exitnum) {
			super();
			this.exitnum = exitnum;
		}
		
	}
	
	private void exec(LoadedSF lsf, SimpleFunction func, List<ConstantValue> list) {
		final long origStack = this.stack;
		SubScope scope = new SubScope(this, lsf, func.type().argMembers(), func.type().resMembers());
		List<SimpleVariable> am = func.type().argMembers();
		if ( am.size() != list.size() ) {
			throw new IllegalArgumentException("illegal argument count");
		}
		allocVars(scope, am);
		allocVars(scope, func.type().resMembers());
		for (int i = 0; i < am.size(); i++) {
			SimpleVariable argVar = am.get(i);
			scope.value(this, argVar.name(), adjust(argVar.type(), list.get(i)));
		}
		exec(scope, func.block(), list);
		list.clear();
		for (SimpleVariable sv : func.type().resMembers()) {
			list.add(scope.value(this, sv.name()));
		}
		this.stack = origStack;
	}
	
	private void allocVars(SubScope scope, List<SimpleVariable> list) {
		for (int i = 0; i < list.size(); i++) {
			SimpleVariable sv = list.get(i);
			long addr = allocStackData(sv.type());
			ConstantValue.DataValue addrValue = new ConstantValue.DataValue(sv.type(), addr);
			scope.values.put(sv.name(), addrValue);
		}
	}
	
	private void exec(SubScope scope, SimpleCommand cmd, List<ConstantValue> list) {
		switch ( cmd ) {
		case StructFuncCallCmd sfc: {
			long addr = ( (ConstantValue.ScalarValue) calculate(scope, sfc.func) ).value();
			LoadedFunction lf = ofAddr(addr);
			list.clear();
			ConstantValue.DataValue fstruct = (DataValue) calculate(scope, sfc.fstruct);
			for (SimpleVariable av : lf.func.type().argMembers()) {
				long off = lf.func.type().offset(av.name());
				list.add(new ConstantValue.DataValue(av.type(), fstruct.address() + off));
			}
			if ( lf.func.block() == null ) {
				throw new IllegalStateException("the function " + lf.func + " could not be resolved!");
			}
			if ( lf.func.block().commandCount() == 1 && lf.func.block().command(0) instanceof JavaCommand jc ) {
				list = jc.func.execute(this, list);
			} else {
				SubScope sub = new SubScope(this, lf.file, lf.func.type().argMembers(), lf.func.type().resMembers());
				exec(sub, lf.func.block(), list);
			}
			List<SimpleVariable> rm = lf.func.type().resMembers();
			for (int i = 0; i < list.size(); i++) {
				long off = lf.func.type().offset(rm.get(i).name());
				put(fstruct.address() + off, list.get(i));
			}
			break;
		}
		case FuncCallCmd fc: {
			long addr = ( (ConstantValue.ScalarValue) calculate(scope, fc.func) ).value();
			LoadedFunction lf = ofAddr(addr);
			list.clear();
			for (SimpleValue av : fc.arguments) {
				list.add(calculate(scope, av));
			}
			list = execute(lf.file.sf, lf.func, list);
			for (int i = 0; i < list.size(); i++) {
				SimpleValue resultTarget = fc.results.get(i);
				if ( resultTarget != null ) {
					assign(scope, resultTarget, list.get(i));
				} // else function result is ignored (? was used)
			}
			break;
		}
		case BlockCmd b: {
			final long origStack = this.stack;
			SubScope sub = new SubScope(scope);
			final int cc = b.commandCount();
			for (int pc = 0; pc < cc; pc++) {
				exec(sub, b.command(pc), list);
			}
			this.stack = origStack;
			break;
		}
		case AssignCmd a:
			assign(scope, a.target, calculate(scope, a.value));
			break;
		case VarDeclCmd vd: {
			long addr = allocStackData(vd.sv.type());
			ConstantValue.DataValue addrValue = new ConstantValue.DataValue(vd.sv.type(), addr);
			scope.values.put(vd.sv.name(), addrValue);
			if ( vd.sv.initialValue() != null ) {
				scope.value(this, vd.sv.name(), calculate(scope, vd.sv.initialValue()));
			}
			break;
		}
		case IfCmd i:
			if ( ( (ConstantValue.ScalarValue) calculate(scope, i.condition) ).value() != 0L ) {
				SubScope sub = new SubScope(scope);
				exec(sub, i.trueCmd, list);
			} else if ( i.falseCmd != null ) {
				SubScope sub = new SubScope(scope);
				exec(sub, i.falseCmd, list);
			}
			break;
		case WhileCmd w:
			while ( ( (ConstantValue.ScalarValue) calculate(scope, w.condition) ).value() != 0L ) {
				SubScope sub = new SubScope(scope);
				exec(sub, w.loop, list);
			}
			break;
		default:
			throw new AssertionError("unknown Command class: " + cmd.getClass());
		}
	}
	
	private void assign(SubScope scope, SimpleValue targetValue, ConstantValue newValue) throws AssertionError {
		switch ( targetValue ) {
		case VariableVal v:
			scope.value(this, v.sv().name(), adjust(v.sv().type(), newValue));
			break;
		case BinaryOpVal bo when bo.op() == BinaryOp.ARR_PNTR_INDEX: {
			ConstantValue ca = calculate(scope, bo.a());
			ConstantValue cb = calculate(scope, bo.b());
			long off = ( (ConstantValue.ScalarValue) cb ).value();
			off *= arrPntrIndexMul(ca.type());
			long targetAddr;
			if ( ca.type() instanceof ArrayType at ) {
				targetAddr = ( (ConstantValue.DataValue) ca ).address() + off;
				newValue = adjust(at.target(), newValue);
			} else {
				targetAddr = ( (ConstantValue.ScalarValue) ca ).value() + off;
				newValue = adjust(( (PointerType) ca.type() ).target(), newValue);
			}
			put(targetAddr, newValue);
			break;
		}
		case BinaryOpVal bo when bo.op() == BinaryOp.DEREF_BY_NAME: {
			ConstantValue ca = calculate(scope, bo.a());
			String name = ( (NameVal) bo.b() ).name();
			long off = switch ( bo.a().type() ) {
			case FuncType ft -> {
				newValue = adjust(ft.member(name, ErrorContext.NO_CONTEXT, false).type(), newValue);
				yield ft.offset(name);
			}
			case StructType st -> {
				newValue = adjust(st.member(name, ErrorContext.NO_CONTEXT).type(), newValue);
				yield st.offset(name);
			}
			default -> throw new AssertionError("illegal value class: " + bo.a().type().getClass());
			};
			off *= arrPntrIndexMul(ca.type());
			long targetAddr = ( (ConstantValue.DataValue) ca ).address() + off;
			put(targetAddr, newValue);
			break;
		}
		default: {
			long targetAddr = addressOf(scope, targetValue);
			put(targetAddr, adjust(targetValue.type(), newValue));
		}
		}
	}
	
	private static ConstantValue adjust(SimpleType type, ConstantValue value) {
		if ( type == value.type() ) return value;
		switch ( value ) {
		case ConstantValue.DataValue dv:
			return new ConstantValue.DataValue(type, dv.address());
		case ConstantValue.ScalarValue sv:
			return new ConstantValue.ScalarValue(type, sv.value());
		case ConstantValue.FPValue fv:
			return new ConstantValue.FPValue(type, fv.value());
		}
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
		if ( blk.commandCount() == 1 && blk.command(0) instanceof JavaCommand jc ) {
			List<ConstantValue> result = jc.func.execute(this, args);
			checkType(t.resMembers(), result, false);
			return result;
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
		case AddressOfVal ao:
			return new ConstantValue.ScalarValue(ao.type(), addressOf(scope, ao.a()));
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
				switch ( bo.op().type ) {
				case ARR_PNTR_INDEX: {
					ConstantValue ca = calculate(scope, bo.a());
					ConstantValue cb = calculate(scope, bo.b());
					long off = ( (ConstantValue.ScalarValue) cb ).value();
					off *= arrPntrIndexMul(ca.type());
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
					off *= arrPntrIndexMul(ca.type());
					return deref(bo.type(), ( (ConstantValue.DataValue) ca ).address() + off);
				}
				case MATH, SHIFT, CMP, BOOL, BIT:
					switch ( bo.op() ) {
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
					case SHIFT_LEFT: {
						long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
						long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
						return new ConstantValue.ScalarValue(bo.type(), ca << cb);
					}
					case SHIFT_RIGTH: {
						long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
						long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
						switch ( bo.type() ) {
						case NativeType.NUM, NativeType.DWORD, NativeType.WORD, NativeType.BYTE:
							return new ConstantValue.ScalarValue(bo.type(), ca >>> cb);
						default:
							return new ConstantValue.ScalarValue(bo.type(), ca >> cb);
						}
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
					// $CASES-OMITTED$
					default:
						throw new AssertionError();
					}
				case MATH_ADDSUB: {
					long ca = ( (ConstantValue.ScalarValue) calculate(scope, bo.a()) ).value();
					long cb = ( (ConstantValue.ScalarValue) calculate(scope, bo.b()) ).value();
					if ( bo.a().type() instanceof PointerType || bo.a().type() instanceof ArrayType ) {
						cb *= arrPntrIndexMul(bo.a().type());
					}
					if ( bo.op() == BinaryOp.MATH_ADD ) {
						return new ConstantValue.ScalarValue(bo.type(), ca + cb);
					}
					return new ConstantValue.ScalarValue(bo.type(), ca - cb);
				}
				// $CASES-OMITTED$
				default:
					throw new AssertionError("unknown binary operator type: " + bo.op().type.name());
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
				if ( c.type() instanceof PointerType p ) {
					return new ConstantValue.ScalarValue(p, d.address());
				}
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
		case FunctionVal f: {
			SimpleFile sf = (SimpleFile) f.func().dep();
			LoadedSF lsf = this.loaded.get(sf);
			long addr = lsf.addressOf(this, f.func().name());
			return new ConstantValue.ScalarValue(f.type(), addr);
		}
		case ScalarNumericVal s:
			return new ConstantValue.ScalarValue(s.type(), s.value());
		case FPNumericVal s:
			return new ConstantValue.FPValue(s.type(), s.value());
		case VariableVal v:
			return scope.value(this, v.sv().name());
		default:
			throw new AssertionError("unknown value class: " + val.getClass());
		}
	}
	
	private long addressOf(ValueScope scope, SimpleValue v) {
		switch ( v ) {
		case CastVal cv:
			return addressOf(scope, cv.value());
		case DataVal d:
			return scope.value(this, d).address();
		case BinaryOpVal bo when bo.op() == BinaryOp.ARR_PNTR_INDEX: {
			ConstantValue ca = calculate(scope, bo.a());
			ConstantValue cb = calculate(scope, bo.b());
			long a = ca instanceof ConstantValue.ScalarValue s ? s.value() : ( (ConstantValue.DataValue) ca ).address();
			long b = ( (ConstantValue.ScalarValue) cb ).value();
			b *= arrPntrIndexMul(ca.type());
			return a + b;
		}
		case BinaryOpVal bo when bo.op() == BinaryOp.DEREF_BY_NAME: {
			ConstantValue ca = calculate(scope, bo.a());
			String name = ( (NameVal) bo.b() ).name();
			long off = switch ( bo.a().type() ) {
			case FuncType ft -> ft.offset(name);
			case StructType st -> st.offset(name);
			default -> throw new AssertionError("Unexpected value: " + bo.a().type());
			};
			off *= arrPntrIndexMul(ca.type());
			return ( (ConstantValue.DataValue) ca ).address() + off;
		}
		case VariableVal vv:
			return scope.addressOf(this, vv.sv().name());
		default:
			throw new IllegalStateException("can't calculate the address the value: " + v);
		}
	}
	
	private static long arrPntrIndexMul(SimpleType t) {
		SimpleType target;
		if ( t instanceof PointerType p ) {
			target = p.target();
		} else {
			target = ( (ArrayType) t ).target();
		}
		long alignM1 = target.align() - 1L;
		long mul = target.size();
		if ( ( mul & alignM1 ) != 0 ) {
			mul = ( mul & ~alignM1 ) + ( alignM1 + 1L );
		}
		return mul;
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
	
	private record SubScope(ValueScope parent, Map<String,ConstantValue> values) implements ValueScope {
		
		public SubScope(SimpleInterpreter si, ValueScope parent, List<SimpleVariable> l0, List<SimpleVariable> l1) {
			this(parent, funcMap(si, l0, l1));
		}
		
		public SubScope(ValueScope parent) {
			this(parent, new HashMap<>());
		}
		
		private static Map<String,ConstantValue> funcMap(SimpleInterpreter si, List<SimpleVariable> l0,
			List<SimpleVariable> l1) {
			Map<String,ConstantValue> map = new HashMap<>();
			putList(si, l0, map);
			putList(si, l1, map);
			return map;
		}
		
		private static void putList(SimpleInterpreter si, List<SimpleVariable> l0, Map<String,ConstantValue> map)
			throws AssertionError {
			for (SimpleVariable sv : l0) {
				switch ( sv.type() ) {
				case NativeType.FPNUM, NativeType.FPDWORD ->
					map.put(sv.name(), new ConstantValue.FPValue(sv.type(), 0d));
				case NativeType nt -> map.put(sv.name(), new ConstantValue.ScalarValue(nt, 0L));
				case PointerType pt -> map.put(sv.name(), new ConstantValue.ScalarValue(pt, 0L));
				case FuncType ft when ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) != 0 ->
					map.put(sv.name(), new ConstantValue.ScalarValue(ft, 0L));
				case ArrayType at -> {
					long addr = si.allocStackData(at.align(), at.size());
					map.put(sv.name(), new ConstantValue.DataValue(at, addr));
				}
				case StructType st -> {
					long addr = si.allocStackData(st.align(), st.size());
					map.put(sv.name(), new ConstantValue.DataValue(st, addr));
				}
				case FuncType ft -> {
					long addr = si.allocStackData(ft.align(), ft.size());
					map.put(sv.name(), new ConstantValue.DataValue(ft, addr));
				}
				default -> throw new AssertionError("unknown type class: " + sv.type().getClass());
				}
			}
		}
		
		@Override
		public ConstantValue value(SimpleInterpreter si, String name) {
			ConstantValue v = this.values.get(name);
			if ( v == null ) return this.parent.value(si, name);
			if ( v instanceof ConstantValue.DataValue d
				&& ( d.type() instanceof NativeType || d.type() instanceof PointerType
					|| ( d.type() instanceof FuncType ft && ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) != 0 ) ) ) {
				return si.deref(d.type(), d.address());
			}
			return v;
		}
		
		@Override
		public void value(SimpleInterpreter si, String name, ConstantValue value) {
			ConstantValue v = this.values.get(name);
			if ( v == null ) {
				this.parent.value(si, name, value);
				return;
			}
			if ( v instanceof ConstantValue.DataValue d ) {
				si.put(d.address(), value);
			} else {
				this.values.put(name, value);
			}
		}
		
		@Override
		public long addressOf(SimpleInterpreter si, String name) {
			ConstantValue v = this.values.get(name);
			if ( v == null ) {
				return this.parent.addressOf(si, name);
			}
			if ( v instanceof ConstantValue.DataValue d ) {
				return d.address();
			}
			long addr = si.allocStackData(v.type().align(), v.type().size());
			si.put(addr, v);
			this.values.put(name, new ConstantValue.DataValue(v.type(), addr));
			return addr;
		}
		
		@Override
		public ConstantValue.DataValue value(SimpleInterpreter si, DataVal data) {
			return this.parent.value(si, data);
		}
		
		
	}
	
	private final Map<Long,LoadedFunction> funcs = new HashMap<>();
	
	public LoadedFunction ofAddr(long addr) {
		LoadedFunction f = this.funcs.get(Long.valueOf(addr));
		if ( f != null ) return f;
		throw new NullPointerException("there is no function at address 0x" + Long.toHexString(addr));
	}
	
	private record LoadedFunction(LoadedSF file, SimpleFunction func) {}
	
	private record LoadedSF(SimpleFile sf, Map<String,ConstantValue.DataValue> addrs, Map<byte[],Long> rodata)
		implements ValueScope {
		
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
			ConstantValue.DataValue res = this.addrs.get(name);
			if ( res == null ) {
				res = allocValue(si, name);
			}
			return si.deref(res.type(), res.address());
		}
		
		@Override
		public long addressOf(SimpleInterpreter si, String name) {
			ConstantValue.DataValue res = this.addrs.get(name);
			if ( res == null ) {
				res = allocValue(si, name);
			}
			return res.address();
		}
		
		@Override
		public void value(SimpleInterpreter si, String name, ConstantValue value) {
			ConstantValue.DataValue res = this.addrs.get(name);
			if ( res == null ) {
				res = allocValue(si, name);
			}
			si.put(res.address(), value);
		}
		
		private ConstantValue.DataValue allocValue(SimpleInterpreter si, String name) {
			ConstantValue.DataValue res;
			SimpleVariable sv = this.sf.variable(name);
			if ( sv == null ) {
				SimpleFunction f = this.sf.function(name);
				if ( f == null ) throw new IllegalArgumentException(noVal(name));
				long addr = si.allocROData(1, 1L);
				res = new ConstantValue.DataValue(f.type(), addr);
				Long l = Long.valueOf(addr);
				this.addrs.put(name, res);
				si.funcs.put(l, new LoadedFunction(this, f));
				return res;
			}
			long addr = si.allocStackData(sv.type().align(), sv.type().size());
			res = new ConstantValue.DataValue(sv.type(), addr);
			this.addrs.put(name, res);
			return res;
		}
		
		private static String noVal(String name) {
			return "no value with name " + name + " found";
		}
		
		@Override
		public ConstantValue.DataValue value(SimpleInterpreter si, DataVal data) {
			DataVal orig = data.orig() == null ? data : data.orig();
			byte[] bytes = orig.value();
			Long addr = this.rodata.get(bytes);
			if ( addr != null ) {
				return new ConstantValue.DataValue(data.type(), addr.longValue() + data.off());
			}
			long roaddr = si.allocROData(data.type().align(), bytes.length);
			si.roaddr = roaddr + bytes.length;
			MemoryManager mem = si.mm;
			addr = Long.valueOf(roaddr);
			this.rodata.put(bytes, addr);
			ByteBuffer bbuf = ByteBuffer.wrap(bytes);
			mem._privilegedSetRO(roaddr, bbuf);
			return new ConstantValue.DataValue(data.type(), roaddr + data.off());
		}
		
	}
	
	private long allocROData(int requestedAlign, long length) {
		final MemoryManager mem = this.mm;
		final long pageSize = mem.pageSize();
		final long align = pageSize < requestedAlign ? pageSize : requestedAlign;
		final long alignMask = align - 1;
		long roAddr = this.roaddr;
		if ( ( roAddr & alignMask ) != 0 ) {
			roAddr = ( roAddr & ~alignMask ) + align;
		}
		if ( pageSize >= ( roAddr & ( pageSize - 1L ) ) + length || roAddr == 0L ) {
			roAddr = mem.allocate(length, 0L, MemoryManager.FLAG_READ_ONLY);
		}
		this.roaddr = roAddr + length;
		return roAddr;
	}
	
	private long allocStackData(SimpleType type) {
		return allocStackData(type.align(), type.size());
	}
	
	private long allocStackData(int align, long length) {
		final MemoryManager mem = this.mm;
		final long alignMask = Math.min(mem.pageSize(), align) - 1;
		long stack = this.stack - length;
		if ( ( stack & alignMask ) != 0 ) {
			stack = ( stack & ~alignMask );
		}
		this.stack = stack;
		return stack;
	}
	
	private sealed interface ValueScope {
		
		ConstantValue value(SimpleInterpreter si, String name);
		
		ConstantValue.DataValue value(SimpleInterpreter si, DataVal data);
		
		void value(SimpleInterpreter si, String name, ConstantValue value);
		
		long addressOf(SimpleInterpreter si, String name);
		
	}
	
}
