package de.hechler.patrick.code.simple.ecl.wizards.create.project;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder.ProjectProps;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeNature;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the provided container. If the container
 * resource (a folder or a project) is selected in the workspace when the wizard is opened, it will accept it as the
 * target container. The wizard creates one file with the extension "ssf". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will be able to open it.
 */

public class SimpleCodeNewProject extends Wizard implements INewWizard {
	
	private SimpleCodeNewProjectPage page;
	
	/**
	 * Constructor for SimpleCodeNewWizard.
	 */
	public SimpleCodeNewProject() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */
	@Override
	public void addPages() {
		page = new SimpleCodeNewProjectPage();
		addPage(page);
	}
	
	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using
	 * wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		final String projectName = page.getProjectName();
		IRunnableWithProgress op = monitor -> {
			try {
				doFinish(projectName, monitor);
			} catch ( CoreException e ) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch ( InterruptedException e ) {
			return false;
		} catch ( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
	
	private void doFinish(String projectName, IProgressMonitor monitor) throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + projectName, 4);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription desc = workspace.newProjectDescription(projectName);
		if ( monitor.isCanceled() ) {
			monitor.done();
			return;
		}
		SimpleCodeNature.configureDesc(desc);
		monitor.worked(1);
		if ( monitor.isCanceled() ) {
			monitor.done();
			return;
		}
		
		project.create(desc, 0, monitor);
		monitor.worked(1);
		if ( monitor.isCanceled() ) {
			monitor.done();
			return;
		}
		project.open(0, monitor);
		monitor.worked(1);
		if ( monitor.isCanceled() ) {
			monitor.done();
			return;
		}
		ProjectProps props = SimpleCodeBuilder.initilize(project, monitor);
		if ( props.src().length == 1 && props.src()[0].members().length == 0 ) {
			IFile hw = props.src()[0].getFile("hello_world.ssf");
			ByteArrayInputStream bais = new ByteArrayInputStream("""
				// This file prints 'hello world\\n' to stdout and then exits with zero on success
				func main <ubyte exitnum> <-- (unum argc, char## argv) {
					unum err;
					std:puts<?,err> <-- ("hello world\\n");
					exitnum <-- err == 0;
				}
				""".getBytes(StandardCharsets.UTF_8));
			hw.create(bais, 0, monitor);
			getShell().getDisplay().asyncExec(() -> {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, hw, true);
				} catch (PartInitException e) {
				}
			});
		}
		monitor.done();
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
	
}
