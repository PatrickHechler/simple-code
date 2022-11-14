package de.hechler.patrick.codesprachen.simple.compile.interfaces;

import java.io.IOException;
import java.nio.file.Path;


public interface Compiler {
	
	void addTranslationUnit(Path source, Path target) throws IOException;
	
	void compile() throws IOException;
	
}
