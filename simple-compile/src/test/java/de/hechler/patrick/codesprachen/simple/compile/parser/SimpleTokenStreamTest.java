package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.*;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertFalse;
import static de.hechler.patrick.zeugs.check.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
class SimpleTokenStreamTest {
	
	private static ErrorContext ctx(int line, int charInLine, int totalChar, String offendingToken) {
		ErrorContext ctx = new ErrorContext(null, null);
		ctx.line       = line;
		ctx.charInLine = charInLine;
		ctx.totalChar  = totalChar;
		ctx.setOffendingTokenCach(offendingToken);
		return ctx;
	}
	
	@Check
	void simpleCheck() throws IllegalArgumentException, IllegalAccessException {
		ByteArrayInputStream bais = new ByteArrayInputStream("dep hello\n  world\n\" \\t	HALLO SUPER tolle wElT\\u0020!\"".getBytes(StandardCharsets.UTF_8));
		SimpleTokenStream    sts  = new SimpleTokenStream(bais, null);
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
		String[] firstNames       = Arrays.copyOf((String[]) field.get(null), FIRST_DYN);
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
			String fname    = f.getName();
			int    fval     = f.getInt(null);
			String fvalName = name(fval);
			if ( fvalName.startsWith("[") && fvalName.startsWith("]") ) {
				if ( !fname.equals(fvalName.substring(1, fvalName.length() - 1)) ) {
					System.err.println("[ERROR]: " + fname + "                ".substring(fname.length()) + ": " + fval
						+ " : " + fvalName);
				}
			} else if ( Character.isLetter(fvalName.charAt(0))
				&& ( !fname.equals(fvalName.toUpperCase()) || !fvalName.equals(fname.toLowerCase()) )
				&& !"EOF".equals(fvalName) && !"INVALID".equals(fname) ) {//@formatter:off
				System.err.println("[ERROR]: " + fname + "                ".substring(fname.length()) + ": " + fval
					+ " : " + fvalName);
			}//@formatter:on
		}
		assertFalse(error);
	}
	
}
