package de.hechler.patrick.codesprachen.simple.compile.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;

public class SimpleTokenStream {
	
	public static final int  EOF             = -1;
	public static final int  BOOL_NOT        = 0;
	public static final int  NOT_EQ          = 1;
	public static final int  DIAMOND         = 2;
	public static final int  MOD             = 3;
	public static final int  BIT_AND         = 4;
	public static final int  BOOL_AND        = 5;
	public static final int  SMALL_OPEN      = 6;
	public static final int  SMALL_CLOSE     = 7;
	public static final int  STAR            = 8;
	public static final int  PLUS            = 9;
	public static final int  COMMA           = 10;
	public static final int  MINUS           = 11;
	public static final int  DIV             = 12;
	public static final int  COLON           = 13;
	public static final int  SEMI            = 14;
	public static final int  LT              = 15;
	public static final int  LARROW          = 16;
	public static final int  SHIFT_LEFT      = 17;
	public static final int  LE              = 18;
	public static final int  ME              = 19;
	public static final int  EQ              = 20;
	public static final int  GT              = 21;
	public static final int  GE              = 22;
	public static final int  SHIFT_RIGTH_ARI = 23;
	public static final int  SHIFT_RIGTH_LOG = 24;
	public static final int  QUESTION        = 25;
	public static final int  ARR_OPEN        = 26;
	public static final int  ARR_CLOSE       = 27;
	public static final int  BIT_XOR         = 28;
	public static final int  ASM             = 29;
	private static final int MIN_NAME        = ASM;
	public static final int  BYTE            = 30;
	public static final int  CALL            = 31;
	public static final int  CHAR            = 32;
	public static final int  CONST           = 33;
	public static final int  DEP             = 34;
	public static final int  DWORD           = 35;
	public static final int  EXP             = 36;
	public static final int  FPDWORD         = 37;
	public static final int  FPNUM           = 38;
	public static final int  FSTRUCT         = 39;
	public static final int  FUNC            = 40;
	public static final int  IF              = 41;
	public static final int  INIT            = 42;
	public static final int  MAIN            = 43;
	public static final int  NOPAD           = 44;
	public static final int  NUM             = 45;
	public static final int  STRUCT          = 46;
	public static final int  TYPEDEF         = 47;
	public static final int  UBYTE           = 48;
	public static final int  UDWORD          = 49;
	public static final int  UNUM            = 50;
	public static final int  UWORD           = 51;
	public static final int  WHILE           = 52;
	public static final int  WORD            = 53;
	private static final int MAX_NAME        = WORD;
	public static final int  BLOCK_OPEN      = 54;
	public static final int  BIT_OR          = 55;
	public static final int  BOOL_OR         = 56;
	public static final int  BLOCK_CLOSE     = 57;
	public static final int  BIT_NOT         = 58;
	private static final int FIRST_DYN       = 59;
	public static final int  NAME            = FIRST_DYN;
	public static final int  NUMBER          = 60;
	public static final int  STRING          = 61;
	public static final int  CHARACTER       = 62;
	public static final int  ASM_BLOCK       = 63;
	
