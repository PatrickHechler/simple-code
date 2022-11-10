package de.hechler.patrick.codesprachen.simple.compile.interfaces;

import java.nio.file.Path;


public interface Compiler {
	
	void addTranslationUnit(Path source, Path target);
	
	void compile();
	
}
