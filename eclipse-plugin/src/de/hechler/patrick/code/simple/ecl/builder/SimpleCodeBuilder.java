package de.hechler.patrick.code.simple.ecl.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;
import de.hechler.patrick.code.simple.parser.SimpleExportFileParser;
import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;
import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleTypedef;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;

public class SimpleCodeBuilder extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = Activator.PLUGIN_ID + ".project.builder";
	
	private static final String MARKER_TYPE = Activator.PLUGIN_ID + ".problem";
	
	
	public SimpleCodeBuilder() {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			log("new() called");
		}
	}
	
	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		
		final ProjectProps     props;
		final IPath            curSrc;
		final IProgressMonitor monitor;
		
		public SampleDeltaVisitor(ProjectProps props, IPath curSrc, IProgressMonitor monitor) {
			this.props = props;
			this.curSrc = curSrc;
			this.monitor = monitor;
		}
		
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch ( delta.getKind() ) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkSSF(resource, this.props, this.curSrc, this.monitor);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				deletedSSF(resource, this.curSrc, this.monitor);
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				checkSSF(resource, this.props, this.curSrc, this.monitor);
				break;
			}
			// return true to continue visiting children.
			return true;
		}
		
	}
	
	class SampleResourceVisitor implements IResourceVisitor {
		
		final ProjectProps     props;
		final IPath            curSrc;
		final IProgressMonitor monitor;
		
		public SampleResourceVisitor(ProjectProps props, IPath curSrc, IProgressMonitor monitor) {
			this.props = props;
			this.curSrc = curSrc;
			this.monitor = monitor;
		}
		
		@Override
		public boolean visit(IResource resource) {
			checkSSF(resource, this.props, this.curSrc, this.monitor);
			// return true to continue visiting children.
			return true;
		}
		
	}
	
	public record ProjectProps(IProject project, IFolder[] src, IProject[] deps, IFolder bin, IFolder exp,
		Map<IPath,SimpleDependency> laodedDeps) {
		
		public ProjectProps(IProject project, IFolder[] src, IProject[] deps, IFolder bin, IFolder exp) {
			this(project, src, deps, bin, exp, new HashMap<>());
		}
		
	}
	
	private static void addMarker(IFile file, String message, int lineNumber, int severity) {
		addMarker(file, message, lineNumber, -1, -1, severity);
	}
	
	private static void addMarker(IFile file, String message, int lineNumber, int charStart, int charEnd,
		int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if ( lineNumber == -1 ) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			if ( charStart != -1 ) {
				marker.setAttribute(IMarker.CHAR_START, charStart);
				marker.setAttribute(IMarker.CHAR_END, charEnd);
			}
		} catch ( @SuppressWarnings("unused") CoreException e ) {}
	}
	
	@Override
	protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor) throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			log("build( " + kind + " , " + args + " , " + monitor + " ) called");
		}
		if ( args == null ) args = Map.of();
		monitor = IProgressMonitor.nullSafe(monitor);
		IProject project = getProject();
		ProjectProps props = parseProps(project, monitor);
		if ( props == null ) return null;// if the .sc-conf file is invalid do not try to build
		IProjectDescription desc = project.getDescription();
		desc.setReferencedProjects(props.deps);
		project.setDescription(desc, IResource.KEEP_HISTORY | IResource.AVOID_NATURE_CONFIG, monitor);
		if ( kind == FULL_BUILD ) {
			fullBuild(monitor, props);
		} else {
			IResourceDelta delta = getDelta(project);
			if ( delta == null ) {
				fullBuild(monitor, props);
			} else {
				incrementalBuild(delta, monitor, props);
			}
		}
		return null;
	}
	
	public static ProjectProps initilize(IProject project, IProgressMonitor monitor) throws CoreException {
		return parseProps(project, monitor, true);
	}
	
	private static final String BINARY_START  = "binary=";
	private static final String EXPORTS_START = "exports=";
	
	private static ProjectProps parseProps(IProject project, IProgressMonitor monitor) throws CoreException {
		return parseProps(project, monitor, false);
	}
	
	private static ProjectProps parseProps(IProject project, IProgressMonitor monitor, boolean createIfNotExists)
		throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			log("parseProps( " + project + " , " + monitor + " ) called");
		}
		IFile confFile = project.getFile(".sc-config");
		if ( !confFile.exists() ) {
			confFile.create(new ByteArrayInputStream("""
				# this is the configuration file of the Simple-Code Builder
				[source]
				# specify here the source folders
				/src
				[depend]
				# specify here the dependency projects (not the export folders of those dependencies)
				[target]
				# specify here the output folders (binary + export)
				binary=/bin
				exports=/exp
				""".getBytes(StandardCharsets.UTF_8)), true, monitor);
			if ( !StandardCharsets.UTF_8.name().equals(confFile.getCharset()) ) {
				confFile.setCharset(StandardCharsets.UTF_8.name(), monitor);
			}
		}
		Charset cs = Charset.forName(confFile.getCharset(), StandardCharsets.UTF_8);
		deleteMarkers(confFile);
		try ( InputStream in = confFile.getContents(); Scanner sc = new Scanner(in, cs) ) {
			String block = "";
			List<IFolder> source = new ArrayList<>();
			List<IProject> dependencies = new ArrayList<>();
			IFolder binary = null;
			IFolder exports = null;
			int lineNumber = 0;
			while ( sc.hasNextLine() ) {
				lineNumber++;
				String line = sc.nextLine();
				if ( line.isBlank() ) continue;
				if ( line.charAt(0) == '#' ) continue;
				if ( line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']' ) {
					block = line.substring(1, line.length() - 1).toLowerCase();
					switch ( block ) {
					case "source", "depend", "target":
						break;
					default:
						addMarker(confFile, "unknown type: '" + block + "'", lineNumber, IMarker.SEVERITY_WARNING);
					}
					continue;
				}
				switch ( block ) {
				case "source" -> {
					IFolder folder = project.getFolder(line);
					if ( !folder.exists() ) {
						if ( createIfNotExists ) {
							folder.create(0, false, monitor);
						} else {
							addMarker(confFile, "the folder '" + line + "' could not be found", lineNumber,
								IMarker.SEVERITY_ERROR);
							return null;
						}
					}
					source.add(folder);
				}
				case "depend" -> {
					IProject dep = project.getWorkspace().getRoot().getProject(line);
					if ( !dep.exists() ) {
						addMarker(confFile, "the project '" + line + "' could not be found", lineNumber,
							IMarker.SEVERITY_ERROR);
						return null;
					}
					dependencies.add(dep);
				}
				case "target" -> {
					if ( line.startsWith(BINARY_START) ) {
						if ( binary != null ) {
							addMarker(confFile, "only one binary folder can be specified", lineNumber,
								IMarker.SEVERITY_ERROR);
							return null;
						}
						IFolder folder = project.getFolder(line.substring(BINARY_START.length()));
						if ( !folder.exists() ) {
							folder.create(IResource.FORCE | IResource.DERIVED, false, monitor);
						}
						binary = folder;
					} else if ( line.startsWith(EXPORTS_START) ) {
						if ( exports != null ) {
							addMarker(confFile, "only one exports folder can be specified", lineNumber,
								IMarker.SEVERITY_ERROR);
							return null;
						}
						IFolder folder = project.getFolder(line.substring(EXPORTS_START.length()));
						if ( !folder.exists() ) {
							folder.create(IResource.FORCE | IResource.DERIVED, false, monitor);
						}
						exports = folder;
					} else {
						addMarker(confFile, "the line has to start with '" + BINARY_START + "' or '" + EXPORTS_START
							+ "' or be of the form '['BLOCK-NAME']'", lineNumber, IMarker.SEVERITY_ERROR);
						return null;
					}
				}
				default -> {}
				}
			}
			if ( binary == null ) {
				addMarker(confFile, "no binary folder specified", -1, IMarker.SEVERITY_ERROR);
				return null;
			}
			if ( exports == null ) {
				addMarker(confFile, "no export folder specified", -1, IMarker.SEVERITY_ERROR);
				return null;
			}
			if ( source.size() == 0 ) {
				addMarker(confFile, "it whould be nice to have at least one source folder", -1,
					IMarker.SEVERITY_WARNING);
			}
			return new ProjectProps(project, source.toArray(new IFolder[source.size()]),
				dependencies.toArray(new IProject[dependencies.size()]), binary, exports);
		} catch ( IOException e ) {
			throw new CoreException(new Status(IStatus.ERROR, SimpleCodeBuilder.class, e.toString()));
		}
	}
	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		if ( Activator.doLog(LogLevel.INFO) ) {
			log("clean( " + monitor + " ) called");
		}
		// delete markers set and files created
		getProject().deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_INFINITE);
		ProjectProps props = parseProps(getProject(), monitor);
		if ( props != null ) {
			props.bin.delete(IResource.FORCE, monitor);
			props.exp.delete(IResource.FORCE, monitor);
		}
	}
	
	private void deletedSSF(IResource resource, IPath curSrc, IProgressMonitor monitor) {
		if ( Activator.doLog(LogLevel.INFO) ) {
			log("deletedSSF( " + resource + " , " + curSrc + " , " + monitor + " ) called");
		}
		IPath relPath = resource.getFullPath().makeRelativeTo(curSrc);
		IPath appended = curSrc.append(relPath);
		IProject project = resource.getProject();
		IFile file = project.getFile(appended.makeRelativeTo(project.getFullPath()));
		try {
			file.delete(IResource.FORCE, monitor);
		} catch ( CoreException e ) {
			System.err.println("failed to delete the file " + file + ": " + e);
		}
	}
	
	private static final SimpleFile STDLIB;
	
	static {
		try {
			//@formatter:off
			STDLIB = noRuntimeLib("std");
			STDLIB.typedef(new SimpleTypedef("file_handle", SimpleTypedef.FLAG_EXPORT, NativeType.UDWORD), ErrorContext.NO_CONTEXT);
			STDLIB.typedef(new SimpleTypedef("fs_e_handle", SimpleTypedef.FLAG_EXPORT, NativeType.UDWORD), ErrorContext.NO_CONTEXT);
			PointerType pntr = PointerType.create(StructType.create(List.of(), 0, ErrorContext.NO_CONTEXT), ErrorContext.NO_CONTEXT);
			STDLIB.variable(new SimpleVariable(
				pntr, "NULL", null, SimpleVariable.FLAG_CONSTANT | SimpleVariable.FLAG_EXPORT), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "puts", FuncType.create(
				List.of(
						new SimpleVariable(NativeType.UNUM, "wrote", null, 0),
						new SimpleVariable(NativeType.UNUM, "errno", null, 0)
					),
				List.of(new SimpleVariable(PointerType.create(NativeType.UBYTE, ErrorContext.NO_CONTEXT), "string", null, 0)),
				FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "mem_move", FuncType.create(
				List.of(),
				List.of(
						new SimpleVariable(pntr, "from", null, 0),
						new SimpleVariable(pntr, "to", null, 0),
						new SimpleVariable(NativeType.UNUM, "length", null, 0)
					), FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "mem_copy", FuncType.create(
				List.of(),
				List.of(
						new SimpleVariable(pntr, "from", null, 0),
						new SimpleVariable(pntr, "to", null, 0),
						new SimpleVariable(NativeType.UNUM, "length", null, 0)
					), FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "mem_free", FuncType.create(
				List.of(), List.of(new SimpleVariable(pntr, "addr", null, 0)),
				FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "mem_realloc", FuncType.create(
				List.of(new SimpleVariable(pntr, "new_addr", null, 0)),
				List.of(
						new SimpleVariable(pntr, "old_addr", null, 0),
						new SimpleVariable(NativeType.UNUM, "new_length", null, 0),
						new SimpleVariable(NativeType.UNUM, "new_align", null, 0)
					), FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "mem_alloc", FuncType.create(
				List.of(new SimpleVariable(pntr, "addr", null, 0)),
				List.of(
					new SimpleVariable(NativeType.UNUM, "length", null, 0),
					new SimpleVariable(NativeType.UNUM, "align", null, 0)
				),
				FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.function(new SimpleFunction(STDLIB, "exit", FuncType.create(
				List.of(), List.of(new SimpleVariable(NativeType.UBYTE, "exitnum", null, 0)),
				FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			SimpleFile stdSys = noRuntimeLib("std:sys");
			stdSys.function(new SimpleFunction(stdSys, "pagesize", FuncType.create(
				List.of(new SimpleVariable(NativeType.UNUM, "result", null, 0)), List.of(), 
				FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			stdSys.function(new SimpleFunction(stdSys, "pageshift", FuncType.create(
				List.of(new SimpleVariable(NativeType.UNUM, "result", null, 0)), List.of(), 
				FuncType.FLAG_EXPORT | FuncType.FLAG_FUNC_ADDRESS, ErrorContext.NO_CONTEXT)), ErrorContext.NO_CONTEXT);
			STDLIB.dependency(stdSys, "sys", ErrorContext.NO_CONTEXT);
			//@formatter:on
		} catch ( Throwable t ) {
			log("failed to initilize the builder class: " + t);
			throw t;
		}
	}
	
	private static SimpleFile noRuntimeLib(String name) {
		return new SimpleFile(null, null) {
			
			@Override
			public boolean equals(Object obj) { return obj == this; }
			
			@Override
			public int hashCode() { return name.hashCode(); }
			
		};
	}
	
	private void checkSSF(IResource resource, ProjectProps props, IPath curSrc, IProgressMonitor monitor) {
		if ( Activator.doLog(LogLevel.INFO) ) {
			log("checkSSF( " + resource + " , " + props + " , " + curSrc + " , " + monitor + " ) called");
		}
		if ( resource instanceof IFile file && "ssf".equals(resource.getFileExtension()) ) {
			deleteMarkers(file);
			monitor.subTask(file.toString());
			try ( InputStream in = file.getContents() ) {
				BiFunction<String,String,SimpleDependency> dep = dep(props, curSrc, file, monitor);
				SimpleSourceFileParser ssfp = new SimpleSourceFileParser(in, file.toString(), dep);
				IPath relPath = file.getFullPath().makeRelativeTo(curSrc);
				IPath relPath1 = relPath.removeLastSegments(1);
				String lastSeg = relPath.segment(relPath.segmentCount() - 1);
				IPath relTarget = relPath1.append(lastSeg.substring(0, lastSeg.length() - "ssf".length()) + "sexp");
				IFile exportTarget = props.exp.getFile(relTarget);
				SimpleFile sf = new SimpleFile(file.toString(), exportTarget.toString());
				initilizeSimpleFile(sf);
				ssfp.parse(sf);
				String exportString = sf.toExportString();
				if ( exportString.isEmpty() ) {
					return;
				}
				ByteArrayInputStream bais = new ByteArrayInputStream(exportString.getBytes(StandardCharsets.UTF_8));
				if ( !exportTarget.exists() ) {
					IContainer parent = exportTarget.getParent();
					if ( !parent.exists() ) {
						IFolder f = (IFolder) parent;
						f.create(false, false, monitor);
					}
					exportTarget.create(bais, IResource.DERIVED | IResource.FORCE, monitor);
				} else {
					exportTarget.setContents(bais, IResource.FORCE, monitor);
					exportTarget.setDerived(true, monitor);
				}
			} catch ( CompileError err ) {
				addMarker(file, err.toString(), err.line, err.totalChar - 1, err.totalChar, IMarker.SEVERITY_ERROR);
			} catch ( CoreException | IOException e ) {
				addMarker(file, e.toString(), -1, IMarker.SEVERITY_ERROR);
			}
		}
	}
	
	private BiFunction<String,String,SimpleDependency> dep(ProjectProps props, IPath curSrc, IFile parseFile,
		IProgressMonitor monitor) {
		if ( Activator.doLog(LogLevel.TRACE) ) {
			log("dep( " + props + " , " + curSrc + " , " + monitor + " ) called");
		}
		return (srcDep, runDep) -> {
			IFile f = null;
			ProjectProps fProps = null;
			if ( srcDep.charAt(0) != '/' && curSrc != null ) {
				IContainer p = parseFile.getParent();
				IPath path = p.getFullPath().makeRelativeTo(curSrc).append(srcDep);
				IFile f0 = props.exp.getFile(path);
				if ( f0.exists() ) {
					f = f0;
					fProps = props;
				}
			} else {
				for (IProject dep : props.deps) {
					try {
						ProjectProps subProps = parseProps(dep, monitor);
						IFile f0 = subProps.exp.getFile(srcDep);
						if ( f0.exists() ) {
							f = f0;
							fProps = subProps;
							break;
						}
					} catch ( CoreException e ) {
						if ( Activator.doLog(LogLevel.WARN) ) {
							log("could not parse the properties of a depend project (" + dep + "): "
								+ e.getLocalizedMessage());
						}
						return null;
					}
				}
			}
			if ( f == null ) {
				return null;
			}
			try ( InputStream in = f.getContents() ) {
				Charset cs = Charset.forName(f.getCharset(), StandardCharsets.UTF_8);
				try ( InputStreamReader reader = new InputStreamReader(in, cs) ) {
					SimpleTokenStream sts = new SimpleTokenStream(reader, f.toString());
					BiFunction<String,String,SimpleDependency> dep = dep(fProps, null, f, monitor);
					SimpleExportFileParser sefp = new SimpleExportFileParser(sts, dep);
					SimpleFile sf = new SimpleFile(f.toString(), f.toString());
					sefp.parse(sf);
					return sf;
				}
			} catch ( IOException | CoreException e ) {
				if ( Activator.doLog(LogLevel.WARN) ) {
					log("could not parse the dependency " + f + ": " + e.getLocalizedMessage());
				}
				return null;
			}
		};
	}
	
	private static void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch ( CoreException e ) {
			System.err.println("failed to delete markers of " + file + ": " + e);
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor, ProjectProps props) throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			log("fullBuild( " + monitor + " , " + props + " ) called");
		}
		for (IFolder f : props.src) {
			f.accept(new SampleResourceVisitor(props, f.getFullPath(), monitor));
		}
	}
	
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor, ProjectProps props)
		throws CoreException {
		if ( Activator.doLog(LogLevel.DEBUG) ) {
			log("incrementalBuild( " + delta + " , " + monitor + " , " + props + " ) called");
		}
		for (IFolder f : props.src) {
			IResourceDelta subDelta = delta.findMember(f.getProjectRelativePath());
			if ( subDelta == null ) continue;
			subDelta.accept(new SampleDeltaVisitor(props, f.getFullPath(), monitor));
		}
	}
	
	private static void log(String msg) {
		Activator.log("project-builder", msg);
	}
	
	public static void initilizeSimpleFile(SimpleFile sf) {
		sf.typedef(new SimpleTypedef("char", 0, NativeType.UBYTE), ErrorContext.NO_CONTEXT);
		sf.dependency(STDLIB, "std", ErrorContext.NO_CONTEXT);
	}
	
}