	private static final String[] NAMES = { // @formatter:off
		"!",           // BOOL_NOT
		"!=",          // NOT_EQ
		"#",           // DIAMOND
		"%",           // MOD
		"&",           // BIT_AND
		"&&",          // BOOL_AND
		"(",           // SMALL_OPEN
		")",           // SMALL_CLOSE
		"*",           // STAR
		"+",           // PLUS
		",",           // COMMA
		"-",           // MINUS
		"/",           // DIV
		":",           // COLON
		";",           // SEMI
		"<",           // LT
		"<--",         // LARROW
		"<<",          // SHIFT_LEFT
		"<=",          // LE
		"<ME>",        // ME
		"==",          // EQ
		">",           // GT
		">=",          // GE
		">>",          // SHIFT_RIGTH_ARITMETIC
		">>>",         // SHIFT_RIGTH_LOGIC
		"?",           // QUESTION
		"[",           // ARR_OPEN
		"]",           // ARR_CLOSE
		"^",           // BIT_XOR
		"asm",         // ASM
		"byte",        // BYTE
		"call",        // CALL
		"char",        // CHAR
		"const",       // CONST
		"dep",         // DEP
		"dword",       // DWORD
		"exp",         // EXP
		"fpdword",     // FPDWORD
		"fpnum",       // FPNUM
		"fstruct",     // FSTRUCT
		"func",        // FUNC
		"if",          // IF
		"init",        // INIT
		"main",        // MAIN
		"nopad",       // NOPAD
		"num",         // NUM
		"struct",      // STRUCT
		"typedef",     // TYPEDEF
		"ubyte",       // UBYTE
		"udword",      // UDWORD
		"unum",        // UNUM
		"uword",       // UWORD
		"while",       // WHILE
		"word",        // WORD
		"{",           // BLOCK_OPEN
		"|",           // BIT_OR
		"||",          // BOOL_OR
		"}",           // BLOCK_CLOSE
		"~",           // BIT_NOT
		"[NAME]",      // NAME
		"[STRING]",    // STRING
		"[CHARACTER]", // CHARACTER
		"[NUMBER]",    // NUMBER
		"[ASM_BLOCK]", // ASM_BLOCK
	};//@formatter:on
	
	public static String name(int token) {
		if ( token < 0 ) {
			if ( token == -1 ) return "EOF";
			return "INVALID: <" + token + ">";
		}
		if ( token >= NAMES.length ) return "INVALID: <" + token + ">";
		return NAMES[token];
	}
	
	private int totalChar;
	private int charInLine;
	private int line = 1;
	
	private int    tok = -1;
	private String dynTok;
	
	private final InputStream in;
	public final String       file;
	
	public SimpleTokenStream(InputStream in, String file) {
		this.in = in.markSupported() ? in : new BufferedInputStream(in);
		this.file = file;
	}
	
	public void consume() {
		this.tok = -1;
		this.dynTok = null;
	}
	
	public String offendingToken() {
		if ( this.dynTok != null ) {
			switch ( this.tok ) {
			case NAME, NUMBER:
				return this.dynTok;
			case STRING:
				return "\"" + this.dynTok.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r")
					.replace("\n", "\\n").replace("\0", "\\0") + "\"";
			case CHAR:
				switch ( this.dynTok ) {
				case "\\":
					return "'\\\\'";
				case "'":
					return "'\\''";
				case "\t":
					return "'\\t'";
				case "\r":
					return "'\\r'";
				case "\n":
					return "'\\n'";
				case "\0":
					return "'\\0'";
				default:
					return "'" + this.dynTok + "'";
				}
			case ASM_BLOCK:
				return ":::" + this.dynTok + ">>>";
			}
		}
		return name(this.tok);
	}
	
	public String dynamicTokenSpecialText() {
		String dt = this.dynTok;
		assert dt != null : "dynamicTokenSpecialText called, but there is no dynamic token";
		return dt;
	}
	
	public int token() {
		if ( tok >= 0 ) {
			return tok;
		}
		try {
			int r;
			while ( true ) {
				in.mark(16); // maximum length until I now which token it is
				r = in.read(); // currently 8 should also work
				if ( r == -1 ) return -1;
				if ( !Character.isWhitespace(r) ) {
					break;
				}
				if ( r == '\n' ) {
					this.line++;
					this.charInLine = 0;
				} else {
					this.charInLine++;
				}
				this.totalChar++;
			}
			int low = 0;
			int high = FIRST_DYN - 1;
			return findToken(r, low, high);
		} catch ( IOException e ) {
			throw new IllegalStateException(e);
		}
	}
	
