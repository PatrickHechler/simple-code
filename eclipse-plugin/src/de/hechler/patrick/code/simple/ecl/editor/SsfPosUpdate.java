package de.hechler.patrick.code.simple.ecl.editor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
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
import de.hechler.patrick.code.simple.parser.objects.value.DataVal;
import de.hechler.patrick.code.simple.parser.objects.value.DependencyVal;
import de.hechler.patrick.code.simple.parser.objects.value.FunctionVal;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;

public class SsfPosUpdate implements IPositionUpdater {
	
	public static final String TOKEN_WHITESPACE                = "token_Whitespace";
	public static final String TOKEN_COMMENT                   = "token_Comment";
	public static final String TOKEN_OTHER_SYMBOL              = "token_Seperator_Math_other_symbol";
	public static final String TOKEN_ASM_BLOCK                 = "token_Asm_block";
	public static final String TOKEN_CHARACTER                 = "token_Character";
	public static final String TOKEN_STRING                    = "token_String";
	public static final String TOKEN_NUMBER                    = "token_Number";
	public static final String TOKEN_PRIM_TYPE                 = "token_Type";
	public static final String TOKEN_DEFED_TYPE                = "keyword_Typedefed_type";
	public static final String KEYWORD_ASM                     = "keyword_Asm";
	public static final String KEYWORD_WHILE_IF_ELSE           = "keyword_While_If_else";
	public static final String KEYWORD_CALL                    = "keyword_Call";
	public static final String KEYWORD_FSTRUCT_STRUCT          = "keyword_Fstruct_Struct";
	public static final String KEYWORD_CONST                   = "keyword_Const";
	public static final String KEYWORD_MAIN_INIT               = "keyword_Main_Init";
	public static final String KEYWORD_NOPAD                   = "keyword_Nopad";
	public static final String KEYWORD_EXP                     = "keyword_Exp";
	public static final String KEYWORD_FUNC_AS_FUNC_ADDR       = "keyword_Func_As_func_addr";
	public static final String KEYWORD_FUNC                    = "keyword_Func";
	public static final String KEYWORD_DEP                     = "keyword_Dep";
	public static final String KEYWORD_TYPEDEF                 = "keyword_Typedef";
	public static final String DECL_TYPEDEF_NAME               = "decl_Typedef_name";
	public static final String DECL_DEP_NAME                   = "decl_Dependency_name";
	public static final String DECL_FUNC_NAME                  = "decl_Function_name";
	public static final String DECL_PARAM_RESULT_VARIABLE_NAME = "decl_Param_Result_Variable_name";
	public static final String DECL_LOCAL_VARIABLE_NAME        = "decl_Local_Variable_name";
	public static final String DECL_GLOBAL_VARIABLE_NAME       = "decl_Global_Variable_name";
	public static final String REF_VALUE_REFERENCE_NAME        = "ref_Value_Reference_name";
	public static final String REF_LOCAL_VARIABLE              = "ref_Local_Variable";
	public static final String REF_GLOBAL_DEPENDENCY           = "ref_Global_Dependency";
	public static final String REF_GLOBAL_FUNCTION             = "ref_Global_Function";
	public static final String REF_GLOBAL_VARIABLE             = "ref_Global_Variable";
	
	private static final int COMMENT_TOKEN = SimpleTokenStream.MAX_TOKEN + 1;
	
	private final IFile    file;
	private final IProject p;
	private final boolean  ssfMode;
	private DocumentTree   tree;
	private String         last;
	private ITypedRegion[] regions;
	
	public SsfPosUpdate(IFile file) {
		this(file, file.getName().endsWith(".ssf"));
	}
	
	public SsfPosUpdate(IFile file, boolean ssfMode) {
		this.file = file;
		this.ssfMode = ssfMode;
		this.p = file.getProject();
	}
	
	public void init(IDocument document) {
		checkChange(document);
	}
	
	public static SsfPosUpdate getSPU(IDocument doc) {
		for (IPositionUpdater pu : doc.getPositionUpdaters()) {
			if ( pu instanceof SsfPosUpdate spu0 ) {
				return spu0;
			}
		}
		return null;
	}
	
	public ITypedRegion[] computePartitioning(int offset, int length) {
		if ( offset >= this.last.length() ) {
			return new ITypedRegion[0];
		} else if ( offset < 0 ) {
			length += offset;
			offset = 0;
		}
		int maxLen = this.last.length() - offset;
		if ( length < maxLen ) {
			length = maxLen;
		}
		if ( offset == 0 && length == this.last.length() ) {
			System.out.println("all regions");
			return this.regions.clone();
		}
		int start = getIndex(offset);
		int end = start + 1;
		for (; end < this.regions.length && this.regions[end].getOffset() < offset + length; end++);
		return Arrays.copyOfRange(this.regions, start, end);
	}
	
