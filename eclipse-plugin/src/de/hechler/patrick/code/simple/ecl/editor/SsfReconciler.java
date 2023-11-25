package de.hechler.patrick.code.simple.ecl.editor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

public class SsfReconciler extends Reconciler {

    private SsfReconcilerStrategy fStrategy;

    public SsfReconciler() {
        // TODO this is logic for .project file to fold tags. Replace with your language logic!
        fStrategy = new SsfReconcilerStrategy();
        this.setReconcilingStrategy(fStrategy, IDocument.DEFAULT_CONTENT_TYPE);
    }

    @Override
    public void install(ITextViewer textViewer) {
        super.install(textViewer);
        ProjectionViewer pViewer =(ProjectionViewer)textViewer;
        fStrategy.setProjectionViewer(pViewer);
    }
}