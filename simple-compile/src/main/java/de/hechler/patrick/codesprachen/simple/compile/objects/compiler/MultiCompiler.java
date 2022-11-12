package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

public abstract class MultiCompiler implements Compiler {

	private final Set<Compiler> comps = new HashSet<>();
	
	public void addTranslationUnit(Path source, Path target) {
		Compiler c = findCompiler(source);
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
