package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.*;

import java.io.InputStream;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFile;

public class SimpleExportFileParser {
	
	private final SimpleTokenStream in;
	
	public SimpleExportFileParser(InputStream in, String file) {
		this(new SimpleTokenStream(in, file));
	}
	
	public SimpleExportFileParser(SimpleTokenStream in) {
		this.in = in;
	}
	
	
	public SimpleDependency parse(String runtimePath) {
		SimpleFile sf = new SimpleFile(runtimePath);
		while ( true ) {
			switch ( in.token() ) {
			case DEP:
				parseDependency(sf);
				break;
			case TYPEDEF:
				parseTypedef(sf);
				break;
			case FUNC:
				parseFunction(sf);
				break;
			default:
				parseVariable(sf);
			case EOF:
				return sf;
			}
		}
	}
	
	protected void parseDependency(SimpleFile sf) {
		in.consume();
		if (in.token() != NAME) {
			throw new CompileError(in.file, in.line, in.file, CHAR, null, null, null)
		}
	}
	
	protected void parseVariable(SimpleFile sf) {
		// TODO Auto-generated method stub
	}
	
	protected void parseTypedef(SimpleFile sf) {
		// TODO Auto-generated method stub
	}
	
	protected void parseFunction(SimpleFile sf) {
		// TODO Auto-generated method stub
	}
	
}
