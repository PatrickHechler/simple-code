package de.hechler.patrick.code.simple.ecl.builder;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;

public class AddRemoveSimpleCodeNatureHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if ( Activator.doLog(LogLevel.TRACE) ) {
			Activator.log("project-toggle", "execute( " + event + " ) called");
		}
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if ( selection instanceof IStructuredSelection ) {
			for (Iterator<?> it = ( (IStructuredSelection) selection ).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if ( element instanceof IProject ) {
					project = (IProject) element;
				} else {
					project = Adapters.adapt(element, IProject.class);
				}
				if ( project != null ) {
					try {
						toggleNature(project);
					} catch ( CoreException e ) {
						throw new ExecutionException("Failed to toggle nature", e);
					}
				}
			}
		}
		
		return null;
	}
	
	private static void toggleNature(IProject project) throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			Activator.log("project-toggle", "toggleNature( " + project + " ) called");
		}
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		
		for (int i = 0; i < natures.length; ++i) {
			if ( SimpleCodeNature.NATURE_ID.equals(natures[i]) ) {
				// Remove the nature
				String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				return;
			}
		}
		
		// Add the nature
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = SimpleCodeNature.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}
	
}
