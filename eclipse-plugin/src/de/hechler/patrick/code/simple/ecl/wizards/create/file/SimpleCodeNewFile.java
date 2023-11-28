package de.hechler.patrick.code.simple.ecl.wizards.create.file;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the provided container. If the container
 * resource (a folder or a project) is selected in the workspace when the wizard is opened, it will accept it as the
 * target container. The wizard creates one file with the extension "ssf". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will be able to open it.
 */

public class SimpleCodeNewFile extends Wizard implements INewWizard {
	private SimpleCodeNewFilePage page;
	private ISelection            selection;
	
	/**
	 * Constructor for SimpleCodeNewWizard.
	 */
	public SimpleCodeNewFile() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */
	@Override
	public void addPages() {
		this.page = new SimpleCodeNewFilePage(this.selection);
		addPage(this.page);
	}
	
	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using
	 * wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		final String containerName = this.page.getContainerName();
		final String fileName = this.page.getFileName();
		IRunnableWithProgress op = monitor -> {
			try {
				doFinish(containerName, fileName, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
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
	
	private void doFinish(String containerName, String fileName, IProgressMonitor monitor) throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + fileName, 3);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(IPath.fromOSString(containerName));
		if ( !resource.exists() || !( resource instanceof IContainer ) ) {
			throw new CoreException(Status.error("Container \"" + containerName + "\" does not exist."));
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(IPath.fromOSString(fileName));
		try {
			InputStream stream = openContentStream(fileName);
			if ( file.exists() ) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (@SuppressWarnings("unused") IOException e) {
		}
		monitor.worked(1);
		String cs = file.getCharset();
		if ( !StandardCharsets.UTF_8.name().equals(cs) && !StandardCharsets.UTF_8.aliases().contains(cs) ) {
			file.setCharset(StandardCharsets.UTF_8.name(), monitor);
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(() -> {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			try {
				IDE.openEditor(activePage, file, true);
			} catch (@SuppressWarnings("unused") PartInitException e) {
			}
		});
		monitor.done();
	}
	
	/**
	 * We will initialize file contents with a sample text.
	 */
	
	private static InputStream openContentStream(String fileName) {
		String content;
		if ( fileName.endsWith(".sexp") ) {
			content = """
				// Simple-Code export file
				 
				""";
		} else {
			content = """
				// Simple-Code source file
				
				""";
		}
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * We will accept the selection in the workbench to see if we can initialize from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
