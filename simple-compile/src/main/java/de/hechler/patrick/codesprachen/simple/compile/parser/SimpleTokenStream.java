package de.hechler.patrick.codesprachen.simple.compile.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;

public class SimpleTokenStream {
	
	private final InputStream in;
	
	public SimpleTokenStream(InputStream in) {
		this.in = in.markSupported() ? in : new BufferedInputStream(in);
	}
	
	public static final int  EOF       = -1;
	public static final int  LARROW    = 5;
	public static final int  ME        = 5;
	public static final int  SEMI      = 5;
	public static final int  CONST     = 5;
	public static final int  EXP       = 5;
	public static final int  DEP       = 5;
	private static final int FIRST_DYN = 6;
	public static final int  NAME      = 6;
	public static final int  STRING    = 6;
	
	private static final String[] NAMES = { //
		"<--", // LARROW
		"<ME>", // ME
		";", // SEMI
		"const", // CONST
		"exp", // EXP
		"dep", // DEP
		"num", // NUM
		"unum", // UNUM
		"fpnum", // FPNUM
		"fpdword", // FPDWORD
		"dword", // DWORD
		"udword", // UDWORD
		"word", // WORD
		"uword", // UWORD
		"byte", // BYTE
		"ubyte", // UBYTE
		"char", // CHAR
		"struct", // STRUCT
		"nopad", // NOPAD
		",", // COMMA
		"#", // DIAMOND
		"[", // ARR_OPEN
		"]", // ARR_CLOSE
		"fstruct", // FSTRUCT
		"(", // SMALL_OPEN
		")", // SMALL_CLOSE
		"<", // LT
		">", // GT
		"?", // QUESTION
		":", // COLON
		"||", // BOOL_OR
		"&&", // BOOL_AND
		"^", // BIT_XOR
		"|", // BIT_OR
		"&", // BIT_AND
		"!=", // NOT_EQ
		"==", // EQ
		">=", // GE
		"<=", // LE
		"<<", // SHIFT_RIGTH
		">>", // SHIFT_LEFT_ARITMETIC
		">>>", // SHIFT_LEFT_LOGIC
		"+", // PLUS
		"-", // MINUS
		"*", // STAR
		"/", // DIV
		"%", // MOD
		"!", // BOOL_NOT
		"~", // BIT_NOT
		"[NAME]", // NAME
		"[STRING]", // STRING
		"[CHARACTER]", // CHARACTER
		"[NUMBER]", // NUMBER
	};
	
	static {
		String[] firstNames = Arrays.copyOf(NAMES, FIRST_DYN);
		String[] sortedFirstNames = firstNames.clone();
		Arrays.sort(sortedFirstNames);
		if ( !Arrays.equals(firstNames, sortedFirstNames) ) {
			for (String name : sortedFirstNames) {
				System.err.println("sorted name: " + name);
			}
			throw new AssertionError("my algo expects the static tokens to be sorted");
		}
		for (int i = 1; i < sortedFirstNames.length; i++) {
			if (sortedFirstNames[i-1].equals(sortedFirstNames[i])) {
				throw new AssertionError("duplicate token name detected: " + sortedFirstNames[i]);
			}
		}
	}
	
	private static final byte[] DYNAMIC_CONTINUE = { //
		-1, // "<", //
		-1, // "<--", //
		-1, // "<ME>", //
		-1, // "=", //
		-1, // ">", //
		NAME,// "dep", //
	};
	
	private static final byte[][] TOKENS;
	
	static {
		TOKENS = new byte[FIRST_DYN][];
		for (int i = FIRST_DYN; --i >= 0;) { // UTF-8 is a superset of ASCII
			TOKENS[i] = NAMES[i].getBytes(StandardCharsets.US_ASCII);
		}
	}
	
	private int totalChar;
	private int charInLine;
	private int line = 1;
	
	private int    tok = -1;
	private String dynTok;
	
	public void consume() {
		this.tok = -1;
		this.dynTok = null;
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
				in.mark(8); // maximum length until I now which token it is
				r = in.read();
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
			int high = TOKENS.length - 1;
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
			int val = TOKENS[mid][depth];
			if ( val < r ) {
				low = mid + 1;
			} else if ( val == r ) {
				low = calcNewLow(r, low, depth, mid);
				high = calcNewHigh(r, high, depth, mid);
				r = in.read();
				depth++;
				if ( r < 0 ) {
					if ( TOKENS[low].length == depth ) {
						this.charInLine += depth;
						this.totalChar += depth;
						this.tok = low;// if it is an exact match at the end of the file, no need to check dynamic
						return low;
					}
				}
				if ( TOKENS[low].length == depth ) {
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
		throw new CompileError((String) null, line, charInLine, totalChar, String.valueOf((char) r),
			(List<String>) null, (String) null);
	}
	
	private int returnToken(int r, int tok) throws IOException {
		int dynTok = DYNAMIC_CONTINUE[tok];
		int len = TOKENS[tok].length;
		if ( dynTok < 0 ) {
			this.charInLine += len;
			this.totalChar += len;
			this.tok = tok;
			return tok;
		}
		switch ( dynTok ) {
		case NAME -> {
			if ( noName(r) ) {
				in.reset();
				in.skipNBytes(len);
				this.tok = tok;
				return tok;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(NAMES[tok]).append((char) r);
			return returnName(sb);
		}
		default -> throw new AssertionError("illegal dynamic token index: " + dynTok);
		}
	}
	
	private int returnName(StringBuilder sb) throws IOException {
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
	
	private static int calcNewLow(int r, int low, int depth, int mid) {
		int oldlow = low;
		low = mid;
		while ( low > oldlow && TOKENS[low - 1].length > depth && TOKENS[low - 1][depth] == r ) {
			low--;
		}
		return low;
	}
	
	private static int calcNewHigh(int r, int high, int depth, int mid) {
		int oldhigh = high;
		high = mid;
		while ( high < oldhigh && TOKENS[high + 1].length > depth && TOKENS[high + 1][depth] == r ) {
			high++;
		}
		return high;
	}
	
	public static void main(String[] args) {
		ByteArrayInputStream bais = new ByteArrayInputStream("dep hello world".getBytes(StandardCharsets.UTF_8));
		SimpleTokenStream sts = new SimpleTokenStream(bais);
		System.out.println(sts.token() + " : " + NAMES[sts.tok] + " : " + sts.dynamicTokenSpecialText());
		System.out.println("  at line " + sts.line + ':' + sts.charInLine + " (total char: " + sts.totalChar + ')');
		sts.consume();
		System.out.println(sts.token() + " : " + NAMES[sts.tok] + " : " + sts.dynamicTokenSpecialText());
		System.out.println("  at line " + sts.line + ':' + sts.charInLine + " (total char: " + sts.totalChar + ')');
		sts.consume();
		System.out.println(sts.token() + " : " + NAMES[sts.tok] + " : " + sts.dynamicTokenSpecialText());
		System.out.println("  at line " + sts.line + ':' + sts.charInLine + " (total char: " + sts.totalChar + ')');
	}
	
}
