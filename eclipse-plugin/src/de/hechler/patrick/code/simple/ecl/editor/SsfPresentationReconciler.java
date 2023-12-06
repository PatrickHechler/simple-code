package de.hechler.patrick.code.simple.ecl.editor;

import org.eclipse.jface.text.IDocument;
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
	
	public SsfPresentationReconciler() {
//		setScanner(0,0,0, PresentationReconciler.);
		setScanner(Token.WHITESPACE, SsfDocumentPartitioner.TOKEN_WHITESPACE);
		setScanner(128, 128, 128, SsfDocumentPartitioner.TOKEN_COMMENT);
		setScanner(0, 0, 0, SsfDocumentPartitioner.TOKEN_OTHER_SYMBOL);
		setScanner(0, 64, 64, SsfDocumentPartitioner.TOKEN_ASM_BLOCK);
		setScanner(0, 128, 0, SsfDocumentPartitioner.TOKEN_CHARACTER);
		setScanner(0, 96, 0, SsfDocumentPartitioner.TOKEN_STRING);
		setScanner(0, 0, 96, SsfDocumentPartitioner.TOKEN_NUMBER);
		setScanner(64, 64, 0, SsfDocumentPartitioner.TOKEN_PRIM_TYPE);
		setScanner(64, 128, 0, SsfDocumentPartitioner.TOKEN_DEFED_TYPE);
		setScanner(0, 64, 128, SsfDocumentPartitioner.KEYWORD_ASM, SsfDocumentPartitioner.KEYWORD_WHILE_IF_ELSE,
			SsfDocumentPartitioner.KEYWORD_CALL, SsfDocumentPartitioner.KEYWORD_FSTRUCT_STRUCT,
			SsfDocumentPartitioner.KEYWORD_CONST, SsfDocumentPartitioner.KEYWORD_MAIN_INIT,
			SsfDocumentPartitioner.KEYWORD_NOPAD, SsfDocumentPartitioner.KEYWORD_EXP,
			SsfDocumentPartitioner.KEYWORD_FUNC_AS_FUNC_ADDR, SsfDocumentPartitioner.KEYWORD_FUNC,
			SsfDocumentPartitioner.KEYWORD_DEP, SsfDocumentPartitioner.KEYWORD_TYPEDEF);
		setScanner(32, 32, 32, SsfDocumentPartitioner.DECL_TYPEDEF_NAME);
		setScanner(64, 0, 64, SsfDocumentPartitioner.DECL_DEP_NAME);
		setScanner(0, 0, 64, SsfDocumentPartitioner.DECL_FUNC_NAME);
		setScanner(32, 16, 32, SsfDocumentPartitioner.DECL_PARAM_RESULT_VARIABLE_NAME);
		setScanner(16, 16, 64, SsfDocumentPartitioner.DECL_LOCAL_VARIABLE_NAME);
		setScanner(0, 64, 128, SsfDocumentPartitioner.DECL_GLOBAL_VARIABLE_NAME);
		setScanner(64, 64, 64, SsfDocumentPartitioner.REF_VALUE_REFERENCE_NAME);
		setScanner(8, 8, 32, SsfDocumentPartitioner.REF_LOCAL_VARIABLE);
		setScanner(64, 0, 128, SsfDocumentPartitioner.REF_GLOBAL_FUNCTION);
		setScanner(128, 0, 64, SsfDocumentPartitioner.REF_GLOBAL_DEPENDENCY);
		setScanner(64, 0, 64, SsfDocumentPartitioner.REF_GLOBAL_VARIABLE);
	}
	
	private final void setScanner(int r, int g, int b, String type) {
		setScanner(r, g, b, type, (String[]) null);
	}
	
	private final void setScanner(int r, int g, int b, String type, String... otherTypes) {
		RGB rgb = new RGB(255, 0, 0);
		TextAttribute attr = new TextAttribute(new Color(Display.getCurrent(), rgb));
		Token tok = new Token(attr);
		setScanner(tok, type);
		if ( otherTypes != null ) {
			for (String otherType : otherTypes) {
				setScanner(r, g, b, otherType);
			}
		}
	}
	
	private final void setScanner(IToken tok, String type) {
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new Scanner(tok));
		this.setDamager(dr, type);
		this.setRepairer(dr, type);
	}
	
	private static class Scanner implements ITokenScanner {
		
		private final IToken tok;
		
		private IToken t;
		private int    off;
		private int    len;
		
		public Scanner(IToken tok) {
			this.tok = tok;
		}
		
		@Override
		public void setRange(IDocument document, int offset, int length) {
			this.off = offset;
			this.len = length;
			final int maxLen = document.getLength();
			this.t = this.tok;
			if ( offset > maxLen ) {
				this.off = maxLen;
				this.len = 0;
				this.t = Token.EOF;
			} else if ( offset + length > maxLen ) {
				this.len = maxLen - offset;
			}
		}
		
		@Override
		public IToken nextToken() {
			IToken result = this.t;
			this.t = Token.EOF;
			if ( result == this.t ) {
				this.off += this.len;
				this.len = 0;
			}
			return result;
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
