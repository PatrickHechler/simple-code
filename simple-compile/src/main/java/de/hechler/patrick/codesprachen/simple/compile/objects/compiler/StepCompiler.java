package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.ThrowingConsumer;
import de.hechler.patrick.zeugs.pfs.interfaces.File;

public abstract class StepCompiler<T extends TranslationUnit> implements Compiler {
	
	/**
	 * this map saves all translation units with their {@link TranslationUnit#source source} path as key
	 */
	private final Map<Path, T>   wtus = new HashMap<>();
	/**
	 * this map is a read only map of all translation units currently saved in a map
	 */
	protected final Map<Path, T> tus  = Collections.unmodifiableMap(this.wtus);
	
	/** {@inheritDoc} */
	public void addTranslationUnit(Path source, File target) {
		T tu = createTU(source, target);
		this.wtus.put(source, tu);
	}
	
	/** {@inheritDoc} */
	public void compile() throws IOException {
		if (!this.skipInit()) {
			compileUnordered(this::init);
		}
		if (!this.skipPrecompile()) {
			compileUnordered(this::precompile);
		}
		if (!this.skipCompile()) {
			compileOrdered(this::compile);
		}
		compileUnordered(t -> {
			try (t.target) {
				finish(t);
			}
		});
	}
	
	private void compileOrdered(ThrowingConsumer<T, IOException> step) throws IOException {
		for (T t : this.wtus.values()) {
			step.accept(t);
		}
	}
	
	private volatile IOException err = null;
	
	private void compileUnordered(ThrowingConsumer<T, IOException> step) throws IOException {
		try (Stream<T> s = this.wtus.values().parallelStream()) {
			s.forEach(t -> {
				try {
					step.accept(t);
				} catch (IOException e) {
					wrappingRethrow(e);
				}
			});
			IOException e = this.err;
			if (e != null) {
				assert false;
				throw e;
			}
		} catch (IllegalArgumentException e) {
			unwrappingRethrow(e);
		} finally {
			this.err = null;
		}
	}
	
	private void wrappingRethrow(IOException e) {
		synchronized (this) {
			if (this.err == null) {
				this.err = e;
			} else {
				this.err.addSuppressed(e);
			}
		}
		throw new IllegalArgumentException(this.err);
	}
	
	private void unwrappingRethrow(IllegalArgumentException e) throws IOException {
		if (e.getCause() == this.err) {
			throw this.err;
		} else {
			throw e;
		}
	}
	
	/**
	 * creates a {@link TranslationUnit} for the given {@codes source} and {@code target}.
	 * 
	 * @param source the source of the {@link TranslationUnit}
	 * @param target the target of the {@link TranslationUnit}
	 * 
	 * @return the newly created {@link TranslationUnit}
	 */
	protected abstract T createTU(Path source, File target);
	
	/**
	 * this method can be overwritten when the implementation of the {@link StepCompiler} does not need the initialization phase
	 * <p>
	 * the default implementation returns <code>false</code>
	 * 
	 * @return <code>true</code> if the initialization phase should be skipped
	 */
	protected boolean skipInit() {
		return false;
	}
	
	/**
	 * Initializes the given translation unit
	 * <p>
	 * this method may be called, while an other thread initializes a different {@link TranslationUnit}
	 * 
	 * @param tu the {@link TranslationUnit} to initialize
	 * 
	 * @throws IOException if an IO error occurs
	 */
	protected abstract void init(T tu) throws IOException;
	
	/**
	 * this method can be overwritten when the implementation of the {@link StepCompiler} does not need the PreCompile phase
	 * <p>
	 * the default implementation returns <code>false</code>
	 * 
	 * @return <code>true</code> if the PreCompile phase should be skipped
	 */
	protected boolean skipPrecompile() {
		return false;
	}
	
	/**
	 * precompiles the given Translation Unit
	 * <p>
	 * this method may be called, while an other thread precompiles a different {@link TranslationUnit}
	 * <p>
	 * it is ensured, that all translation units are initialized at this point
	 * 
	 * @param tu the {@link TranslationUnit} to precompile
	 * 
	 * @throws IOException if an IO error occurs
	 */
	protected abstract void precompile(T tu) throws IOException;
	
	/**
	 * this method can be overwritten when the implementation of the {@link StepCompiler} does not need the compile phase
	 * <p>
	 * the default implementation returns <code>false</code>
	 * 
	 * @return <code>true</code> if the compile phase should be skipped
	 */
	protected boolean skipCompile() {
		return false;
	}
	
	/**
	 * compiles the given Translation Unit
	 * <p>
	 * this method may not be called, while an other thread compiles a different {@link TranslationUnit}
	 * <p>
	 * it is ensured, that all translation units are initialized and precompiled at this point
	 * 
	 * @param tu the {@link TranslationUnit} to compile
	 * 
	 * @throws IOException if an IO error occurs
	 */
	protected abstract void compile(T tu) throws IOException;
	
	/**
	 * finishes the translation of the given Translation Unit
	 * <p>
	 * this method may be called, while an other thread finishes a different {@link TranslationUnit}
	 * <p>
	 * it is ensured, that all translation units are compiled at this point
	 * <p>
	 * this method is intended to do the remaining IO and other stuff, because {@link #compile(TranslationUnit)} is not asyncronos <br>
	 * after this method finished the {@link TranslationUnit#target} file of the passed TranslationUnit is automatically closed.
	 * <p>
	 * note that there is no skipFinish() method, because the {@link StepCompiler} {@link File#close() closes} the {@link TranslationUnit#target target} file, if
	 * there is nothing more needed the implementation can just make a this method return directly
	 * 
	 * @param tu the {@link TranslationUnit} to precompile
	 * 
	 * @throws IOException if an IO error occurs
	 */
	protected abstract void finish(T tu) throws IOException;
	
}
