package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

import java.nio.file.Path;

import java.util.Set;
import java.util.HashSet;


public abstract class MultiCompiler implements Compiler {
	
	private final Set<PatCompiler> comps = new HashSet<>();
	
	public void addTranslationUnit(Path source, Path target) {
		Compiler c = findCompiler(source, target);
		c.addTranslationUnit(source, target);
		comps.add(c);
	}
	
	public void compile() {
		for (Compiler c : comps) {
			c.compile();
		}
	}
	
	protected abstract Compiler findCompiler(Path source);
	
}
