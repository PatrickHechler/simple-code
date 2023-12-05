package de.hechler.patrick.code.simple.ecl.editor;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.osgi.service.log.LogLevel;

import de.hechler.patrick.code.simple.ecl.Activator;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder;
import de.hechler.patrick.code.simple.ecl.builder.SimpleCodeBuilder.ProjectProps;
import de.hechler.patrick.code.simple.ecl.editor.FilePosition.FileToken;
import de.hechler.patrick.code.simple.parser.SimpleExportFileParser;
import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;
import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.value.DependencyVal;
import de.hechler.patrick.code.simple.parser.objects.value.FunctionVal;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;

public class SsfDocumentPartitioner implements IDocumentPartitioner {
	
	public static final String TOKEN_WHITESPACE                = "token: whitespace";
	public static final String TOKEN_COMMENT                   = "token: comment";
	public static final String TOKEN_OTHER_SYMBOL              = "token: seperator/math/other-symbol";
	public static final String TOKEN_ASM_BLOCK                 = "token: asm block";
	public static final String TOKEN_CHARACTER                 = "token: character";
	public static final String TOKEN_STRING                    = "token: string";
	public static final String TOKEN_NUMBER                    = "token: number";
	public static final String TOKEN_PRIM_TYPE                 = "token: type";
	public static final String TOKEN_DEFED_TYPE                = "keyword: typedefed type";
	public static final String KEYWORD_ASM                     = "keyword: asm";
	public static final String KEYWORD_WHILE_IF_ELSE           = "keyword: while/if/else";
	public static final String KEYWORD_CALL                    = "keyword: call";
	public static final String KEYWORD_FSTRUCT_STRUCT          = "keyword: fstruct/struct";
	public static final String KEYWORD_CONST                   = "keyword: const";
	public static final String KEYWORD_MAIN_INIT               = "keyword: main/init";
	public static final String KEYWORD_NOPAD                   = "keyword: nopad";
	public static final String KEYWORD_EXP                     = "keyword: exp";
	public static final String KEYWORD_FUNC_AS_FUNC_ADDR       = "keyword: func (as function address type)";
	public static final String KEYWORD_FUNC                    = "keyword: func";
	public static final String KEYWORD_DEP                     = "keyword: dep";
	public static final String KEYWORD_TYPEDEF                 = "keyword: typedef";
	public static final String DECL_TYPEDEF_NAME               = "decl: typedef name";
	public static final String DECL_DEP_NAME                   = "decl: dependency name";
	public static final String DECL_FUNC_NAME                  = "decl: function name";
	public static final String DECL_PARAM_RESULT_VARIABLE_NAME = "decl: param/result variable name";
	public static final String DECL_LOCAL_VARIABLE_NAME        = "decl: local variable name";
	public static final String DECL_GLOBAL_VARIABLE_NAME       = "decl: global variable name";
	public static final String REF_VALUE_REFERENCE_NAME        = "ref: value reference name";
	public static final String REF_LOCAL_VARIABLE              = "ref: local variable";
	public static final String REF_GLOBAL_DEPENDENCY           = "ref: global dependency";
	public static final String REF_GLOBAL_FUNCTION             = "ref: global function";
	public static final String REF_GLOBAL_VARIABLE             = "ref: global variable";
	
	private static final int COMMENT_TOKEN = SimpleTokenStream.MAX_TOKEN + 1;
	
	private static final String[] LEGAL = new String[] { //
		TOKEN_WHITESPACE, //
		TOKEN_COMMENT, //
		TOKEN_OTHER_SYMBOL, //
		TOKEN_ASM_BLOCK, //
		TOKEN_CHARACTER, //
		TOKEN_STRING, //
		TOKEN_NUMBER, //
		TOKEN_PRIM_TYPE, //
		TOKEN_DEFED_TYPE, //
		KEYWORD_ASM, //
		KEYWORD_WHILE_IF_ELSE, //
		KEYWORD_CALL, //
		KEYWORD_FSTRUCT_STRUCT, //
		KEYWORD_CONST, //
		KEYWORD_MAIN_INIT, //
		KEYWORD_NOPAD, //
		KEYWORD_EXP, //
		KEYWORD_FUNC_AS_FUNC_ADDR, //
		KEYWORD_FUNC, //
		KEYWORD_DEP, //
		KEYWORD_TYPEDEF, //
		DECL_TYPEDEF_NAME, //
		DECL_DEP_NAME, //
		DECL_FUNC_NAME, //
		DECL_PARAM_RESULT_VARIABLE_NAME, //
		DECL_LOCAL_VARIABLE_NAME, //
		DECL_GLOBAL_VARIABLE_NAME, //
		REF_VALUE_REFERENCE_NAME, //
		REF_LOCAL_VARIABLE, //
		REF_GLOBAL_DEPENDENCY, //
		REF_GLOBAL_FUNCTION, //
		REF_GLOBAL_VARIABLE,//
	};
	
