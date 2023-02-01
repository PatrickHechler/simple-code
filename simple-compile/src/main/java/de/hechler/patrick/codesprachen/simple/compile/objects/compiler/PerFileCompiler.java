package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.Compiler;
import de.hechler.patrick.zeugs.pfs.interfaces.File;


public abstract class PerFileCompiler implements Compiler {
	
	private final List<TwoFiles> paths = new LinkedList<>();
	
	public void addTranslationUnit(Path source, File target) throws IOException {
		paths.add(new TwoFiles(source, target));
	}
	
	public void compile() throws IOException {
		try {
			for (TwoFiles tp : paths) {
				compile(tp.source, tp.target);
			}
		} finally {
			for (TwoFiles tp : paths) {
				tp.target.close();
			}
		}
	}
	
	/**
	 * compile the given source file to the target file.
	 * <p>
	 * after all files are compiled they are closed automatically, therre is no need to close the target file
	 * 
	 * @param source the source file
	 * @param target the target file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	protected abstract void compile(Path source, File target) throws IOException;
	
	private static class TwoFiles {
		
		private final Path source;
		private final File target;
		
		private TwoFiles(Path source, File target) {
			this.source = source;
			this.target = target;
		}
		
	}
	
}
