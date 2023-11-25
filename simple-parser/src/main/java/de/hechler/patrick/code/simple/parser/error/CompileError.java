//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.code.simple.parser.error;

import java.util.List;

public class CompileError extends RuntimeException {
	
	private static final long serialVersionUID = 624098131567818737L;
	
	public String        file;
	public final int     line;
	public final int     charInLine;
	public final int     totalChar;
	public final String  offendingToken;
	private List<String> expectedTokens;// NOSONAR the list is serializable
	
	public CompileError(ErrorContext ctx, List<String> expectedTokens) {
		this(ctx.file, ctx.line, ctx.charInLine, ctx.totalChar, ctx.offendingToken(), expectedTokens, null);
	}
	
	public CompileError(ErrorContext ctx, String additionalMsg) {
		this(ctx.file, ctx.line, ctx.charInLine, ctx.totalChar, ctx.offendingToken(), null, additionalMsg);
	}
	
	public CompileError(ErrorContext ctx, List<String> expectedTokens, String additionalMsg) {
		this(ctx.file, ctx.line, ctx.charInLine, ctx.totalChar, ctx.offendingToken(), expectedTokens, additionalMsg);
	}
	
	public CompileError(String file, int line, int charInLine, int totalChar, String offendingToken,
		List<String> expectedTokens, String additionalMessage) {
		super(generateMessage(file, line, charInLine, totalChar, offendingToken, expectedTokens, additionalMessage));
		this.file = file;
		this.line = line;
		this.charInLine = charInLine;
		this.totalChar = totalChar;
		this.offendingToken = offendingToken;
		this.expectedTokens = expectedTokens == null ? List.of() : List.copyOf(expectedTokens);
	}
	
	private static String generateMessage(String file, int line, int charInLine, int totalChar, String offendingToken,
		List<String> expectedTokens, String additionalMessage) {
		StringBuilder b = new StringBuilder().append("illegal input");
		if ( file != null && !file.isEmpty() ) {
			b.append(" in file ").append(file);
		}
		if ( line != -1 ) {
			b.append(" at line ").append(line);
			if ( charInLine != -1 ) {
				b.append(':').append(charInLine);
			}
		}
		if ( totalChar != -1 ) {
			b.append(" at total char ").append(totalChar);
		}
		if ( additionalMessage != null && !additionalMessage.isEmpty() ) {
			b.append(": ").append(additionalMessage);
		}
		if ( offendingToken != null && !offendingToken.isEmpty() ) {
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
	
	
	public List<String> expectedTokens() {
		return expectedTokens;
	}
	
	
	public void expectedTokens(List<String> expectedTokens) {
		this.expectedTokens = expectedTokens == null ? null : List.copyOf(expectedTokens);
	}
	
}
