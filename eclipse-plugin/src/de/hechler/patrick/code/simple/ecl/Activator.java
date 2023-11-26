package de.hechler.patrick.code.simple.ecl;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogLevel;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = "de.hechler.patrick.code.simple.ecl"; //$NON-NLS-1$
	
	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		if ( doLog(LogLevel.INFO) ) {
			log("activator", "start( " + context + " ) called");
		}
		super.start(context);
		plugin = this;
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		if ( doLog(LogLevel.INFO) ) {
			log("activator", "stop( " + context + " ) called");
		}
		plugin = null;
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public static boolean doLog(LogLevel level) {
		return level.ordinal() <= LogLevel.DEBUG.ordinal();
	}
	
	public static void log(String caller, String msg) {
		if ( msg.indexOf('\n') == -1 ) {
			System.err.println("[pat.simple-code." + caller + "]: " + msg);
		} else {
			msg.lines().forEach(line -> System.err.println("[pat.simple-code." + caller + "]: " + line));
		}
	}
	
}
