package de.hechler.patrick.code.simple.ecl.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class SsfPresentationReconciler extends PresentationReconciler {
	
	private static Map<String,IToken> TOKENS;
	
	static {
		TOKENS = new HashMap<>();
		TOKENS.put(SsfPosUpdate.TOKEN_WHITESPACE, Token.WHITESPACE);
		TOKENS.put(SsfPosUpdate.TOKEN_COMMENT, token(127, 127, 127));
		TOKENS.put(SsfPosUpdate.TOKEN_OTHER_SYMBOL, token(0, 0, 0));
		TOKENS.put(SsfPosUpdate.TOKEN_ASM_BLOCK, token(0, 95, 95));
		TOKENS.put(SsfPosUpdate.TOKEN_CHARACTER, token(0, 191, 0));
		TOKENS.put(SsfPosUpdate.TOKEN_STRING, token(0, 191, 0));
		TOKENS.put(SsfPosUpdate.TOKEN_NUMBER, token(0, 0, 0));
		IToken _127_0_255 = token(127, 0, 255);
		TOKENS.put(SsfPosUpdate.KEYWORD_ASM, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_WHILE_IF_ELSE, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_CALL, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_FSTRUCT_STRUCT, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_MAIN_INIT, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_FUNC_AS_FUNC_ADDR, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_FUNC, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_DEP, _127_0_255);
		TOKENS.put(SsfPosUpdate.KEYWORD_TYPEDEF, _127_0_255);
		IToken _95_0_191 = token(95, 0, 191);
		TOKENS.put(SsfPosUpdate.KEYWORD_CONST, _95_0_191);
		TOKENS.put(SsfPosUpdate.KEYWORD_NOPAD, _95_0_191);
		TOKENS.put(SsfPosUpdate.KEYWORD_EXP, _95_0_191);
		
		TOKENS.put(SsfPosUpdate.TOKEN_PRIM_TYPE, token(63, 0, 159));
		TOKENS.put(SsfPosUpdate.DECL_TYPEDEF_NAME, token(127, 63, 191));
		TOKENS.put(SsfPosUpdate.TOKEN_DEFED_TYPE, token(127, 63, 191));
		
		TOKENS.put(SsfPosUpdate.DECL_PARAM_RESULT_VARIABLE_NAME, token(159, 159, 127));
		TOKENS.put(SsfPosUpdate.DECL_LOCAL_VARIABLE_NAME, token(127, 127, 63));
		TOKENS.put(SsfPosUpdate.REF_LOCAL_VARIABLE, token(127, 127, 63));
		
		TOKENS.put(SsfPosUpdate.DECL_GLOBAL_VARIABLE_NAME, token(63, 0, 127));
		TOKENS.put(SsfPosUpdate.REF_GLOBAL_VARIABLE, token(63, 0, 127));
		
		TOKENS.put(SsfPosUpdate.DECL_DEP_NAME, token(159, 0, 0));
		TOKENS.put(SsfPosUpdate.REF_GLOBAL_DEPENDENCY, token(159, 0, 0));
		
		TOKENS.put(SsfPosUpdate.DECL_FUNC_NAME, token(127, 63, 0));
		TOKENS.put(SsfPosUpdate.REF_GLOBAL_FUNCTION, token(127, 63, 0));
		
		TOKENS.put(SsfPosUpdate.REF_VALUE_REFERENCE_NAME, token(0, 63, 127));
	}
	
	private static IToken token(int r, int g, int b) {
		RGB rgb = new RGB(r, g, b);
		TextAttribute attr = new TextAttribute(new Color(Display.getCurrent(), rgb));
		Token tok = new Token(attr);
		return tok;
	}
	
	public static void setToken(String key, int r, int g, int b) {
		IToken tok = token(r, g, b);
		setToken(key, tok);
	}
	
	public static void setToken(String key, IToken tok) {
		if ( tok.isEOF() ) {
			throw new IllegalArgumentException("EOF token");
		}
		if ( !TOKENS.containsKey(key) ) {
			throw new IllegalArgumentException("unknown key: '" + key + "'");
		}
		TOKENS.put(key, tok);
	}
	
	public SsfPresentationReconciler() {
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new Scanner());
		this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}
	
	private static class Scanner implements ITokenScanner {
		
		private IDocument doc;
		private int       off;
		private int       len;
		private int       endOff;
		
		public Scanner() {
		}
		
		@Override
		public void setRange(IDocument document, int offset, int length) {
			this.doc = document;
			this.off = offset;
			this.len = 0;
			this.endOff = offset + length;
			final int maxLen = document.getLength();
			if ( endOff > maxLen ) {
				endOff = maxLen;
			}
		}
		
		@Override
		public IToken nextToken() {
			SsfPosUpdate spu = SsfPosUpdate.getSPU(this.doc);
			if ( spu == null ) {
				this.len = 0;
				System.err.println("SsfPosUpdate not found");
				return Token.EOF;
			}
			this.off += this.len;
			if ( this.off >= this.endOff ) {
				this.len = 0;
				return Token.EOF;
			}
			ITypedRegion part = spu.getPartition(this.off);
			int poff = part.getOffset();
			int plen = part.getLength();
			int pend = poff + plen;
			if ( pend > this.endOff ) {
				pend = this.endOff;
			}
			this.len = pend - this.off;
			String type = part.getType();
			IToken tok = TOKENS.get(type);
			Object data = tok.getData();
			if ( data instanceof TextAttribute ta ) {
				data = ta.getForeground() + " : " + ta;
			}
			System.out.println("token: " + this.off + ".." + ( this.off + this.len ) + " : " + data + " : " + type);
			return tok;
		}
		
		@Override
		public int getTokenOffset() {
			return this.off;
		}
		
		@Override
		public int getTokenLength() {
			return this.len;
		}
		
	}
	
}