	public ITypedRegion getPartition(int offset) {
		if ( offset >= this.last.length() ) {
			return this.regions[this.regions.length - 1];
		} else if ( offset < 0 ) {
			return this.regions[0];
		}
		return this.regions[getIndex(offset)];
	}
	
	private int getIndex(int offset) {
		ITypedRegion[] regs = this.regions;
		int low = 0;
		int high = regs.length - 1;
		while ( low <= high ) {
			int mid = ( low + high ) >>> 1;
			ITypedRegion reg = regs[mid];
			int off = reg.getOffset();
			if ( off > offset ) {
				high = mid - 1;
				continue;
			}
			int end = off + reg.getLength();
			if ( end > offset ) {
				return mid;
			}
			low = mid + 1;
		}
		throw new IllegalArgumentException(
			"len: " + this.last.length() + " off: " + offset + " regions: " + Arrays.toString(regs));
	}
	
	@Override
	public void update(DocumentEvent event) {
		checkChange(event.fDocument);
	}
	
	private void checkChange(IDocument doc) {
		String docStr = doc.get();
		if ( docStr.equals(this.last) ) {
			return;
		}
		if ( docStr.isEmpty() ) {
			this.regions = new ITypedRegion[1];
			this.regions[0] = new TypedRegion(0, 1, TOKEN_WHITESPACE);
			return;
		}
		try ( StringReader reader = new StringReader(docStr) ) {
			this.file.deleteMarkers(SimpleCodeBuilder.VOLATILE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
			buildTree(reader);
			rebuildPartitions(doc);
			this.last = docStr;
			return;
		} catch ( CoreException e ) {
			if ( Activator.doLog(LogLevel.ERROR) ) {
				log("validation crashed: " + e);
			}
			return;
		}
	}
	
	private void rebuildPartitions(IDocument doc) {
		List<ITypedRegion> regions = new LinkedList<>();
		int prevEnd = addTree(doc, this.tree, 0, regions);
		insertWSReg(prevEnd, regions, doc.getLength());
		this.regions = regions.toArray(new ITypedRegion[regions.size()]);
		for (ITypedRegion reg : regions) {
			System.out.println("reg: " + reg);
		}
	}
	
	private int addTree(IDocument doc, DocumentTree docTree, int off, List<ITypedRegion> regions) {
		for (FilePosition.FileRegion reg : docTree) {
			switch ( reg ) {
			case FilePosition.FileToken tok -> off = addTok(doc, tok, off, regions);
			case DocumentTree dt -> off = addTree(doc, dt, off, regions);
			}
		}
		return off;
	}
	
	private static int addTok(IDocument doc, FileToken tok, int off, List<ITypedRegion> regions) {
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
					case DataVal _v -> REF_VALUE_REFERENCE_NAME;
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
		case COMMENT_TOKEN -> TOKEN_COMMENT;
		default -> throw new AssertionError(tok);
		};
		final int start = tok.start().totalChar();
		final int end = tok.end().totalChar();
		if ( tok.start().line() != tok.end().line() ) {
			final int oldRegSize = regions.size();
			try {
				int offInLine = tok.start().charInLine();
				for (int l = tok.start().line(); l <= tok.end().line(); l++) {
					int len = doc.getLineLength(l - 1) - offInLine - 1;// use getLineInfo instead?
					int offset = doc.getLineOffset(l - 1) + offInLine;
					if ( offset + len > end ) {
						len = end - offset;
					}
					for (; Character.isWhitespace(doc.getChar(offset + len - 1)) && len > 0; len--);
					if ( len > 0 ) {// remove leading and trailing whitespace, trailing may be important for windows
						for (; Character.isWhitespace(doc.getChar(offset)); offset++, len--);
					}
					insertWSReg(off, regions, offset);
					ITypedRegion treg = new TypedRegion(offset, len, type);
					regions.add(treg);
					offInLine = 0;
					off = offset + len;
				}
				return off;// may differ if the token contains trailing whitespace
			} catch ( BadLocationException e ) {
				e.printStackTrace();
				regions.subList(oldRegSize, regions.size()).clear();
				insertWSReg(off, regions, start);
				ITypedRegion treg = new TypedRegion(start, end - start, type);
				regions.add(treg);
			}
		} else {
			insertWSReg(off, regions, start);
			ITypedRegion treg = new TypedRegion(start, end - start, type);
			regions.add(treg);
		}
		return end;
	}
	