	private final IFile    file;
	private final IProject p;
	private final boolean  ssfMode;
	private DocumentTree   tree;
	private String         last;
	private ITypedRegion[] regions;
	
	public SsfDocumentPartitioner(IFile file) {
		this(file, file.getName().endsWith(".ssf"));
	}
	
	public SsfDocumentPartitioner(IFile file, boolean ssfMode) {
		this.file = file;
		this.ssfMode = ssfMode;
		this.p = file.getProject();
	}
	
	@Override
	public void connect(IDocument document) {
		checkChange(document.get());
	}
	
	@Override
	public void disconnect() {}
	
	@Override
	public void documentAboutToBeChanged(@SuppressWarnings("unused") DocumentEvent event) {}
	
	@Override
	public String[] getLegalContentTypes() {
		return LEGAL.clone();
	}
	
	@Override
	public String getContentType(int offset) {
		return this.regions[getIndex(offset)].getType();
	}
	
	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
		if ( offset == 0 && length == this.last.length() ) {
			System.out.println("all regions");
			return this.regions.clone();
		}
		int start = getIndex(offset);
		int end = start + 1;
		for (; end < this.regions.length && this.regions[end].getOffset() < offset + length; end++);
		return Arrays.copyOfRange(this.regions, start, end);
	}
	
	@Override
	public ITypedRegion getPartition(int offset) {
		documentChanged(null);
		return this.regions[getIndex(offset)];
	}
	
	private int getIndex(int offset) {
		ITypedRegion[] regs = this.regions;
		int low = 0;
		int high = regs.length - 1;
		if ( low == high ) {
			return low;
		}
		while ( true ) {
			int mid = ( low + high ) >>> 1;
			ITypedRegion reg = regs[mid];
			int off = reg.getOffset();
			if ( off > offset ) {
				high = mid - 1;
				if ( low > high ) {
					return high;
				}
			}
			int end = off + reg.getLength();
			if ( end > offset ) {
				return mid;
			}
			low = mid + 1;
			if ( low > high ) {
				return low;
			}
		}
	}
	
	@Override
	public boolean documentChanged(DocumentEvent event) {
		return checkChange(event.fDocument.get());
	}
	
	private boolean checkChange(String doc) {
		if ( doc.equals(this.last) ) {
			return true;
		}
		try (StringReader reader = new StringReader(doc)) {
			this.file.deleteMarkers(SimpleCodeBuilder.VOLATILE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
			buildTree(reader);
			rebuildPartitions();
			this.last = doc;
			return true;
		} catch (CoreException e) {
			if ( Activator.doLog(LogLevel.ERROR) ) {
				log("validation crashed: " + e);
			}
			return false;
		}
	}
	
	private void rebuildPartitions() {
		List<ITypedRegion> regions = new ArrayList<>();
		addTree(this.tree, 0, regions);
		this.regions = regions.toArray(new ITypedRegion[regions.size()]);
	}
	
	private int addTree(DocumentTree docTree, int off, List<ITypedRegion> regions) {
		for (FilePosition.FileRegion reg : docTree) {
			switch ( reg ) {
			case FilePosition.FileToken tok -> off = addTok(tok, off, regions);
			case DocumentTree dt -> off = addTree(dt, off, regions);
			}
		}
		return off;
	}
	
	private static int addTok(FileToken tok, int off, List<ITypedRegion> regions) {
		String type = switch ( tok.token() ) {
		case SimpleTokenStream.INVALID -> TOKEN_COMMENT;
		case SimpleTokenStream.BOOL_NOT -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.NOT_EQ -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.DIAMOND -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.MOD -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BIT_AND -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BOOL_AND -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.SMALL_OPEN -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.SMALL_CLOSE -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.STAR -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.PLUS -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.COMMA -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.MINUS -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.DIV -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.COLON -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.SEMI -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.LT -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.LARROW -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.SHIFT_LEFT -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.LE -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.ME -> DECL_DEP_NAME;
		case SimpleTokenStream.EQ -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.GT -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.GE -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.SHIFT_RIGTH -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.QUESTION -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.ARR_OPEN -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.ARR_CLOSE -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BIT_XOR -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.ASM -> KEYWORD_ASM;
		case SimpleTokenStream.BYTE -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.CALL -> KEYWORD_CALL;
		case SimpleTokenStream.CONST -> KEYWORD_CONST;
		case SimpleTokenStream.DEP -> KEYWORD_DEP;
		case SimpleTokenStream.DWORD -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.ELSE -> KEYWORD_WHILE_IF_ELSE;
		case SimpleTokenStream.EXP -> KEYWORD_EXP;
		case SimpleTokenStream.FPDWORD -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.FPNUM -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.FSTRUCT -> KEYWORD_FSTRUCT_STRUCT;
		case SimpleTokenStream.FUNC -> {
			for (DocumentTree p = tok.parent();; p = p.parent()) {
				int ps = p.global().state();
				if ( ps == SimpleExportFileParser.STATE_FUNCTION ) {
					yield KEYWORD_FUNC;
				}
				if ( ps == SimpleSourceFileParser.STATE_EX_CODE ) {
					yield KEYWORD_FUNC_AS_FUNC_ADDR;
				}
			}
		}
		case SimpleTokenStream.IF -> KEYWORD_WHILE_IF_ELSE;
		case SimpleTokenStream.INIT -> KEYWORD_MAIN_INIT;
		case SimpleTokenStream.MAIN -> KEYWORD_MAIN_INIT;
		case SimpleTokenStream.NOPAD -> KEYWORD_NOPAD;
		case SimpleTokenStream.NUM -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.STRUCT -> KEYWORD_FSTRUCT_STRUCT;
		case SimpleTokenStream.TYPEDEF -> KEYWORD_TYPEDEF;
		case SimpleTokenStream.UBYTE -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.UDWORD -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.UNUM -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.UWORD -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.WHILE -> KEYWORD_WHILE_IF_ELSE;
		case SimpleTokenStream.WORD -> TOKEN_PRIM_TYPE;
		case SimpleTokenStream.BLOCK_OPEN -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BIT_OR -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BOOL_OR -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BLOCK_CLOSE -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.BIT_NOT -> TOKEN_OTHER_SYMBOL;
		case SimpleTokenStream.NAME -> {
			for (DocumentTree p = tok.parent();; p = p.parent()) {
				switch ( p.global().state() ) {
				case SimpleSourceFileParser.STATE_EX_CODE_VAR_DECL:
					yield DECL_LOCAL_VARIABLE_NAME;
				case SimpleExportFileParser.STATE_FUNCTION:
					yield DECL_FUNC_NAME;
				case SimpleExportFileParser.STATE_DEPENDENCY:
					yield DECL_DEP_NAME;
				case SimpleExportFileParser.STATE_VAL_POSTFIX:
					yield REF_VALUE_REFERENCE_NAME;
				case SimpleExportFileParser.STATE_VAL_DIRECT: {
					yield switch ( p.global().info() ) {
					case DependencyVal _v -> REF_GLOBAL_DEPENDENCY;
					case FunctionVal _v -> REF_GLOBAL_FUNCTION;
					case VariableVal v -> {
						if ( ( v.sv().flags() & SimpleVariable.FLAG_GLOBAL ) != 0 ) {
							yield REF_GLOBAL_VARIABLE;
						}
						yield REF_LOCAL_VARIABLE;
					}
					default -> throw new AssertionError("illegal info " + p.global().info().getClass());
					};
				}
				case SimpleExportFileParser.STATE_TYPE_TYPEDEFED_TYPE:
					yield TOKEN_DEFED_TYPE;
				case SimpleExportFileParser.STATE_TYPE_FUNC_STRUCT_REF:
					yield REF_GLOBAL_FUNCTION;
				case SimpleExportFileParser.STATE_TYPE_FUNC_ADDR:
					yield DECL_PARAM_RESULT_VARIABLE_NAME;
				case SimpleExportFileParser.STATE_TYPEDEF:
					yield DECL_TYPEDEF_NAME;
				case SimpleExportFileParser.STATE_VARIALBE:
					yield DECL_GLOBAL_VARIABLE_NAME;
				default:
				}
			}
		}
		case SimpleTokenStream.NUMBER -> TOKEN_NUMBER;
		case SimpleTokenStream.STRING -> TOKEN_STRING;
		case SimpleTokenStream.CHARACTER -> TOKEN_CHARACTER;
		case SimpleTokenStream.ASM_BLOCK -> TOKEN_ASM_BLOCK;
		default -> throw new AssertionError(tok);
		};
		int start = tok.start().totalChar();
		int end = tok.end().totalChar();
		if ( start != off ) {
			ITypedRegion treg = new TypedRegion(off, start - off, TOKEN_WHITESPACE);
			regions.add(treg);
		}
		ITypedRegion treg = new TypedRegion(start, end - start, type);
		regions.add(treg);
		return end;
	}
	
	static void addMarker(IFile file, String msg, int line, int severity) {
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
	
	private static BiFunction<String, String, SimpleDependency> dep(IFile file, IProject p) throws CoreException {
		ProjectProps props = SimpleCodeBuilder.parseProps(p, null);
		if ( props == null ) return null;
		IPath fp = file.getFullPath();
		for (IFolder src : props.src()) {
			BiFunction<String, String, SimpleDependency> d = dep0(file, props, fp, src);
			if ( d != null ) return d;
		}
		if ( file.getName().endsWith(".sexp") ) {
			return dep0(file, props, fp, props.exp());
		}
		return null;
	}
	
	private static BiFunction<String, String, SimpleDependency> dep0(IFile file, ProjectProps props, IPath fp,
		IFolder src) {
		IPath sfp = src.getFullPath();
		if ( sfp.isPrefixOf(fp) ) {
			IFile exportFile = src.getFile(fp.makeRelativeTo(sfp));
			BiFunction<String, String, SimpleDependency> result = SimpleCodeBuilder.dep(props, sfp, exportFile, null);
			return (source, runtime) -> {
				try {
					return result.apply(source, runtime);
				} catch (CompileError ce) {
					addMarker(exportFile, ce.getLocalizedMessage(), ce.line, IMarker.SEVERITY_ERROR);
					addMarker(file, "the dependency " + source + " could not be parsed: " + ce.getLocalizedMessage(),
						-1, IMarker.SEVERITY_ERROR);
					return null;
				}
			};
		}
		return null;
	}
	
	private void buildTree(Reader reader) throws CoreException {
		DocumentTree tree = new DocumentTree();
		LogTokenStream sts = new LogTokenStream(reader, this.file, tree);
		BiFunction<String, String, SimpleDependency> dep = dep(this.file, this.p);
		if ( dep == null ) {
			SsfDocumentPartitioner.this.tree = null;
			return;
		}
		SimpleExportFileParser sp = createParser(tree, sts, dep);
		SimpleFile sf = new SimpleFile(this.file.toString(), this.file.toString());
		SimpleCodeBuilder.initilizeSimpleFile(sf);
		sp.parse(sf);
		SsfDocumentPartitioner.this.tree = tree;
	}
	
	private static final class LogTokenStream extends SimpleTokenStream {
		
		private final DocumentTree tree;
		private final IFile        myFile;
		
		private FilePosition pos;
		private FileToken    ftok;
		private boolean      b;
		private boolean      pushed;
		private boolean      pushedDynTok;
		
		public LogTokenStream(Reader in, IFile file, DocumentTree tree) {
			super(in, file.toString());
			this.myFile = file;
			this.tree = tree;
			this.pos = genPos();
		}
		
		@Override
		public void tok(int t) {
			this.pushed = true;
			super.tok(t);
		}
		
		@Override
		public String consumeDynTokSpecialText() {
			if ( this.b ) return super.consumeDynTokSpecialText();
			try {
				this.b = true;
				if ( this.pushedDynTok ) {
					this.pushed = false;
					this.pushedDynTok = false;
					return super.consumeDynTokSpecialText();
				}
				FileToken ft = this.ftok;
				// if consumeTok() was called pushedDynTok is set, this.ftok should not be null
				this.tree.parsedToken(ft);
				this.ftok = null;
				return super.consumeDynTokSpecialText();
			} finally {
				this.b = false;
			}
		}
		
		@Override
		public int consumeTok() {
			if ( this.b ) return super.consumeTok();
			try {
				this.b = true;
				if ( this.pushed ) {
					this.pushed = false;
					return super.consumeTok();
				}
				FileToken ft = this.ftok;
				if ( ft == null ) {
					ft = nfct();
				} else {
					this.ftok = null;
				}
				this.pushedDynTok = true;
				this.tree.parsedToken(ft);
				return super.consumeTok();
			} finally {
				this.b = false;
			}
		}
		
		@Override
		public void consume() {
			if ( this.b ) {
				super.consume();
				return;
			}
			try {
				this.b = true;
				FileToken ft = this.ftok;
				if ( ft == null ) {
					if ( this.pushed || this.pushedDynTok ) {
						this.pushed = false;
						this.pushedDynTok = false;
					}
					super.consume();
				} else {
					this.ftok = null;
					super.consume();
					this.tree.parsedToken(ft);
				}
			} finally {
				this.b = false;
			}
		}
		
		@Override
		public int tok() {
			if ( this.b ) return super.tok();
			try {
				this.b = true;
				if ( this.pushed ) {
					return super.tok();
				}
				this.pushedDynTok = false;
				FileToken ft = this.ftok;
				if ( ft == null ) {
					ft = nft();
					this.ftok = ft;
				}
				return ft.token();
			} finally {
				this.b = false;
			}
		}
		
		private FileToken nft() {
			FileToken ft;
			FilePosition start = genPos();
			int tok = super.tok();
			FilePosition end = genPos();
			int token = tok == INVALID ? COMMENT_TOKEN : tok;
			ft = new FilePosition.FileToken(start, token, end);
			return ft;
		}
		
		private FileToken nfct() {
			FileToken ft;
			FilePosition start = genPos();
			int tok = super.consumeTok();
			FilePosition end = genPos();
			int token = tok == INVALID ? COMMENT_TOKEN : tok;
			ft = new FilePosition.FileToken(start, token, end);
			return ft;
		}
		
		private FilePosition genPos() {
			return new FilePosition(super.totalChar(), super.line(), super.charInLine());
		}
		
		public FilePosition pos() {
			return this.pos;
		}
		
		@Override
		public void handleError(CompileError err) {
			addMarker(this.myFile, err.getLocalizedMessage(), err.line, IMarker.SEVERITY_ERROR);
		}
		
	}
	
	private SimpleExportFileParser createParser(DocumentTree tree, LogTokenStream sts,
		BiFunction<String, String, SimpleDependency> dep) {
		SimpleExportFileParser sp;
		if ( this.ssfMode ) {
			sp = new SimpleSourceFileParser(sts, dep) {
				
				@Override
				protected Object enterUnknownState() {
					FilePosition pos = sts.pos();
					return tree.enterState(pos, -1);
				}
				
				@Override
				protected Object maybeFinishUnknownState() {
					return sts.pos();
				}
				
				@Override
				protected Object enterState(int state) {
					FilePosition pos = sts.pos();
					return tree.enterState(pos, state);
				}
				
				@Override
				protected void exitState(int state, Object enterResult, Object additionalData) {
					FilePosition pos = sts.pos();
					tree.exitState(pos, state, additionalData, (FilePosition.FileState) enterResult);
				}
				
				@Override
				protected void remenberExitedState(int state, Object enterResult, Object enterUnknownEndMarker,
					Object additionalData) {
					if ( enterUnknownEndMarker instanceof FilePosition fp ) {
						tree.rememberExitedState((FilePosition.FileState) enterResult, fp, state, additionalData);
					} else {
						tree.rememberExitedState((FilePosition.FileState) enterResult,
							( (FilePosition.FileState) enterUnknownEndMarker ).start(), state, additionalData);
					}
				}
				
				@Override
				protected Object decidedState(int state, Object unknownStateResult) {
					return tree.decideState((FilePosition.FileState) unknownStateResult, state);
				}
				
				@Override
				protected Object[] decidedStates(int[] states, Object unknownStateResult) {
					return tree.decideStates((FilePosition.FileState) unknownStateResult, states);
				}
				
			};
		} else {
			sp = new SimpleExportFileParser(sts, dep) {
				
				@Override
				protected Object enterUnknownState() {
					FilePosition pos = sts.pos();
					return tree.enterState(pos, -1);
				}
				
				@Override
				protected Object maybeFinishUnknownState() {
					return sts.pos();
				}
				
				@Override
				protected Object enterState(int state) {
					FilePosition pos = sts.pos();
					return tree.enterState(pos, state);
				}
				
				@Override
				protected void exitState(int state, Object enterResult, Object additionalData) {
					FilePosition pos = sts.pos();
					tree.exitState(pos, state, additionalData, (FilePosition.FileState) enterResult);
				}
				
				@Override
				protected void remenberExitedState(int state, Object enterResult, Object enterUnknownEndMarker,
					Object additionalData) {
					if ( enterUnknownEndMarker instanceof FilePosition fp ) {
						tree.rememberExitedState((FilePosition.FileState) enterResult, fp, state, additionalData);
					} else {
						tree.rememberExitedState((FilePosition.FileState) enterResult,
							( (FilePosition.FileState) enterUnknownEndMarker ).start(), state, additionalData);
					}
				}
				
				@Override
				protected Object decidedState(int state, Object unknownStateResult) {
					return tree.decideState((FilePosition.FileState) unknownStateResult, state);
				}
				
				@Override
				protected Object[] decidedStates(int[] states, Object unknownStateResult) {
					return tree.decideStates((FilePosition.FileState) unknownStateResult, states);
				}
				
			};
		}
		return sp;
	}
	
	public DocumentTree tree() {
		return this.tree;
	}
	
	private static void log(String msg) {
		Activator.log("editor.partitioner", msg);
	}
	
}
