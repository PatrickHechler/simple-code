package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;

import java.nio.file.Path;


public class IgnoreCompiler implements Compiler {
	
	public void addTranslationUnit(Path source, Path target) { }
	
	public void compile() { }
	
}
