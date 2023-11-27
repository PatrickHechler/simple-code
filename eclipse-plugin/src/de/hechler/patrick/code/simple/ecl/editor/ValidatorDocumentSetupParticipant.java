package de.hechler.patrick.code.simple.ecl.editor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IDocumentSetupParticipantExtension;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder;
import de.hechler.patrick.code.simple.parser.SimpleExportFileParser;
import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;
import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;

public class ValidatorDocumentSetupParticipant
	implements IDocumentSetupParticipant, IDocumentSetupParticipantExtension {
	
	public static final int TOKEN_COMMENT = SimpleTokenStream.MAX_TOKEN + 1;
	
	private final class DocumentValidator implements IDocumentListener {
		
		private final IFile   file;
		private final boolean ssfMode;
		private List<IMarker> marker = new ArrayList<>();
		private DocumentTree  tree;
		
		private DocumentValidator(IFile file) {
			this.file = file;
			this.ssfMode = file.getName().endsWith(".ssf");
		}
		
		private static FilePosition pos(SimpleTokenStream sts) {
			return new FilePosition(sts.totalChar(), sts.line(), sts.charInLine());
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			for (IMarker m : this.marker) {
				try {
					m.delete();
				} catch ( CoreException e ) {
					if ( Activator.doLog(LogLevel.ERROR) ) {
						Activator.log("editor.ssf : " + DocumentValidator.this.file, "could not delete a marker: " + e);
					}
				}
			}
			this.marker.clear();
			try ( StringReader reader = new StringReader(event.getDocument().get()) ) {
				buildTree(reader);
			}
		}
		
		private void buildTree(Reader reader) {
			DocumentTree tree = new DocumentTree();
			SimpleTokenStream sts = new SimpleTokenStream(reader, this.file.toString()) {
				
				@Override
				protected int findToken(int r) throws IOException {
					FilePosition start = pos(this);
					int tok = super.findToken(r);
					FilePosition end = pos(this);
					int token = tok == INVALID ? TOKEN_COMMENT : tok;
					FilePosition.FileToken ftok = new FilePosition.FileToken(start, token, end);
					tree.parsedToken(ftok);
					return tok;
				}
				
			};
			BiFunction<String,String,SimpleDependency> dep = dep(this.file);
			SimpleExportFileParser sp = createParser(tree, sts, dep);
			SimpleFile sf = new SimpleFile(this.file.toString(), this.file.toString());
			SimpleCodeBuilder.initilizeSimpleFile(sf);
			sp.parse(sf);
			this.tree = tree;
		}
		
		private SimpleExportFileParser createParser(DocumentTree tree, SimpleTokenStream sts,
			BiFunction<String,String,SimpleDependency> dep) {
			SimpleExportFileParser sp;
			if ( this.ssfMode ) {
				sp = new SimpleSourceFileParser(sts, dep) {
					
					@Override
					protected Object enterUnknownState() {
						FilePosition pos = pos(sts);
						return tree.enterState(pos, -1);
					}
					
					@Override
					protected Object maybeFinishUnknownState() {
						return pos(sts);
					}
					
					@Override
					protected Object enterState(int state) {
						FilePosition pos = pos(sts);
						return tree.enterState(pos, state);
					}
					
					@Override
					protected void exitState(int state, Object enterResult, Object additionalData) {
						FilePosition pos = pos(sts);
						tree.exitState(pos, state, additionalData, (FilePosition.FileState) enterResult);
					}
					
					@Override
					protected void remenberExitedState(int state, Object enterResult, Object enterUnknownEndMarker,
						Object additionalData) {
						if ( enterUnknownEndMarker instanceof FilePosition fp ) {
							tree.rememberExitedState((FilePosition.FileState) enterResult, fp, state, additionalData);
						} else {
							tree.rememberExitedState((FilePosition.FileState) enterResult,
								( (FilePosition.FileState) enterUnknownEndMarker ).start(), state, additionalData);
						}
					}
					
					@Override
					protected Object decidedState(int state, Object unknownStateResult) {
						return tree.decideState((FilePosition.FileState) unknownStateResult, state);
					}
					
					@Override
					protected Object[] decidedStates(int[] states, Object unknownStateResult) {
						return tree.decideStates((FilePosition.FileState) unknownStateResult, states);
					}
					
					@Override
					protected void handleError(CompileError err) {
						try {
							IMarker m = file.createMarker(IMarker.PROBLEM);
							DocumentValidator.this.marker.add(m);
							m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
							m.setAttribute(IMarker.MESSAGE, err.getMessage());
							m.setAttribute(IMarker.LINE_NUMBER, err.line);
						} catch ( CoreException e ) {
							if ( Activator.doLog(LogLevel.ERROR) ) {
								Activator.log("editor.ssf : " + DocumentValidator.this.file,
									"could not set a marker: " + e);
							}
						}
					}
					
				};
			} else {
				sp = new SimpleExportFileParser(sts, dep) {
					
					@Override
					protected Object enterUnknownState() {
						FilePosition pos = pos(sts);
						return tree.enterState(pos, -1);
					}
					
					@Override
					protected Object maybeFinishUnknownState() {
						return pos(sts);
					}
					
					@Override
					protected Object enterState(int state) {
						FilePosition pos = pos(sts);
						return tree.enterState(pos, state);
					}
					
					@Override
					protected void exitState(int state, Object enterResult, Object additionalData) {
						FilePosition pos = pos(sts);
						tree.exitState(pos, state, additionalData, (FilePosition.FileState) enterResult);
					}
					
					@Override
					protected void remenberExitedState(int state, Object enterResult, Object enterUnknownEndMarker,
						Object additionalData) {
						if ( enterUnknownEndMarker instanceof FilePosition fp ) {
							tree.rememberExitedState((FilePosition.FileState) enterResult, fp, state, additionalData);
						} else {
							tree.rememberExitedState((FilePosition.FileState) enterResult,
								( (FilePosition.FileState) enterUnknownEndMarker ).start(), state, additionalData);
						}
					}
					
					@Override
					protected Object decidedState(int state, Object unknownStateResult) {
						return tree.decideState((FilePosition.FileState) unknownStateResult, state);
					}
					
					@Override
					protected Object[] decidedStates(int[] states, Object unknownStateResult) {
						return tree.decideStates((FilePosition.FileState) unknownStateResult, states);
					}
					
					@Override
					protected void handleError(CompileError err) {
						try {
							IMarker m = file.createMarker(IMarker.PROBLEM);
							DocumentValidator.this.marker.add(m);
							m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
							m.setAttribute(IMarker.MESSAGE, err.getMessage());
							m.setAttribute(IMarker.LINE_NUMBER, err.line);
						} catch ( CoreException e ) {
							if ( Activator.doLog(LogLevel.ERROR) ) {
								Activator.log("editor.ssf : " + DocumentValidator.this.file,
									"could not set a marker: " + e);
							}
						}
					}
					
				};
			}
			return sp;
		}
		
		private BiFunction<String,String,SimpleDependency> dep(IFile file2) { // TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}
		
	}
	
	@Override
	public void setup(IDocument document) {
	}
	
	@Override
	public void setup(IDocument document, IPath location, LocationKind locationKind) {
		if ( locationKind == LocationKind.IFILE ) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
			document.addDocumentListener(new DocumentValidator(file));
		}
	}
	
}
