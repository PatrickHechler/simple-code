package de.hechler.patrick.codesprachen.simple.compile.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;

public class SimpleTokenStream {
	
	private final InputStream in;
	
	public SimpleTokenStream(InputStream in) {
		this.in = in.markSupported() ? in : new BufferedInputStream(in);
	}
	
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
	public static final int  STRING          = 60;
	public static final int  CHARACTER       = 61;
	public static final int  NUMBER          = 62;
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
		int len = NAMES[tok].length();
		if ( tok < MIN_NAME || tok > MAX_NAME || noName(r) ) {
			this.charInLine += len;
			this.totalChar += len;
			this.tok = tok;
			return tok;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(NAMES[tok]).append((char) r);
		return returnName(sb);
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
		SimpleTokenStream sts = new SimpleTokenStream(bais);
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
