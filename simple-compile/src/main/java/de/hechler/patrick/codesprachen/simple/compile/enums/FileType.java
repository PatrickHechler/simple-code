package de.hechler.patrick.codesprachen.simple.compile.enums;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;

public enum FileType {
	
	/**
	 * compile the file as a simple code file.<br>
	 * mark the output file as executable if it has a main function
	 * <p>
	 * by default every file with *.sc is {@link #simpleCode}
	 */
	simpleCode,
	/**
	 * compile the file as a simple code file.<br>
	 * never mark the output file as executable
	 * <p>
	 * by default no file is {@link #simpleCodeNonExecutable}
	 */
	simpleCodeNonExecutable,
	/**
	 * assemble the file with the {@link PrimitiveAssembler}<br>
	 * mark the output file as executable
	 * <p>
	 * by default every file with *.psc is {@link #primitiveCode}
	 */
	primitiveCode,
	/**
	 * assemble the file with the {@link PrimitiveAssembler}<br>
	 * do not mark the output file as executable
	 * <p>
	 * by default no file is {@link #primitiveCodeExecutable}
	 */
	primitiveCodeExecutable,
	/**
	 * copy the file<br>
	 * do not mark the copied file as executable
	 * <p>
	 * by default every file which is not used anywhere else is {@link #copy}
	 */
	copy,
	/**
	 * copy the file<br>
	 * mark the copied file as executable
	 * <p>
	 * by default no file is {@link #copyExecutable}
	 */
	copyExecutable,
	/**
	 * ignore the file<br>
	 * do not create an output file for this input file
	 * <p>
	 * by default no file is {@link #ignore}
	 */
	ignore,
	
}
