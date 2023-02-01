package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.IOException;
import java.nio.file.Path;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;
import de.hechler.patrick.zeugs.pfs.interfaces.File;


public class IgnoreCompiler implements Compiler {
	
	public void addTranslationUnit(Path source, File target) throws IOException {
		target.close();
	}
	
	public void compile() {/**/}
	
}
