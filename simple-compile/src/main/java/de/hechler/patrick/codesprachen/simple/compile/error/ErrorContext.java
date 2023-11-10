package de.hechler.patrick.codesprachen.simple.compile.error;

import java.util.function.Supplier;

public class ErrorContext {
	
	public static final ErrorContext NO_CONTEXT = new ErrorContext(-1, -1, -1, (String) null);
	
	public final int               line;
	public final int               charInLine;
	public final int               totalChar;
	private final Supplier<String> offendingTokenSub;
	private String                 offendingToken;
	
	public ErrorContext(int line, int charInLine, int totalChar, Supplier<String> offendingTokenSub) {
		this.line              = line;
		this.charInLine        = charInLine;
		this.totalChar         = totalChar;
		this.offendingTokenSub = offendingTokenSub;
		this.offendingToken    = null;
	}
	
	public ErrorContext(int line, int charInLine, int totalChar, String offendingToken) {
		this.line              = line;
		this.charInLine        = charInLine;
		this.totalChar         = totalChar;
		this.offendingTokenSub = null;
		this.offendingToken    = offendingToken;
	}
	
	public String offendingToken() {
		String ot = this.offendingToken;
		if ( ot != null ) return ot;
		ot                  = this.offendingTokenSub.get();
		this.offendingToken = ot;
		return ot;
	}
	
}
