package de.hechler.patrick.code.simple.ecl.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IDocumentSetupParticipantExtension;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeNature;

public class ValidatorDocumentSetupParticipant implements IDocumentSetupParticipant, IDocumentSetupParticipantExtension {
	
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
				SsfPosUpdate docVal = new SsfPosUpdate(file);
				document.addPositionUpdater(docVal);
			} catch (UnsupportedClassVersionError e) {
				SsfPosUpdate.addMarker(file, "are you running eclipse with JavaSE21 with preview enabled? " + e, -1, IMarker.SEVERITY_ERROR);
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
