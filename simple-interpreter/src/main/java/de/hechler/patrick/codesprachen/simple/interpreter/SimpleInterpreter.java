package de.hechler.patrick.codesprachen.simple.interpreter;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongBiFunction;

import de.hechler.patrick.codesprachen.simple.interpreter.fs.FSManager;
import de.hechler.patrick.codesprachen.simple.interpreter.fs.FSManagerImpl;
import de.hechler.patrick.codesprachen.simple.interpreter.java.JavaFunction.ConstantValue;
import de.hechler.patrick.codesprachen.simple.interpreter.java.JavaStdLib;
import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManager;
import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManagerImpl;
import de.hechler.patrick.codesprachen.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;

public class SimpleInterpreter {
	
	private final Path[]                                       src;
	private final ToLongBiFunction<SimpleInterpreter,String[]> allocArgs;
	private final Path                                         defMain;
	private final MemoryManager                                mm;
	private final FSManager                                    fsm;
	private final SimpleDependency                             stdlib;
	private final Map<Path,SimpleFile>                         files = new HashMap<>();
	
	public SimpleInterpreter(List<Path> src) {
		this(src, null, JavaStdLib::allocArgs);
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter,String[]> allocArgs) {
		this(src, stdlib, allocArgs, null, src.get(0));
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter,String[]> allocArgs, Path defMain, Path root) {
		this(src, stdlib, allocArgs, defMain, new MemoryManagerImpl(), root);
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter,String[]> allocArgs, Path defMain, MemoryManager memManager, Path root) {
		this(src, stdlib, allocArgs, defMain, memManager, new FSManagerImpl(root, memManager));
	}
	
	public SimpleInterpreter(List<Path> src, SimpleDependency stdlib,
		ToLongBiFunction<SimpleInterpreter,String[]> allocArgs, Path defMain, MemoryManager memManager,
		FSManager fsManager) {
		this.src = src.toArray(new Path[src.size()]);
		this.allocArgs = allocArgs;
		this.defMain = defMain;
		this.mm = memManager;
		this.fsm = fsManager;
		this.stdlib = stdlib == null ? new JavaStdLib(this) : stdlib;
	}
	
	protected synchronized SimpleFile load(Path p, Path rel) throws IOException {
		if ( !p.isAbsolute() ) {
			if ( rel == null ) p = p.relativize(p.getRoot());
			else p = p.resolve(rel);
		}
		Path oldP = p;
		SimpleFile sf = this.files.get(p);
		if ( sf != null ) return sf;
		sf = this.files.get(p);
		if ( sf != null ) return sf;
		for (Path path : this.src) {
			p = path.resolve(p);
			if ( !Files.exists(p) ) {
				continue;
			}
			final Path realP = p.toRealPath();
			p = realP;
			try ( InputStream in = Files.newInputStream(p) ) {
				SimpleSourceFileParser parser = new SimpleSourceFileParser(in, oldP.toString(),
					(srcPath, runtimePath) -> {
						if ( runtimePath == null ) {
							if ( srcPath.endsWith(".sexp") ) {
								runtimePath = srcPath.substring(0, srcPath.length() - ".sexp".length());
							} else {
								runtimePath = srcPath;
							}
						}
						runtimePath += ".ssf";
						try {
							Path srcFile = realP.getFileSystem().getPath(runtimePath);
							Path relDir = realP.getParent();
							return load(srcFile, relDir);
						} catch ( IOException e ) {
							throw new IOError(e);
						}
					});
				sf = new SimpleFile(oldP.toString());
				sf.dependency(this.stdlib, "std", ErrorContext.NO_CONTEXT);
				parser.parse(sf);
				this.files.put(oldP, sf);
				return sf;
			}
		}
		throw new FileNotFoundException(p.toString());
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
		SimpleFile mainFile = load(main, null);
		SimpleFunction func = Objects.requireNonNull(mainFile.main(), "the main file has no main function");
		long addr = this.allocArgs.applyAsLong(this, args);
		List<ConstantValue> list = List.of(new ConstantValue.ScalarValue(NativeType.UNUM, args.length),
			new ConstantValue.ScalarValue(FuncType.MAIN_TYPE.argMembers().get(1).type(), addr));
		return (int) ( (ConstantValue.ScalarValue) execute(func, list).get(0) ).value();
	}
	
	public List<ConstantValue> execute(SimpleFunction func, List<ConstantValue> args) {
		
	}
	
}
