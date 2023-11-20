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
package de.hechler.patrick.codesprachen.simple.parser.error;

import java.util.function.Supplier;

public class ErrorContext {
	
	public static final ErrorContext NO_CONTEXT = new ErrorContext(null, null);
	
	public String                  file;      // NOSONAR
	public int                     line;      // NOSONAR
	public int                     charInLine;// NOSONAR
	public int                     totalChar; // NOSONAR
	private String                 offendingToken;
	private final Supplier<String> offendingTokenSup;
	
	public ErrorContext(String file, Supplier<String> offendingTokenSup) {
		this.file              = file;
		this.line              = -1;
		this.charInLine        = -1;
		this.totalChar         = -1;
		this.offendingTokenSup = offendingTokenSup;
	}
	
	public void setOffendingTokenCach(String offendingToken) {
		this.offendingToken = offendingToken;
	}
	
	public ErrorContext copy() {
		ErrorContext copy = new ErrorContext(this.file, this.offendingTokenSup);
		copy.charInLine     = this.charInLine;
		copy.line           = this.line;
		copy.totalChar      = this.totalChar;
		copy.offendingToken = this.offendingToken;
		return copy;
	}
	
	public String offendingToken() {
		String ot = this.offendingToken;
		if ( ot == null ) {
			Supplier<String> sub = this.offendingTokenSup;
			if ( sub != null ) {
				ot                  = sub.get();
				this.offendingToken = ot;
			}
		}
		return ot;
	}
	
	@Override
	public int hashCode() {
		final int prime  = 31;
		int       result = 1;
		result = prime * result + this.charInLine;
		result = prime * result + ( ( this.file == null ) ? 0 : this.file.hashCode() );
		result = prime * result + this.line;
		result = prime * result + ( ( this.offendingToken() == null ) ? 0 : this.offendingToken().hashCode() );
		result = prime * result + this.totalChar;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		ErrorContext other = (ErrorContext) obj;
		if ( this.charInLine != other.charInLine ) return false;
		if ( this.file == null ) {
			if ( other.file != null ) return false;
		} else if ( !this.file.equals(other.file) ) return false;
		if ( this.line != other.line ) return false;
		if ( this.offendingToken() == null ) {
			if ( other.offendingToken() != null ) return false;
		} else if ( !this.offendingToken().equals(other.offendingToken()) ) return false;
		return this.totalChar == other.totalChar;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrorContext [file=");
		builder.append(this.file);
		builder.append(", line=");
		builder.append(this.line);
		builder.append(", charInLine=");
		builder.append(this.charInLine);
		builder.append(", totalChar=");
		builder.append(this.totalChar);
		builder.append(", offendingToken()=");
		builder.append(offendingToken());
		builder.append("]");
		return builder.toString();
	}
	
}
