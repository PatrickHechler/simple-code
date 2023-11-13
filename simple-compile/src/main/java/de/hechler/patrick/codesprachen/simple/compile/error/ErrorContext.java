package de.hechler.patrick.codesprachen.simple.compile.error;

import java.util.function.Supplier;

public class ErrorContext {
	
	public static final ErrorContext NO_CONTEXT = new ErrorContext(null, null);
	
	public String                  file;
	public int                     line;
	public int                     charInLine;
	public int                     totalChar;
	private String                 offendingToken;
	private final Supplier<String> offendingTokenSup;
	
	public ErrorContext(String file, Supplier<String> offendingTokenSup) {
		this.file = file;
		this.line = -1;
		this.charInLine = -1;
		this.totalChar = -1;
		this.offendingTokenSup = offendingTokenSup;
	}
	
	public void setOffendingTokenCach(String offendingToken) {
		this.offendingToken = offendingToken;
	}
	
	public String offendingToken() {
		String ot = this.offendingToken;
		if ( ot == null ) {
			Supplier<String> sub = this.offendingTokenSup;
			if ( sub != null ) {
				ot = sub.get();
				this.offendingToken = ot;
			}
		}
		return ot;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrorContext [file=");
		builder.append(file);
		builder.append(", line=");
		builder.append(line);
		builder.append(", charInLine=");
		builder.append(charInLine);
		builder.append(", totalChar=");
		builder.append(totalChar);
		builder.append(", offendingToken()=");
		builder.append(offendingToken());
		builder.append("]");
		return builder.toString();
	}
	
}
