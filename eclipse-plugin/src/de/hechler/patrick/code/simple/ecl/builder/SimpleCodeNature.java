package de.hechler.patrick.code.simple.ecl.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;

public class SimpleCodeNature implements IProjectNature {
	
	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = Activator.PLUGIN_ID + ".project.nature";
	
	private IProject project;
	
	@Override
	public void configure() throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			Activator.log("project-nature", "configure() called");
		}
		IProjectDescription desc = project.getDescription();
		
		configureDesc(desc);
		project.setDescription(desc, null);
	}
	
	public static void configureDesc(IProjectDescription desc) {
		ICommand[] commands = desc.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if ( commands[i].getBuilderName().equals(SimpleCodeBuilder.BUILDER_ID) ) {
				return;
			}
		}
		
		ICommand[] newCommands = new ICommand[commands.length + 1];
		for (int i = 0; i < commands.length; i++) {
			newCommands[i] = commands[i];
		}
		ICommand command = desc.newCommand();
		command.setBuilderName(SimpleCodeBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
	}
	
	@Override
	public void deconfigure() throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			Activator.log("project-nature", "deconfigure() called");
		}
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if ( commands[i].getBuilderName().equals(SimpleCodeBuilder.BUILDER_ID) ) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);
				return;
			}
		}
	}
	
	@Override
	public IProject getProject() {
		return project;
	}
	
	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
	
}
