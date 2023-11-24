package de.hechler.patrick.codesprachen.simple.parser;

import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.DEP;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.FIRST_DYN;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.STRING;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.name;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertFalse;
import static de.hechler.patrick.zeugs.check.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
class SimpleTokenStreamTest {
	
	private static ErrorContext ctx(int line, int charInLine, int totalChar, String offendingToken) {
		ErrorContext ctx = new ErrorContext(null, null);
		ctx.line = line;
		ctx.charInLine = charInLine;
		ctx.totalChar = totalChar;
		ctx.setOffendingTokenCach(offendingToken);
		return ctx;
	}
	
	@Check
	void simpleCheck() throws IllegalArgumentException, IllegalAccessException {
		ByteArrayInputStream bais = new ByteArrayInputStream(
			"dep hello\n  world\n\" \\t	HALLO SUPER tolle wElT\\u0020!\"".getBytes(StandardCharsets.UTF_8));
		SimpleTokenStream sts = new SimpleTokenStream(bais, null);
		assertEquals(DEP, sts.tok());
		assertThrows(AssertionError.class, () -> sts.dynTokSpecialText());
		assertThrows(AssertionError.class, () -> sts.consumeDynTokSpecialText());
		assertEquals(sts.ctx(), sts.ctx());
		assertEquals(ctx(1, 3, 3, "dep"), sts.ctx());
		sts.consume();
		assertEquals(NAME, sts.tok());
		assertEquals("hello", sts.dynTokSpecialText());
		assertEquals(ctx(1, 9, 9, "hello"), sts.ctx());
		sts.consume();
		assertEquals(NAME, sts.tok());
		assertEquals("world", sts.dynTokSpecialText());
		assertEquals(ctx(2, 7, 17, "world"), sts.ctx());
		sts.consume();
		assertEquals(STRING, sts.tok());
		assertEquals(" \t	HALLO SUPER tolle wElT\u0020!", sts.dynTokSpecialText());
		assertEquals(ctx(3, 35, 53, "\" \\t\\tHALLO SUPER tolle wElT !\""), sts.ctx());
	}
	
	@Check
	void checkNames() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field field = SimpleTokenStream.class.getDeclaredField("NAMES");
		field.setAccessible(true);
		String[] firstNames = Arrays.copyOf((String[]) field.get(null), FIRST_DYN);
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
	
	private static final Map<String,String> NO_CHAR_NAMES = new HashMap<>();
	
	static {
		NO_CHAR_NAMES.put("BOOL_NOT", "!");
		NO_CHAR_NAMES.put("NOT_EQ", "!=");
		NO_CHAR_NAMES.put("DIAMOND", "#");
		NO_CHAR_NAMES.put("MOD", "%");
		NO_CHAR_NAMES.put("BIT_AND", "&");
		NO_CHAR_NAMES.put("BOOL_AND", "&&");
		NO_CHAR_NAMES.put("SMALL_OPEN", "(");
		NO_CHAR_NAMES.put("SMALL_CLOSE", ")");
		NO_CHAR_NAMES.put("STAR", "*");
		NO_CHAR_NAMES.put("PLUS", "+");
		NO_CHAR_NAMES.put("COMMA", ",");
		NO_CHAR_NAMES.put("MINUS", "-");
		NO_CHAR_NAMES.put("DIV", "/");
		NO_CHAR_NAMES.put("COLON", ":");
		NO_CHAR_NAMES.put("SEMI", ";");
		NO_CHAR_NAMES.put("LT", "<");
		NO_CHAR_NAMES.put("LARROW", "<--");
		NO_CHAR_NAMES.put("SHIFT_LEFT", "<<");
		NO_CHAR_NAMES.put("LE", "<=");
		NO_CHAR_NAMES.put("ME", "<ME>");
		NO_CHAR_NAMES.put("EQ", "==");
		NO_CHAR_NAMES.put("GT", ">");
		NO_CHAR_NAMES.put("GE", ">=");
		NO_CHAR_NAMES.put("SHIFT_RIGTH", ">>");
		NO_CHAR_NAMES.put("QUESTION", "?");
		NO_CHAR_NAMES.put("ARR_OPEN", "[");
		NO_CHAR_NAMES.put("ARR_CLOSE", "]");
		NO_CHAR_NAMES.put("BIT_XOR", "^");
		NO_CHAR_NAMES.put("BLOCK_OPEN", "{");
		NO_CHAR_NAMES.put("BIT_OR", "|");
		NO_CHAR_NAMES.put("BOOL_OR", "||");
		NO_CHAR_NAMES.put("BLOCK_CLOSE", "}");
		NO_CHAR_NAMES.put("BIT_NOT", "~");
		
	}
	
	@Check
	void checkTokenFields() throws IllegalAccessException {
		boolean error = false;
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
			if ( fvalName.startsWith("[") && fvalName.endsWith("]") ) {
				if ( !fname.equals(fvalName.substring(1, fvalName.length() - 1)) ) {
					System.err.println("[ERROR]: " + fname + "                ".substring(fname.length()) + ": " + fval
						+ " : " + fvalName);
					error = true;
				}
			} else if ( Character.isLetter(fvalName.charAt(0))
				&& ( !fname.equals(fvalName.toUpperCase()) || !fvalName.equals(fname.toLowerCase()) )
				&& !"EOF".equals(fvalName) && !"INVALID".equals(fname) ) {//@formatter:off
				System.err.println("[ERROR]: " + fname + "                ".substring(fname.length()) + ": " + fval
					+ " : " + fvalName);
				error = true;
			} else if ( !Character.isLetter(fvalName.charAt(0)) && !NO_CHAR_NAMES.get(fname).equals(fvalName)) {
				System.err.println("[ERROR]: " + fname + "                ".substring(fname.length()) + ": " + fval
					+ " : " + fvalName);
				error = true;
			}//@formatter:on
		}
		assertFalse(error);
	}
	
}
