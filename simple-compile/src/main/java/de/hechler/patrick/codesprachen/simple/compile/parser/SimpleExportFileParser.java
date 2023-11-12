package de.hechler.patrick.codesprachen.simple.compile.parser;

import java.io.InputStream;

import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFile;

public class SimpleExportFileParser {
	
	private final SimpleTokenStream in;
	
	public SimpleExportFileParser(InputStream in) {
		this(new SimpleTokenStream(in));
	}
	
	public SimpleExportFileParser(SimpleTokenStream in) {
		this.in = in;
	}
	
	
	public SimpleDependency parse() {
		SimpleFile file;
		
	}
	
}
