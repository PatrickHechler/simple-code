package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.nio.file.Files;
import java.nio.file.Path;


public class CopyCompiler extends PerFileCompiler {
	
	protected void compile(Path source, Path target) {
		Files.copy(source, target);
	}
	
}
