package de.hechler.patrick.code.simple.ecl.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class SsfAutoEditStrategy implements IAutoEditStrategy {
	
	@Override
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		String txt = command.text;
		if ( "\"".equals(txt) || "'".equals(txt) ) {
			try {
				int coff = command.offset;
				IRegion reg = document.getLineInformationOfOffset(coff);
				int off = reg.getOffset();
				if ( cnt(document, off, off + reg.getLength(), txt.charAt(0), coff) ) {
					command.text = txt.concat(txt);
					command.shiftsCaret = false;
					// seems to indicate if caretOffset should interpreted as offset from before or after the command
					command.caretOffset = coff + 1;
				}
			} catch ( BadLocationException e ) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean cnt(IDocument document, int off, int end, char c, int coff) {
		try {
			boolean res = true;
			for (; off < coff; off++) {
				if ( document.getChar(off) == c ) res = !res;
			}
			if ( !res ) return false;
			for (; off < end; off++) {
				if ( document.getChar(off) == c ) res = !res;
			}
			return res;
		} catch ( BadLocationException e ) {
			e.printStackTrace();
			return false;
		}
	}
	
}
