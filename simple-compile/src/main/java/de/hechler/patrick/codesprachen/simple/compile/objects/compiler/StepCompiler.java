package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

public abstract class StepCompiler<TU extends TranslationUnit> implements Compiler {
	
	protected final Map<Path,TU> tus = new HashMap<>();
	
	public void addTranslationUnit(Path source, Path target) {
		TU tu = init(source, target);
		tus.put(source, tu);
	}
	
	public void compile() {
		for (TU tu : tus.values()) {
			precompile(tu);
		}
		for (TU tu : tus.values()) {
			compile(tu);
		}
		for (TU tu : tus.values()) {
			finish(tu);
		}
	}
	
	protected abstract TU init(Path source, Path target);
	
	protected abstract void precompile(TU tu);
	
	protected abstract void compile(TU tu);
	
	protected abstract void finish(TU tu);
	
}
