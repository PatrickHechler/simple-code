package de.hechler.patrick.codesprachen.simple.compile.interfaces;

import java.io.IOException;
import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.File;

/**
 * a compiler can compile/translate some source files to target files.
 * <p>
 * <ol>
 * <li>at first all source/target files are added with the {@link #addTranslationUnit(Path, File)} method.</li>
 * <li>then the {@link #compile()} method is invoked, there the compiler should execute it's translation process</li>
 * </ol>
 * 
 * @author pat
 */
public interface Compiler {
	
	/**
	 * registers the given source file and target file.
	 * <p>
	 * the target file will not be closed after this call, the compile has to close the target file, after it is no
	 * longer needed
	 * <p>
	 * the compiler should not compile the source file directly in this method. (even if it can)
	 * 
	 * @param source the source file
	 * @param target the target file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void addTranslationUnit(Path source, File target) throws IOException;
	
	/**
	 * compile all added files to the given target files.
	 * <p>
	 * this method should close all target files.
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void compile() throws IOException;
	
}
