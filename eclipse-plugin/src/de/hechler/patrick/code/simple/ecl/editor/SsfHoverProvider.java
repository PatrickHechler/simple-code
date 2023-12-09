package de.hechler.patrick.code.simple.ecl.editor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import de.hechler.patrick.code.simple.ecl.editor.FilePosition.FileToken;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;

public class SsfHoverProvider implements ITextHover {
	
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		SsfPosUpdate spu = SsfPosUpdate.getSPU(textViewer.getDocument());
		if ( spu == null ) return null;
		FileToken tok = spu.tree().find(hoverRegion.getOffset());
		if ( tok.token() == SimpleTokenStream.MAX_TOKEN + 1 ) return null;
		Object info = tok.parent().global().info();
		if ( info == null ) return null;
		return info.toString();
	}
	
	@Override
	public IRegion getHoverRegion(@SuppressWarnings("unused") ITextViewer textViewer, int offset) {
		return new Region(offset, 0);
	}
	
}
