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

public class ValidatorDocumentSetupParticipant implements IDocumentSetupParticipant, IDocumentSetupParticipantExtension {
	
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
				log("could not set a marker: " + e);
			}
		}
	}
	
	@Override
	public void setup(@SuppressWarnings("unused") IDocument document) {}
	
	@Override
	public void setup(IDocument document, IPath location, LocationKind locationKind) {
		if ( locationKind == LocationKind.IFILE ) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
			IProject p = file.getProject();
			try {
				IProjectDescription desc = p.getDescription();
				if ( !desc.hasNature(SimpleCodeNature.NATURE_ID) ) {
					return;
				}
				SsfDocumentPartitioner docVal = new SsfDocumentPartitioner(file);
				document.setDocumentPartitioner(docVal);
			} catch (UnsupportedClassVersionError e) {
				addMarker(file, "are you running eclipse with JavaSE21 with preview enabled? " + e, -1, IMarker.SEVERITY_ERROR);
			} catch (CoreException e) {
				if ( Activator.doLog(LogLevel.ERROR) ) {
					log("could not initilize the editor: " + e);
				}
			}
		}
	}
	
	private static void log(String msg) {
		Activator.log("editor.validator", msg);
	}
	
}