	private int findToken(int r, int low, int high) throws IOException {
		int depth = 0;
		int errRes = -1;
		while ( true ) {
			int mid = ( low + high ) >>> 1;
			int val = NAMES[mid].charAt(depth);
			if ( val < r ) {
				low = mid + 1;
			} else if ( val == r ) {
				low = calcNewLow(r, low, depth, mid);
				high = calcNewHigh(r, high, depth, mid);
				r = in.read();
				depth++;
				if ( r < 0 ) {
					if ( NAMES[low].length() == depth ) {
						this.charInLine += depth;
						this.totalChar += depth;
						this.tok = low;// if it is an exact match at the end of the file, no need to check dynamic
						return low;
					}
				}
				if ( NAMES[low].length() == depth ) {
					if ( low == high ) return returnToken(r, low);
					errRes = low;
					low++;
				} else continue; // low <= high
			} else {// value > r
				high = mid - 1;
			}
			if ( low > high ) {
				if ( errRes != -1 ) {
					return returnToken(r, errRes);
				}
				return returnErrToken();
			}
		}
	}
	
	private int returnErrToken() throws IOException {
		in.reset();
		int r = in.read();
		// assert r >= 0
		if ( nameStart(r) ) {
			return returnName(new StringBuilder().append((char) r));
		}
		if ( numberStart(r) ) {
			return returnNumber(new StringBuilder().append((char) r));
		}
		if ( r == '"' ) {
			return returnString(new StringBuilder());
		}
		if ( r == '\'' ) {
			return returnChar();
		}
		throw new CompileError(this.file, line, charInLine, totalChar, String.valueOf((char) r), null, null);
	}
	
	private int returnToken(int r, int tok) throws IOException {
		int len = NAMES[tok].length();
		if ( tok == MINUS && numberStartAfterMinus(r) ) {
			StringBuilder sb = new StringBuilder();
			sb.append('-').append((char) r);
			return returnNumber(sb);
		} else if ( tok >= MIN_NAME && tok <= MAX_NAME && !noName(r) ) {
			StringBuilder sb = new StringBuilder();
			sb.append(NAMES[tok]).append((char) r);
			return returnName(sb);
		} else if ( tok == COLON && r == ':' && in.read() == ':' ) {
			StringBuilder sb = new StringBuilder();
			return returnAsm(sb);
		} else {
			in.reset();
			in.skipNBytes(len);
			this.charInLine += len;
			this.totalChar += len;
			this.tok = tok;
			return tok;
		}
	}
	
	private int returnNumber(StringBuilder sb) throws IOException {
		byte[] bytes = new byte[8];
		if ( sb.length() >= 2 ) return readDecimal(bytes, sb, sb.indexOf(".") != -1);
		switch ( sb.charAt(0) ) {
		case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9':
		case '-':
			return readDecimal(bytes, sb, false);
		case '.':
			return readDecimal(bytes, sb, true);
		case 'N': {
			int r = in.read();
			switch ( r ) {
			case 'D':
				return readNumberWithPrefix(bytes, sb.append('D'), "NDEC-", 10);
			case 'H':
				return readNumberWithPrefix(bytes, sb.append('H'), "NHEX-", 16);
			case 'B':
				return readNumberWithPrefix(bytes, sb.append('B'), "NBIN-", 2);
			case 'O':
				return readNumberWithPrefix(bytes, sb.append('O'), "NOCT-", 8);
			case -1:
				throw new CompileError(file, line, charInLine, totalChar, "N", null, "expected more input");
			default:
				throw new CompileError(file, line, charInLine, totalChar, "N", null, null);
			}
		}
		case 'D':
			return readNumberWithPrefix(bytes, sb, "DEC-", 10);
		case 'U':
			return readNumberWithPrefix(bytes, sb, "UHEX-", 16);
		case 'H':
			return readNumberWithPrefix(bytes, sb, "HEX-", 16);
		case 'B':
			return readNumberWithPrefix(bytes, sb, "BIN-", 2);
		case 'O':
			return readNumberWithPrefix(bytes, sb, "OCT-", 8);
		default:
			throw new AssertionError("illegal StringBuilder start: " + sb);
		}
	}
	
