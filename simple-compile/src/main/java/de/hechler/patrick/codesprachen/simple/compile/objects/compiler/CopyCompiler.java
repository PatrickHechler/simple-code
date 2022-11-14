package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class CopyCompiler extends PerFileCompiler {
	
	protected void compile(Path source, Path target) {
		try {
			Files.copy(source, target);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