	private static void insertWSReg(int prevEnd, List<ITypedRegion> regions, int myStart) {
		if ( myStart != prevEnd ) {
			ITypedRegion treg = new TypedRegion(prevEnd, myStart - prevEnd, TOKEN_WHITESPACE);
			regions.add(treg);
		}
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
		} catch ( CoreException e ) {
			if ( Activator.doLog(LogLevel.ERROR) ) {
				log("could not set a marker: " + e);
			}
		}
	}
	
	private static BiFunction<String,String,SimpleDependency> dep(IFile file, IProject p) throws CoreException {
		ProjectProps props = SimpleCodeBuilder.parseProps(p, null);
		if ( props == null ) return null;
		IPath fp = file.getFullPath();
		for (IFolder src : props.src()) {
			BiFunction<String,String,SimpleDependency> d = dep0(file, props, fp, src);
			if ( d != null ) return d;
		}
		if ( file.getName().endsWith(".sexp") ) {
			return dep0(file, props, fp, props.exp());
		}
		return null;
	}
	
	private static BiFunction<String,String,SimpleDependency> dep0(IFile file, ProjectProps props, IPath fp, IFolder src) {
		IPath sfp = src.getFullPath();
		if ( sfp.isPrefixOf(fp) ) {
			IFile exportFile = src.getFile(fp.makeRelativeTo(sfp));
			BiFunction<String,String,SimpleDependency> result = SimpleCodeBuilder.dep(props, sfp, exportFile, null);
			return (source, runtime) -> {
				try {
					return result.apply(source, runtime);
				} catch ( CompileError ce ) {
					addMarker(exportFile, ce.getLocalizedMessage(), ce.line, IMarker.SEVERITY_ERROR);
					addMarker(file, "the dependency " + source + " could not be parsed: " + ce.getLocalizedMessage(), -1,
						IMarker.SEVERITY_ERROR);
					return null;
				}
			};
		}
		return null;
	}
	
	private void buildTree(Reader reader) throws CoreException {
		DocumentTree tree = new DocumentTree();
		LogTokenStream sts = new LogTokenStream(reader, this.file, tree);
		BiFunction<String,String,SimpleDependency> dep = dep(this.file, this.p);
		if ( dep == null ) {
			SsfPosUpdate.this.tree = null;
			return;
		}
		SimpleExportFileParser sp = createParser(tree, sts, dep);
		SimpleFile sf = new SimpleFile(this.file.toString(), this.file.toString());
		SimpleCodeBuilder.initilizeSimpleFile(sf);
		sp.parse(sf);
		SsfPosUpdate.this.tree = tree;
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
				if ( ft.token() != EOF ) {
					this.tree.parsedToken(ft);
				}
				this.ftok = null;
				String specialText = super.consumeDynTokSpecialText();
				this.pos = ft.end();
				return specialText;
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
					super.consumeTok();
				}
				this.pos = ft.end();
				this.pushedDynTok = false;
				if ( ft.token() != EOF ) {
					this.tree.parsedToken(ft);
				}
				return ft.token();
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
					this.pushed = false;
					this.pushedDynTok = false;
					super.consume();
				} else {
					this.ftok = null;
					super.consume();
					this.pos = ft.end();
					if ( ft.token() != EOF ) {
						this.tree.parsedToken(ft);
					}
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
			while ( true ) {
				FilePosition start;
				int tok;
				try {// manually do what super.tok() does:
					int r = super.skipInitialWS();
					start = genPos();
					tok = super.findToken(r);
				} catch ( IOException e ) {
					addMarker(this.myFile, e.getLocalizedMessage(), super.line(), IMarker.SEVERITY_ERROR);
					start = genPos();
					return new FilePosition.FileToken(start, EOF, start);
				}
				FilePosition end = genPos();
				if ( tok == INVALID ) {
					FileToken ft = new FilePosition.FileToken(start, COMMENT_TOKEN, end);
					this.tree.parsedToken(ft);
					continue;
				}
				return new FilePosition.FileToken(start, tok, end);
			}
		}
		
		private FileToken nfct() {
			while ( true ) {
				FileToken ft;
				FilePosition start = genPos();
				int tok = super.consumeTok();
				FilePosition end = genPos();
				ft = new FilePosition.FileToken(start, tok, end);
				return ft;
			}
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
		BiFunction<String,String,SimpleDependency> dep) {
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
