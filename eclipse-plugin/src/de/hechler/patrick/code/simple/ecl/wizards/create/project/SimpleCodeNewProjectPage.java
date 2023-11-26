package de.hechler.patrick.code.simple.ecl.wizards.create.project;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The "New" wizard page allows setting the container for the new file as well as the file name. The page will only
 * accept file name without the extension OR with the extension that matches the expected one (ssf).
 */

public class SimpleCodeNewProjectPage extends WizardPage {
	
	private Text projectNameField;
	
	/**
	 * Constructor for SampleNewWizardPage.
	 *
	 * @param pageName
	 */
	public SimpleCodeNewProjectPage() {
		super("wizardPage");
		setTitle("Create Simple Code Project");
		setDescription("This wizard creates a new Simple-Code Project.");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// new project label
		Label projectLabel = new Label(composite, SWT.NONE);
		projectLabel.setText("&Project name:");
		projectLabel.setFont(parent.getFont());
		
		// new project name entry field
		projectNameField = new Text(composite, SWT.BORDER);
		projectNameField.setFont(parent.getFont());
		
		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		projectNameField.addListener(SWT.Modify, this::nameModifyListener);
		
		setPageComplete(false);
		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}
	
	private void nameModifyListener(@SuppressWarnings("unused") Event event) {
		dialogChanged();
	}
	
	/**
	 * Ensures that both text fields are set.
	 */
	
	private void dialogChanged() {
		String projectName = getProjectName();
		if ( projectName.length() == 0 ) {
			updateStatus("project name must be specified");
			return;
		}
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath rootPath = root.getFullPath();
		IPath path = rootPath.append(projectName);
		if ( path.equals(rootPath) ) {
			updateStatus("project name can not refer to the workspace root");
			return;
		}
		if ( path.segmentCount() - 1 != rootPath.segmentCount() ) {
			updateStatus("project name must not contain a path seperator");
			return;
			
		}
		if ( !rootPath.isPrefixOf(path) ) {
			updateStatus("project path must start with the workspace root");
			return;
		}
		if ( root.findMember(projectName) != null ) {
			updateStatus("project path must not exist already");
		}
		
		updateStatus(null);
	}
	
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	public String getProjectName() {
		return projectNameField.getText();
	}
	
}
