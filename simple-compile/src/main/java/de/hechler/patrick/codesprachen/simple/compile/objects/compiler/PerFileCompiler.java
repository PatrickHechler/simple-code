package de.hechler.patrick.codesprachen.simple.compile.objects;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

import java.nio.file.Path;
import java.util.List;
import java.util.LinkedList;


public abstract class PerFileCompiler implements Compiler {
	
	private final List<TwoPaths> paths = new LinkedList<>();
	
	public void addTranslationUnit(Path source, Path target) {
		paths.add(new TwoPaths(source, target));
	}
	
	public void compile() {
		for (TwoPaths tp : paths) {
			compile(tp.source, tp.target);
		}
	}
	
	protected abstract void compile(Path source, Path target);
	
	private static class TwoPaths {
		
		private final Path source, target;
		
		private TwoPaths(Path source, Path target) {
			this.source = source;
			this.target = target;
		}
		
	}
	
}
