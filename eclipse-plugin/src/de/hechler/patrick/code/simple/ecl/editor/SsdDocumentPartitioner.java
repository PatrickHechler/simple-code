package de.hechler.patrick.code.simple.ecl.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;

public class SsdDocumentPartitioner implements IDocumentPartitioner {
	
	private final IFile    file;
	private final IProject p;
	private final boolean  ssfMode;
	private DocumentTree   tree;
	
	@Override
	public void connect(IDocument document) {}
	
	@Override
	public void disconnect() {}
	
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {}
	
	@Override
	public boolean documentChanged(DocumentEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String[] getLegalContentTypes() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getContentType(int offset) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ITypedRegion getPartition(int offset) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public DocumentTree tree() {
		return this.tree;
	}
	
}
