package de.hechler.patrick.codesprachen.simple.compile.error;

import java.util.List;

public class CompileError extends Exception {
	
	private static final long serialVersionUID = 624098131567818737L;
	
	public final int          line;
	public final int          charInLine;
	public final int          totalChar;
	public final String       offendingToken;
	public final List<String> expectedTokens;// NOSONAR the list is serializable
	
	public CompileError(int line, int charInLine, int totalChar, String offendingToken, List<String> expectedTokens) {
		this(line, charInLine, totalChar, offendingToken, expectedTokens, null);
	}
	
	public CompileError(int line, int charInLine, int totalChar, String offendingToken, List<String> expectedTokens,
		String additionalMessage) {
		super(generateMessage(line, charInLine, totalChar, offendingToken, expectedTokens, additionalMessage));
		this.line           = line;
		this.charInLine     = charInLine;
		this.totalChar      = totalChar;
		this.offendingToken = offendingToken;
		this.expectedTokens = expectedTokens == null ? List.of() : List.copyOf(expectedTokens);
	}
	
	private static String generateMessage(int line, int charInLine, int totalChar, String offendingToken,
		List<String> expectedTokens, String additionalMessage) {
		StringBuilder b = new StringBuilder().append("illegal input");
		if ( line != -1 ) {
			b.append(" at line ").append(line);
			if ( charInLine != -1 ) {
				b.append(":").append(charInLine);
			}
		}
		if ( totalChar != -1 ) {
			b.append(" at total char ").append(totalChar);
		}
		if ( additionalMessage != null ) {
			b.append(": ").append(additionalMessage);
		}
		if ( offendingToken != null ) {
			b.append(": got '").append(offendingToken).append('\'');
			if ( expectedTokens != null && !expectedTokens.isEmpty() ) {
				b.append(',');
			}
		} else if ( expectedTokens != null && !expectedTokens.isEmpty() ) {
			b.append(':');
		}
		if ( expectedTokens != null && !expectedTokens.isEmpty() ) {
			b.append(" expected [").append(expectedTokens.get(0));
			for (int i = 1; i < expectedTokens.size(); i++) {
				b.append(", ").append(expectedTokens.get(i));
			}
			b.append(']');
		}
		return b.toString();
	}
	
}
