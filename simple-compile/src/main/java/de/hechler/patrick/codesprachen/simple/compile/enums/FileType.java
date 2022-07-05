package de.hechler.patrick.codesprachen.simple.compile.enums;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;

public enum FileType {
	
	/**
	 * compile the file as a simple code file.<br>
	 * mark the output file as executable if it has a main function
	 */
	simpleCode,
	/**
	 * compile the file as a simple code file.<br>
	 * never mark the output file as executable
	 */
	simpleCodeNonExecutable,
	/**
	 * assemble the file with the {@link PrimitiveAssembler}<br>
	 * mark the output file as executable
	 */
	primitiveCode,
	/**
	 * assemble the file with the {@link PrimitiveAssembler}<br>
	 * do not mark the output file as executable
	 */
	primitiveCodeExecutable,
	/**
	 * copy the file<br>
	 * do not mark the copied file as executable
	 */
	copy,
	/**
	 * copy the file<br>
	 * mark the copied file as executable
	 */
	copyExecutable,
	/**
	 * ignore the file<br>
	 * do not create an output file for this input file
	 */
	ignore,
	
}
