package de.hechler.patrick.code.simple.ecl.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;

public record FilePosition(int totalChar, int line, int charInLine) {
	
	public sealed interface FileRegion permits FileToken, DocumentTree {
		
		FilePosition start();
		
		FilePosition end();
		
		DocumentTree parent();
		
		int parentIndex();
		
	}
	
	public record FileToken(FilePosition start, int token, FilePosition end, DocumentTree parent, int parentIndex)
		implements FileRegion {
		
		public FileToken(FilePosition start, int token, FilePosition end, DocumentTree parent, int parentIndex) {
			this.start = start;
			this.token = token;
			this.end = end;
			this.parent = parent;
			this.parentIndex = parentIndex;
		}
		
		public FileToken(FilePosition start, int token, FilePosition end) {
			this(start, token, end, null, -1);
		}
		
		public FileToken initParent(DocumentTree parent, int parentIndex) {
			if ( this.parent != null ) {
				throw new IllegalStateException("parent already initilized");
			}
			if ( parent == null ) {
				throw new NullPointerException("can't initilize a null parent");
			}
			if ( parentIndex < 0 ) {
				throw new IllegalArgumentException("can't initilized a negative parent index");
			}
			return new FileToken(this.start, this.token, this.end, parent, parentIndex);
		}
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("Token [");
			b.append(this.token);
			b.append(" (");
			b.append(tokenName(this.token));
			b.append(") from ");
			b.append(this.start);
			b.append(" to ");
			b.append(this.end);
			b.append(']');
			return b.toString();
		}
		
	}
	
	private static Object tokenName(int token) {
		if ( token == SimpleTokenStream.INVALID ) {
			return "COMMENT";
		}
		return SimpleTokenStream.name(token);
	}
	
	public record FileState(FilePosition start, int state, FilePosition end, Object info) {
		
		public FileState finish(FilePosition end, int state, Object info) {
			if ( state != this.state ) {
				throw new AssertionError("state=" + state + ", this.state=" + this.state);
			}
			if ( this.end != null ) {
				throw new AssertionError("I am already finished!");
			}
			if ( this.state == -1 ) {
				throw new AssertionError("I don't yet have a decided state!");
			}
			return new FileState(this.start, state, end, info);
		}
		
		public FileState decide(int state) {
			if ( this.state != -1 ) {
				throw new AssertionError("I am already have a state!");
			}
			return new FileState(this.start, state, null, null);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("State [");
			builder.append(this.state);
			builder.append(" (");
			builder.append(stateName(this.state));
			builder.append(") from ");
			builder.append(this.start);
			if ( this.end != null ) {
				builder.append(" to ");
				builder.append(this.end);
			}
			if ( this.info != null ) {
				builder.append(" info=");
				builder.append(this.info);
			}
			builder.append(']');
			return builder.toString();
		}
		
	}
	
	private static String stateName(int state) {
		for (Field f : SimpleSourceFileParser.class.getFields()) {
			int m = f.getModifiers();
			if ( !Modifier.isStatic(m) ) {
				continue;
			}
			Class<?> t = f.getType();
			if ( t != Integer.TYPE ) {
				continue;
			}
			try {
				if ( f.getInt(null) != state ) {
					continue;
				}
			} catch ( IllegalArgumentException | IllegalAccessException e ) {
				throw new AssertionError(e);
			}
			String name = f.getName();
			if ( !name.startsWith("STATE_") ) {
				continue;
			}
			return name.substring("STATE_".length());
		}
		if ( state == -1 ) {
			return "UNKNOWN";
		}
		return "<UNKNOWN STATE: " + state + '>';
	}
	
	@Override
	public String toString() {
		return new StringBuilder()//
			.append(this.line)//
			.append(" : ").append(this.charInLine)//
			.append(" : ").append(this.totalChar).toString();
	}
	
}
