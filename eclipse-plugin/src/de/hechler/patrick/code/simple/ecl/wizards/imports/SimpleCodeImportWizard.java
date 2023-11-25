package de.hechler.patrick.code.simple.ecl.wizards.imports;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class SimpleCodeImportWizard extends Wizard implements IImportWizard {
	
	SimpleCpdeImportWizardPage mainPage;

	public SimpleCodeImportWizard() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		IFile file = mainPage.createNewFile();
        if (file == null)
            return false;
        return true;
	}
	 
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Simple-Code File Import Wizard"); //NON-NLS-1
		setNeedsProgressMonitor(true);
		mainPage = new SimpleCpdeImportWizardPage("Import Simple-Code File",selection); //NON-NLS-1
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
	public void addPages() {
        super.addPages(); 
        addPage(mainPage);        
    }

}