	private int readNumberWithPrefix(byte[] bytes, StringBuilder sb, String prefix, int number) throws IOException {
		while ( true ) {
			in.mark(8);
			int r = in.readNBytes(bytes, 0, 8);
			for (int i = 0; i < r; i++) {
				if ( sb.length() < prefix.length() ) {
					if ( bytes[i] != prefix.charAt(sb.length()) ) {
						throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
							sb.append((char) bytes[i]).toString(), null, "invalid number prefix");
					}
				} else if ( invalidNumber(bytes[i], number) ) {
					int val = bytes[i];
					switch ( val ) {
					case 'S', 's', 'U', 'u':
						sb.append(val);
						if ( ++i >= r ) val = in.read();
						else val = bytes[i];
					}
					switch ( val ) {
					case 'Q', 'q', 'N', 'n', 'D', 'd', 'W', 'w', 'B', 'b':
						sb.append(val);
					}
					in.reset();
					in.skipNBytes(i);
					this.dynTok = sb.toString();
					this.charInLine += this.dynTok.length();
					this.totalChar += this.dynTok.length();
					this.tok = NUMBER;
					return NUMBER;
				}
				sb.append((char) bytes[i]);
			}
			if ( r == 0 ) {
				if ( sb.length() <= prefix.length() ) {
					throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, sb.toString(), null,
						"reached EOF too early, the number after the prefix is missing");
				}
				this.dynTok = sb.toString();
				this.charInLine += this.dynTok.length();
				this.totalChar += this.dynTok.length();
				this.tok = NUMBER;
				return NUMBER;
			}
		}
	}
	
	private static boolean invalidNumber(byte num, int system) {
		if ( num < '0' ) return true;
		if ( system <= 10 ) {
			if ( num <= '0' + system ) return false;
			return true;
		}
		if ( num <= '9' ) return false;
		if ( num < 'A' ) return true;
		if ( num < 'A' + system - 10 ) return false;
		if ( num < 'a' ) return true;
		if ( num < 'a' + system - 10 ) return false;
		return true;
	}
	
	private int readDecimal(byte[] bytes, StringBuilder sb, boolean alreadyDot) throws IOException {
		while ( true ) {
			in.mark(8);
			int r = in.readNBytes(bytes, 0, 8);
			for (int i = 0; i < r; i++) {
				if ( bytes[i] < '0' || bytes[i] > '9' ) {
					if ( bytes[i] == '.' && !alreadyDot ) {
						alreadyDot = true;
					} else {
						switch (bytes[i]) {
						case 'Q', 'q', 'N', 'n', 'D', 'd';
						}
						this.dynTok = sb.toString();
						if ( this.dynTok.length() == 1 && ".".equals(this.dynTok) ) {
							this.dynTok = null; // needs to be changed when there is a . token
							throw new CompileError(file, line, charInLine, totalChar, sb.toString(), null, ".");
						} else if ( this.dynTok.length() == 2 && "-.".equals(this.dynTok) ) {
							this.charInLine++; // this will soon fail, because there is no . token
							this.totalChar++;
							this.dynTok = null;
							this.tok = MINUS;
							return MINUS;
						}
						in.reset();
						in.skipNBytes(i);
						this.charInLine += this.dynTok.length();
						this.totalChar += this.dynTok.length();
						this.tok = NUMBER;
						return NUMBER;
					}
				}
				sb.append((char) bytes[i]);
			}
		}
	}
	
	private int returnChar() throws IOException {
		byte[] bytes = new byte[4];
		in.mark(4);
		int r = in.readNBytes(bytes, 0, 4);
		if ( r < 2 ) {
			throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, "\'", null,
				"reached EOF too early, character could not be closed");
		}
		int off = 1;
		switch ( bytes[0] ) {
		case '\n', '\r' -> {
			throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, "\'" + (char) r, null,
				"line seperator inside of a character");
		}
		case '\'' -> {
			throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, "''", null,
				"character closed before a character occured");
		}
		case '\\' -> {
			off = 2;
			switch ( bytes[1] ) {
			case '0' -> this.dynTok = "\0";
			case 'n' -> this.dynTok = "\n";
			case 'r' -> this.dynTok = "\r";
			case 't' -> this.dynTok = "\t";
			case '\'' -> this.dynTok = "\'";
			default -> throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, "'\\", null,
				"invalid character escape");
			}
		}
		default -> {
			ByteBuffer bb = ByteBuffer.wrap(bytes, 0, 1);
			CharBuffer cb = StandardCharsets.UTF_8.newDecoder().replaceWith("--")
				.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)
				.decode(bb);
			if ( cb.remaining() != 1 ) {
				throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, "'", null,
					"invalid character sequence (maybe a non single byte character)");
			}
			this.dynTok = Character.toString(cb.get(0));
		}
		}
		if ( bytes[off] != '\'' ) {
			throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
				"'" + ( off == 2 ? "\\" + (char) bytes[1] : (char) bytes[0] ), null, "character not directly closed");
		}
		in.reset();
		in.mark(off + 1);
		this.charInLine += off + 2;
		this.totalChar += off + 2;
		this.tok = CHARACTER; // dynTok is already set
		return CHARACTER;
	}
	
	private int returnString(StringBuilder sb) throws IOException {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		byte[] bytes = new byte[32];
		char[] chars = new char[32];
		ByteBuffer bbuf = ByteBuffer.wrap(bytes);
		CharBuffer cbuf = CharBuffer.wrap(chars);
		int len = 2;
		while ( true ) {
			in.mark(32);
			int r = in.readNBytes(bytes, bbuf.position(), 32 - bbuf.position());
			CoderResult cr = decoder.decode(bbuf, cbuf, true);
			sb.append(chars, 0, cbuf.position());
			checkInvalid("string", "\"", sb, cr);
			int sbi = 0;
			cbuf.position(0);
			for (int i = 0; i < 32; i++) {
				char c = chars[i];
				switch ( c ) {
				case '"' -> {
					len += i;
					in.reset();
					in.skipNBytes(i + 1);
					this.charInLine = len;
					this.totalChar += len;
					this.dynTok = sb.toString();
					this.tok = STRING;
					return STRING;
				}
				case '\n', '\r' -> {
					throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
						sb.insert(0, '"').append(chars, sbi, i).toString(), null, "line seperator inside of a string");
				}
				case '\\' -> {
					if ( i == 31 ) {
						if ( sbi == 0 ) {
							sbi++;
							sb.append(chars[0]);
						}
						chars[0] = '\\';// temporary remove the \ from the string builder
						sb.replace(sb.length() - 1, sb.length(), "");
						cbuf.position(1);
					} else {
						sb.append(chars, sbi, i - sbi);
						switch ( chars[i + 1] ) {
						case 'u' -> {
							if ( i >= 27 ) {
								if ( sbi <= 6 ) {
									sbi = 6;
									sb.append(chars, 0, 6);
								}
								System.arraycopy(chars, i, chars, 0, r - i);
								cbuf.position(r - i);
							}
							int val = parseBSUHex(sb, chars[i + 2]) | parseBSUHex(sb, chars[i + 3]) << 4;
							val |= ( parseBSUHex(sb, chars[i + 4]) << 8 ) | ( parseBSUHex(sb, chars[i + 5]) << 12 );
							sb.append((char) val);
							sbi = i + 6;
						}
						case '\\', '"' -> {
							sbi = i + 1;// simply include those in the next append
						}
						case '0' -> {
							sbi = i + 2;
							sb.append('\0');
						}
						case 'n' -> {
							sbi = i + 2;
							sb.append('\n');
						}
						case 'r' -> {
							sbi = i + 2;
							sb.append('\r');
						}
						case 't' -> {
							sbi = i + 2;
							sb.append('\t');
						}
						default -> throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
							sb.insert(0, '"').append(chars, sbi, i).toString(), null, "invalid string escape");
						}
					}
				}
				default -> {}
				}
			}
			if ( bbuf.position() != r ) {
				System.arraycopy(bytes, bbuf.position(), bytes, 0, r - bbuf.position());
			} else {
				bbuf.position(0);
			}
			len += r;
			if ( r < 32 ) {
				throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
					sb.insert(0, '"').toString(), null, "the string does not end!");
			}
		}
	}
	
	private int parseBSUHex(StringBuilder sb, char c) {
		if ( c < '0' ) {
			invalidBSUFormat(sb);
		}
		if ( c <= '9' ) {
			return c - '0';
		}
		if ( c < 'A' ) {
			invalidBSUFormat(sb);
		}
		if ( c <= 'F' ) {
			return c - 'A';
		}
		if ( c < 'a' ) {
			invalidBSUFormat(sb);
		}
		if ( c <= 'f' ) {
			return c - 'a';
		} // just make the compiler happy
		throw invalidBSUFormat(sb);
	}
	
	private CompileError invalidBSUFormat(StringBuilder sb) {
		throw new CompileError(this.file, this.line, this.charInLine, this.totalChar, sb.insert(0, '"').toString(),
			null, "invalid \\u formatting");
	}
	
	private int returnAsm(StringBuilder sb) throws IOException {
		int len = 6 + sb.length(); // directly include the start and end tokens
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		byte[] bytes = new byte[128];
		char[] chars = new char[128];
		ByteBuffer bbuf = ByteBuffer.wrap(bytes);
		CharBuffer cbuf = CharBuffer.wrap(chars);
		int lines = 0;
		int lll = this.charInLine;
		while ( true ) {
			in.mark(128);
			int r = in.readNBytes(bytes, bbuf.position(), 128 - bbuf.position());
			int steps = bytes.length - 2;
			for (int i = 0; i < steps; i++) {
				if ( bytes[i] == '>' && bytes[i + 1] == '>' && bytes[i + 2] == '>' ) {
					finishAppend("asm block", ":::", sb, decoder, chars, bbuf, cbuf);
					len += i;
					in.reset();
					in.skipNBytes(i + 3);
					this.line += lines;
					this.charInLine = len - lll;
					this.totalChar += len;
					this.dynTok = sb.toString();
					this.tok = ASM_BLOCK;
					return ASM_BLOCK;
				} else if ( bytes[i] == '\n' ) {
					lines++;
					lll = len;
				}
			}
			if ( r == 128 && bytes[127] == '>' ) {
				bbuf.limit(bytes[126] == '>' ? 126 : 127);
			} else {
				bbuf.limit(r);
			}
			CoderResult cr = decoder.decode(bbuf, cbuf, true);
			sb.append(chars, 0, cbuf.position());
			checkInvalid("asm block", ":::", sb, cr);
			if ( bbuf.position() != r ) { // check if a code point (or trailing > chars) is between two blocks
				System.arraycopy(bytes, bbuf.position(), bytes, 0, r - bbuf.position());
			} else {
				bbuf.position(0);
			}
			cbuf.position(0);
			len += r;
			if ( r < 128 ) throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
				sb.insert(0, ":::").toString(), null, "the asm block does not end!");
		}
	}
	
	private void finishAppend(String token, String insert, StringBuilder sb, CharsetDecoder decoder, char[] chars,
		ByteBuffer bbuf, CharBuffer cbuf) {
		CoderResult cr = decoder.decode(bbuf, cbuf, true);
		sb.append(chars, 0, cbuf.position());
		checkInvalid(token, insert, sb, cr);
		while ( true ) {
			cbuf.position(0);
			cr = decoder.flush(cbuf);
			sb.append(chars, 0, cbuf.position());
			checkInvalid(token, insert, sb, cr);
			if ( cr.isUnderflow() ) break;
		}
	}
	
	private void checkInvalid(String token, String insert, StringBuilder sb, CoderResult cr) {
		if ( cr.isError() ) {
			throw new CompileError(this.file, this.line, this.charInLine, this.totalChar,
				sb.insert(0, insert).toString(), null, "the " + token + " contains invalid UTF-8 characters!");
		}
	}
	
	private int returnName(StringBuilder sb) throws IOException {
		// no need for a decoder: name only allows ASCII characters
		byte[] bytes = new byte[16];
		int len = sb.length();
		while ( true ) {
			in.mark(16);
			int r = in.readNBytes(bytes, 0, 16);
			if ( r <= 0 ) {
				this.charInLine += len;
				this.totalChar += len;
				this.dynTok = sb.toString();
				this.tok = NAME;
				return NAME;
			}
			for (int i = 0; i < r; i++) {
				if ( noName(bytes[i]) ) {
					in.reset();
					in.skipNBytes(i);
					len += i;
					this.charInLine += len;
					this.totalChar += len;
					this.dynTok = sb.toString();
					this.tok = NAME;
					return NAME;
				}
				sb.append((char) bytes[i]);
			}
		}
	}
	
	private static boolean noName(int r) { // 0-9..A-Z.._..a-z
		return ( r < '0' ) || ( r > '9' && r < 'A' ) || ( r > 'Z' && r != '_' && r < 'a' ) || ( r > 'z' );
	}
	
	private static boolean nameStart(int r) { // A-Z..a-z
		return ( r >= 'A' && r <= 'Z' ) || ( r >= 'a' && r <= 'z' );
	}
	
	private static boolean numberStart(int r) {
		switch ( r ) {
		case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9':
		case 'D', 'N', 'H', 'U', 'B', 'O':
		case '.', '-':
			return true;
		default:
			return false;
		}
	}
	
	private static boolean numberStartAfterMinus(int r) {
		return r >= '0' && r <= '9';
	}
	
	private static int calcNewLow(int r, int low, int depth, int mid) {
		int oldlow = low;
		low = mid;
		while ( low > oldlow && NAMES[low - 1].length() > depth && NAMES[low - 1].charAt(depth) == r ) {
			low--;
		}
		return low;
	}
	
	private static int calcNewHigh(int r, int high, int depth, int mid) {
		int oldhigh = high;
		high = mid;
		while ( high < oldhigh && NAMES[high + 1].length() > depth && NAMES[high + 1].charAt(depth) == r ) {
			high++;
		}
		return high;
	}
	
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException {
		ByteArrayInputStream bais = new ByteArrayInputStream("dep hello world".getBytes(StandardCharsets.UTF_8));
		SimpleTokenStream sts = new SimpleTokenStream(bais, null);
		System.out.println(sts.token() + " : " + name(sts.tok) + " : " + sts.dynamicTokenSpecialText());
		System.out.println("  at line " + sts.line + ':' + sts.charInLine + " (total char: " + sts.totalChar + ')');
		sts.consume();
		System.out.println(sts.token() + " : " + name(sts.tok) + " : " + sts.dynamicTokenSpecialText());
		System.out.println("  at line " + sts.line + ':' + sts.charInLine + " (total char: " + sts.totalChar + ')');
		sts.consume();
		System.out.println(sts.token() + " : " + name(sts.tok) + " : " + sts.dynamicTokenSpecialText());
		System.out.println("  at line " + sts.line + ':' + sts.charInLine + " (total char: " + sts.totalChar + ')');
		for (Field f : SimpleTokenStream.class.getDeclaredFields()) {
			int fmods = f.getModifiers();
			if ( !Modifier.isFinal(fmods) || !Modifier.isStatic(fmods) || !Modifier.isPublic(fmods) ) {
				continue;
			}
			if ( f.getType() != Integer.TYPE ) {
				continue;
			}
			String fname = f.getName();
			int fval = f.getInt(null);
			String fvalName = name(fval);
			if ( Character.isLetter(fvalName.charAt(0)) && ( !fname.equals(fvalName.toUpperCase())
				|| !fvalName.equals(fname.toLowerCase()) && !"EOF".equals(fvalName) ) ) {
				System.err.println("[ERROR]: " + fname + "                ".substring(fname.length()) + ": " + fval
					+ " : " + fvalName);
			}
		}
		String[] firstNames = Arrays.copyOf(NAMES, FIRST_DYN);
		String[] sortedFirstNames = firstNames.clone();
		Arrays.sort(sortedFirstNames);
		if ( !Arrays.equals(firstNames, sortedFirstNames) ) {
			System.err.println("the algo expects the static tokens to be sorted!");
			System.err.println("the correct order whould be:");
			for (String name : sortedFirstNames) {
				System.err.println(name);
			}
		}
		for (int i = 1; i < sortedFirstNames.length; i++) {
			if ( sortedFirstNames[i - 1].equals(sortedFirstNames[i]) ) {
				System.err.println("duplicate token name detected: " + sortedFirstNames[i]);
			}
		}
	}
	
}
