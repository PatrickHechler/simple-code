package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.DEP;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.EOF;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.FUNC;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.SEMI;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.STRING;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.TYPEDEF;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFile;

public class SimpleExportFileParser {
	
	private final SimpleTokenStream                          in;
	private final BiFunction<String,String,SimpleDependency> dep;
	
	public SimpleExportFileParser(InputStream in, String file, BiFunction<String,String,SimpleDependency> dep) {
		this(new SimpleTokenStream(in, file), dep);
	}
	
	public SimpleExportFileParser(SimpleTokenStream in, BiFunction<String,String,SimpleDependency> dep) {
		this.in = in;
		this.dep = dep;
	}
	
	
	public SimpleDependency parse(String runtimePath) {
		SimpleFile sf = new SimpleFile(runtimePath);
		while ( true ) {
			switch ( in.tok() ) {
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
		expectToken(NAME, "expected to get `[NAME] [STRING] ;´ after `dep´");
		String name = in.consumeDynTokSpecialText();
		expectToken(STRING, "expected to get `[STRING] ;´ after `dep [NAME]´");
		String path = in.consumeDynTokSpecialText();
		consumeToken(SEMI, "expected to get `;´ after `dep [NAME] [STRING]´");
		sf.dependency(dep.apply(name, path), name, in.ctx());
	}
	
	private void expectToken(int tok, String msg) {
		if ( in.tok() != tok ) {
			throw new CompileError(in.ctx(), List.of(SimpleTokenStream.name(tok)), msg);
		}
	}
	
	private void consumeToken(int tok, String msg) {
		if ( in.consumeTok() != tok ) {
			throw new CompileError(in.ctx(), List.of(SimpleTokenStream.name(tok)), msg);
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
