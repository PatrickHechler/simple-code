package de.hechler.patrick.code.simple.ecl.editor;

import java.io.Reader;
import java.io.StringReader;
import java.util.function.BiFunction;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IDocumentSetupParticipantExtension;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder.ProjectProps;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeNature;
import de.hechler.patrick.code.simple.ecl.editor.FilePosition.FileToken;
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
		
		private final IFile    file;
		private final IProject p;
		private final boolean  ssfMode;
		private DocumentTree   tree;
		
		private DocumentValidator(IFile file, IProject p) {
			this.file = file;
			this.p = p;
			this.ssfMode = file.getName().endsWith(".ssf");
		}
		
		private static FilePosition pos(SimpleTokenStream sts) {
			return new FilePosition(sts.totalChar(), sts.line(), sts.charInLine());
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			try (StringReader reader = new StringReader(event.getDocument().get())) {
				this.file.deleteMarkers(SimpleCodeBuilder.VOLATILE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				buildTree(reader);
			} catch (CoreException e) {
				if ( Activator.doLog(LogLevel.ERROR) ) {
					Activator.log("editor.validator", "validation crashed: " + e);
				}
			}
		}
		
		private void buildTree(Reader reader) throws CoreException {
			DocumentTree tree = new DocumentTree();
			SimpleTokenStream sts = new SimpleTokenStream(reader, this.file.toString()) {
				
				private FileToken ftok;
				private boolean   b;
				
				@Override
				public String consumeDynTokSpecialText() {
					if ( this.b ) return super.consumeDynTokSpecialText();
					try {
						this.b = true;
						FileToken ft = this.ftok;
						if ( ft != null ) {
							tree.parsedToken(ft);
							this.ftok = null;
						}// consumeTok() allows this method to be called, when the token is already consumed
						return super.consumeDynTokSpecialText();
					} finally {
						this.b = false;
					}
				}
				
				@Override
				public int consumeTok() {
					if ( this.b ) return super.consumeTok();
					try {
						this.b = true;
						FileToken ft = this.ftok;
						if ( ft == null ) {
							ft = nft();
						} else {
							this.ftok = null;
						}
						tree.parsedToken(ft);
						return super.consumeTok();
					} finally {
						this.b = false;
					}
				}
				
				@Override
				public void consume() {
					if ( this.b ) { super.consume(); return; }
					try {
						this.b = true;
						FileToken ft = this.ftok;
						if ( ft == null ) {
							ft = nfct();
						} else {
							this.ftok = null;
							super.consumeTok();
						}
						tree.parsedToken(ft);
					} finally {
						this.b = false;
					}
				}
				
				@Override
				public int tok() {
					if ( this.b ) return super.tok();
					try {
						this.b = true;
						FileToken ft = this.ftok;
						if ( ft == null ) {
							ft = nft();
							this.ftok = ft;
						}
						return ft.token();
					} finally {
						this.b = false;
					}
				}
				
				private FileToken nft() {
					FileToken ft;
					FilePosition start = pos(this);
					int tok = super.tok();
					FilePosition end = pos(this);
					int token = tok == INVALID ? TOKEN_COMMENT : tok;
					ft = new FilePosition.FileToken(start, token, end);
					return ft;
				}
				
				private FileToken nfct() {
					FileToken ft;
					FilePosition start = pos(this);
					int tok = super.consumeTok();
					FilePosition end = pos(this);
					int token = tok == INVALID ? TOKEN_COMMENT : tok;
					ft = new FilePosition.FileToken(start, token, end);
					return ft;
				}
				
			};
			BiFunction<String, String, SimpleDependency> dep = dep(this.file, this.p);
			if ( dep == null ) {
				this.tree = null;
				return;
			}
			SimpleExportFileParser sp = createParser(tree, sts, dep);
			SimpleFile sf = new SimpleFile(this.file.toString(), this.file.toString());
			SimpleCodeBuilder.initilizeSimpleFile(sf);
			sp.parse(sf);
			this.tree = tree;
		}
		
		private SimpleExportFileParser createParser(DocumentTree tree, SimpleTokenStream sts,
			BiFunction<String, String, SimpleDependency> dep) {
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
						addMarker(DocumentValidator.this.file, err.getLocalizedMessage(), err.line,
							IMarker.SEVERITY_ERROR);
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
						addMarker(DocumentValidator.this.file, err.getLocalizedMessage(), err.line,
							IMarker.SEVERITY_ERROR);
					}
					
				};
			}
			return sp;
		}
		
		private static BiFunction<String, String, SimpleDependency> dep(IFile file, IProject p) throws CoreException {
			ProjectProps props = SimpleCodeBuilder.parseProps(p, null);
			if ( props == null ) return null;
			IPath fp = file.getFullPath();
			for (IFolder src : props.src()) {
				BiFunction<String, String, SimpleDependency> d = dep0(file, props, fp, src);
				if ( d != null ) return d;
			}
			if ( file.getName().endsWith(".sexp") ) {
				return dep0(file, props, fp, props.exp());
			}
			return null;
		}
		
		private static BiFunction<String, String, SimpleDependency> dep0(IFile file, ProjectProps props, IPath fp,
			IFolder src) {
			IPath sfp = src.getFullPath();
			if ( sfp.isPrefixOf(fp) ) {
				IFile exportFile = src.getFile(fp.makeRelativeTo(sfp));
				BiFunction<String, String, SimpleDependency> result =
					SimpleCodeBuilder.dep(props, sfp, exportFile, null);
				return (source, runtime) -> {
					try {
						return result.apply(source, runtime);
					} catch (CompileError ce) {
						addMarker(exportFile, ce.getLocalizedMessage(), ce.line, IMarker.SEVERITY_ERROR);
						addMarker(file,
							"the dependency " + source + " could not be parsed: " + ce.getLocalizedMessage(), -1,
							IMarker.SEVERITY_ERROR);
						return null;
					}
				};
			}
			return null;
		}
		
		@Override
		public void documentAboutToBeChanged(@SuppressWarnings("unused") DocumentEvent event) {
		}
		
	}
	
	private static void addMarker(IFile file, String msg, int line, int severity) {
		try {
			IMarker m = file.createMarker(SimpleCodeBuilder.VOLATILE_MARKER_TYPE);
			m.setAttribute(IMarker.SEVERITY, severity);
			m.setAttribute(IMarker.MESSAGE, msg);
			if ( line == -1 ) {
				line = 1;
			}
			m.setAttribute(IMarker.LINE_NUMBER, line);
		} catch (CoreException e) {
			if ( Activator.doLog(LogLevel.ERROR) ) {
				Activator.log("editor.ssf : " + file, "could not set a marker: " + e);
			}
		}
	}
	
	@Override
	public void setup(@SuppressWarnings("unused") IDocument document) {
	}
	
	@Override
	public void setup(IDocument document, IPath location, LocationKind locationKind) {
		if ( locationKind == LocationKind.IFILE ) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
			IProject p = file.getProject();
			DocumentValidator docVal = new DocumentValidator(file, p);
			try {
				IProjectDescription desc = p.getDescription();
				if ( !desc.hasNature(SimpleCodeNature.NATURE_ID) ) {
					return;
				}
				document.addDocumentListener(docVal);
			} catch (UnsupportedClassVersionError e) {
				addMarker(file, "are you running eclipse with JavaSE21 with --enable-preview? " + e, -1,
					IMarker.SEVERITY_ERROR);
			} catch (CoreException e) {
				if ( Activator.doLog(LogLevel.ERROR) ) {
					Activator.log("editor.setup", "could not initilize the editor: " + e);
				}
			}
		}
	}
	
}
