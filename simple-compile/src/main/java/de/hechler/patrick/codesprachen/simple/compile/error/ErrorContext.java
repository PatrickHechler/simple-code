package de.hechler.patrick.codesprachen.simple.compile.error;

public class ErrorContext {
	
	public static final ErrorContext NO_CONTEXT = new ErrorContext();
	
	public String            file;
	public int               line;
	public int               charInLine;
	public int               totalChar;
	public String           offendingToken;
	
	public ErrorContext() {
		this.line = -1;
		this.charInLine = -1;
		this.totalChar = -1;
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
		builder.append(", offendingToken=");
		builder.append(offendingToken);
		builder.append("]");
		return builder.toString();
	}
	
}
