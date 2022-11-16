package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

public abstract class StepCompiler<TU extends TranslationUnit> implements Compiler {
	
	protected final Map<Path, TU> tus = new HashMap<>();
	
	public void addTranslationUnit(Path source, Path target) throws IOException {
		TU tu = createTU(source, target);
		tus.put(source, tu);
	}
	
	private volatile IOException err = null;
	
	public void compile() throws IOException {
		try {
			tus.values().parallelStream().forEach(t -> {
				try {
					init(t);
				} catch (IOException e) {
					synchronized (StepCompiler.this) {
						if (err == null) {
							err = e;
						} else {
							err.addSuppressed(e);
						}
						throw new RuntimeException(err);
					}
				}
			});
			if (err != null) { throw err; }
		} catch (RuntimeException e) {
			if (e.getCause() == err) {
				throw err;
			} else {
				throw e;
			}
		}
		try {
			tus.values().parallelStream().forEach(t -> {
				try {
					precompile(t);
				} catch (IOException e) {
					synchronized (StepCompiler.this) {
						if (err == null) {
							err = e;
						} else {
							err.addSuppressed(e);
						}
						throw new RuntimeException(err);
					}
				}
			});
			if (err != null) { throw err; }
		} catch (RuntimeException e) {
			if (e.getCause() == err) {
				throw err;
			} else {
				throw e;
			}
		}
		for (TU tu : tus.values()) {
			compile(tu);
		}
		try {
			tus.values().parallelStream().forEach(t -> {
				try {
					finish(t);
				} catch (IOException e) {
					synchronized (StepCompiler.this) {
						if (err == null) {
							err = e;
						} else {
							err.addSuppressed(e);
						}
						throw new RuntimeException(err);
					}
				}
			});
			if (err != null) { throw err; }
		} catch (RuntimeException e) {
			if (e.getCause() == err) {
				throw err;
			} else {
				throw e;
			}
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
	protected abstract TU createTU(Path source, Path target);
	
	/**
	 * Initializes the given translation unit <p> this method may be called, while an other thread initilises a
	 * different {@link TranslationUnit}
	 * 
	 * @param tu the {@link TranslationUnit} to initialize
	 * 
	 * @throws IOException
	 */
	protected abstract void init(TU tu) throws IOException;
	
	/**
	 * precompiles the given Translation Unit <p> this method may be called, while an other thread precompiles a
	 * different {@link TranslationUnit} <p> it is ensured, that all translation units are initilised at this point
	 * 
	 * @param tu the {@link TranslationUnit} to precompile
	 * 
	 * @throws IOException
	 */
	protected abstract void precompile(TU tu) throws IOException;
	
	/**
	 * compiles the given Translation Unit <p> this method may not be called, while an other thread compiles a different
	 * {@link TranslationUnit} <p> it is ensured, that all translation units are initilised and precompiled at this
	 * point
	 * 
	 * @param tu the {@link TranslationUnit} to compile
	 * 
	 * @throws IOException
	 */
	protected abstract void compile(TU tu) throws IOException;
	
	/**
	 * finishes the translation of the given Translation Unit <p> this method may be called, while an other thread
	 * finishes a different {@link TranslationUnit} <p> it is ensured, that all translation units are compiled at this
	 * point <p> this method is intended to do the remaining io and other stuff, because
	 * {@link #compile(TranslationUnit)} is not asyncronos
	 * 
	 * @param tu the {@link TranslationUnit} to precompile
	 * 
	 * @throws IOException
	 */
	protected abstract void finish(TU tu) throws IOException;
	
}
