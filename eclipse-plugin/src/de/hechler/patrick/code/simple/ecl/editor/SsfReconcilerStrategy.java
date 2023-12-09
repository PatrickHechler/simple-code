package de.hechler.patrick.code.simple.ecl.editor;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

public class SsfReconcilerStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
	
	private IDocument        document;
	private String           oldDocument;
	private ProjectionViewer projectionViewer;
	private List<Annotation> oldAnnotations = new ArrayList<>();
	
	@Override
	public void setDocument(IDocument document) {
		this.document = document;
	}
	
	public void setProjectionViewer(ProjectionViewer projectionViewer) {
		this.projectionViewer = projectionViewer;
	}
	
	@Override
	public void reconcile(@SuppressWarnings("unused") DirtyRegion dirtyRegion, @SuppressWarnings("unused") IRegion subRegion) {
		initialReconcile();
	}
	
	@Override
	public void reconcile(@SuppressWarnings("unused") IRegion partition) {
		initialReconcile();
	}
	
	@Override
	public void initialReconcile() {
		String doc = this.document.get();
		boolean b = doc.equals(this.oldDocument);
		if ( b ) return;
//		if ( !b ) return;
//		for (int i = 0; i < 100; i++) {
//			System.err.println("WARN: reconcile");
//		}
		this.oldDocument = doc;
		
		List<Position> positions = getNewPositionsOfAnnotations(doc);
		
		for (Annotation a : this.oldAnnotations) {
			this.projectionViewer.getProjectionAnnotationModel().removeAnnotation(a);
		}
		
		for (Position position : positions) {
			Annotation annotation = new ProjectionAnnotation();
			this.projectionViewer.getProjectionAnnotationModel().addAnnotation(annotation, position);
			this.oldAnnotations.add(annotation);
		}
	}
	
	private List<Position> getNewPositionsOfAnnotations(String doc) {
		List<Position> positions = new ArrayList<>();
		try {
			int depth = 0;
			int startLine = -1;
			final int len = doc.length();
			for (int i = 0; i < len; i++) {
				char c = doc.charAt(i);
				if ( c == '{' ) {
					if ( depth == 0 ) {
						startLine = this.document.getLineOfOffset(i);
					}
					depth++;
				} else if ( c == '}' ) {
					depth--;
					if ( depth == 0 ) {
						int endLine = this.document.getLineOfOffset(i) - 1;
						if ( startLine < endLine ) {
							int startOff = this.document.getLineOffset(startLine);
							int endOff = this.document.getLineOffset(endLine) + this.document.getLineLength(endLine);
							for (; i < len && Character.isWhitespace(doc.charAt(i)) && i < endOff; i++);
							if ( i >= endOff ) {
								endLine++;
								endOff = this.document.getLineOffset(endLine) + this.document.getLineLength(endLine);
							}
							positions.add(new Position(startOff, endOff - startOff));
						}
					}
				}
			}
		} catch ( BadLocationException e ) {
			// skip the remainder of file due to error
			e.printStackTrace();// this should never happen
		}
		return positions;
	}
	
	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
		// no progress monitor used
	}
	
}
