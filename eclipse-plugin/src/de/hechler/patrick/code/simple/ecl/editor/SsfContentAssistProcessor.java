package de.hechler.patrick.code.simple.ecl.editor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import de.hechler.patrick.code.simple.ecl.Activator;
import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;
import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;
import de.hechler.patrick.code.simple.parser.objects.value.DependencyVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;

public class SsfContentAssistProcessor implements IContentAssistProcessor {
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offset) {
		if ( offset == 0 ) return null;
		IDocument doc = viewer.getDocument();
		SsfPosUpdate spu = SsfPosUpdate.getSPU(doc);
		if ( spu == null ) return null;
		FilePosition.FileToken tok = spu.tree().find(offset);
		final int replaceLen;
		String tokStr = null;
		try {
			if ( tok.token() == SimpleTokenStream.NAME ) {
				replaceLen = tok.end().totalChar() - offset;
				int start = tok.start().totalChar();
				tokStr = doc.get(start, offset - start);
			} else {
				replaceLen = 0;
				FilePosition.FileRegion before = tok;
				while ( true ) {
					if ( before instanceof FilePosition.FileToken ft ) {
						if ( ft.token() == SimpleTokenStream.NAME && ft.end().totalChar() == offset ) {
							tok = ft;
							int start = tok.start().totalChar();
							tokStr = doc.get(start, offset - start);
							break;
						}
						if ( ft.token() == SimpleTokenStream.COLON && ft.end().totalChar() <= offset ) {
							tok = ft;
							tokStr = ":";
							break;
						}
						if ( ft.token() == SimpleTokenStream.MAX_TOKEN + 1 ) {
							before = ft;
							while ( before.parentIndex() == 0 ) {
								before = before.parent();
								if ( before == null ) return null;
							}
							before = before.parent().child(before.parentIndex() - 1);
							continue;
						}
						if ( ft.parentIndex() > 0 && ft.start().totalChar() >= offset ) {
							before = tok.parent().child(tok.parentIndex() - 1);
							continue;
						}
					} else {
						DocumentTree dt = (DocumentTree) before;
						before = dt.child(dt.childCount() - 1);
						continue;
					}
					return null;
				}
			}
			DocumentTree dt = tok.parent();
			switch ( dt.global().state() ) {
			case SimpleSourceFileParser.STATE_VAL_DIRECT -> {
				DocumentTree pdt = parent(dt, SimpleSourceFileParser.STATE_EX_CODE);
				if ( pdt == null ) pdt = spu.tree();
				Object inf = pdt.global().info();
				if ( inf instanceof SimpleScope scope ) {
					return completesFromScope(offset, replaceLen, tokStr, scope.availableNames(), str -> {
						Object res = scope.nameTypeOrDepOrFuncOrNull(str);
						if ( res != null ) return res;
						return scope.nameValueOrNull(str, ErrorContext.NO_CONTEXT);
					}, CFS_F_DEP | CFS_F_VAR | CFS_F_FNC);
				}
			}
			case SimpleSourceFileParser.STATE_VAL_POSTFIX -> {
				DocumentTree pdt = parent(dt, SimpleSourceFileParser.STATE_EX_CODE);
				if ( pdt == null ) pdt = spu.tree();
				Object inf = pdt.global().info();
				if ( inf instanceof SimpleScope scope ) {
					int pi = tok.parentIndex();
					Object obj = dt.child(0) instanceof DocumentTree first ? first.global().info() : null;
					if ( obj instanceof DependencyVal dv ) {
						obj = dv.dep();
					} else if ( obj instanceof SimpleValue sv ) {
						obj = sv.type();
					} else {
						return null;
					}
					boolean iterEndWithColon = false;
					for (int i = 1; i < pi; i++) {
						FilePosition.FileRegion c = dt.child(i);
						if ( !( c instanceof FilePosition.FileToken ft ) ) return null;
						if ( ft.token() == SimpleTokenStream.COLON ) {
							if ( ++i == pi ) {
								iterEndWithColon = true;
								break;
							}
							c = dt.child(i);
							if ( !( c instanceof FilePosition.FileToken ft0 ) ) return null;
							if ( ft0.token() != SimpleTokenStream.NAME ) return null;
							int start = ft0.start().totalChar();
							int len = ft0.end().totalChar() - start;
							String name = doc.get(start, len);
							if ( obj instanceof SimpleScope s ) {
								SimpleValue val = scope.nameValueOrNull(name, ErrorContext.NO_CONTEXT);
								if ( val == null ) return null;
								if ( val instanceof DependencyVal dv ) {
									obj = dv.dep();
								} else {
									obj = val.type();
								}
							} else if ( obj instanceof StructType struct ) {
								try {
									obj = struct.member(name, ErrorContext.NO_CONTEXT).type();
								} catch ( @SuppressWarnings("unused") CompileError e ) {
									return null;
								}
							} else if ( obj instanceof FuncType func && ( func.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
								try {
									obj = func.member(name, ErrorContext.NO_CONTEXT, false).type();
								} catch ( @SuppressWarnings("unused") CompileError e ) {
									return null;
								}
							} else {
								return null;
							}
						} else if ( ft.token() == SimpleTokenStream.ARR_OPEN ) {
							i += 2;
							if ( i >= pi ) {
								Activator.log("conent-assist", "something went wrong:\n"
									+ "  I landed in the middle of an array/pointer index, but outside of the index value");
								return null;
							}
							if ( obj instanceof PointerType type ) {
								obj = type.target();
							} else if ( obj instanceof ArrayType type ) {
								obj = type.target();
							} else {
								return null;
							}
						} else if ( ft.token() == SimpleTokenStream.DIAMOND ) {
							if ( obj instanceof PointerType type ) {
								obj = type.target();
							} else if ( obj instanceof ArrayType type ) {
								obj = type.target();
							} else {
								return null;
							}
						} else {
							return null;
						}
					}
					Set<String> names;
					if ( iterEndWithColon ) {
						if ( ":".equals(tokStr) ) return null;
					} else if ( ":".equals(tokStr) ) {
						tokStr = "";
					} else return null;
					int flags = CFS_F_DEP | CFS_F_VAR | CFS_F_FNC;
					if ( obj instanceof SimpleDependency dep ) {
						names = dep.availableNames();
					} else if ( obj instanceof StructType struct ) {
						flags |= CFS_F_IS_STRUCT;
						names = new HashSet<>();
						struct.members().stream().map(v -> v.name()).forEach(names::add);
					} else if ( obj instanceof FuncType func && ( func.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
						flags |= CFS_F_IS_STRUCT;
						names = new HashSet<>();
						func.argMembers().stream().map(v -> v.name()).forEach(names::add);
						func.resMembers().stream().map(v -> v.name()).forEach(names::add);
					} else {
						return null;
					}
					final Object fobj = obj;
					return completesFromScope(offset, replaceLen, tokStr, names, str -> {
						if ( fobj instanceof SimpleDependency dep ) {
							Object res = dep.nameTypeOrDepOrFuncOrNull(str);
							if ( res != null ) return res;
							return dep.nameValueOrNull(str, ErrorContext.NO_CONTEXT);
						} else if ( fobj instanceof StructType type ) {
							try {
								return type.member(str, ErrorContext.NO_CONTEXT);
							} catch ( @SuppressWarnings("unused") CompileError e ) {
								return null;
							}
						} else if ( fobj instanceof FuncType type && ( type.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
							try {
								return type.member(str, ErrorContext.NO_CONTEXT, false);
							} catch ( @SuppressWarnings("unused") CompileError e ) {
								return null;
							}
						} else {
							return null;
						}
					}, flags);
				}
			}
			case SimpleSourceFileParser.STATE_TYPE, SimpleSourceFileParser.STATE_TYPE_TYPEDEFED_TYPE -> {
				DocumentTree pdt = parent(dt, SimpleSourceFileParser.STATE_EX_CODE);
				if ( pdt == null ) pdt = spu.tree();
				Object inf = pdt.global().info();
				if ( inf instanceof SimpleScope scope ) {
					return completesFromScope(offset, replaceLen, tokStr, scope.availableNames(), str -> {
						Object res = scope.nameTypeOrDepOrFuncOrNull(str);
						if ( res != null ) return res;
						return scope.nameValueOrNull(str, ErrorContext.NO_CONTEXT);
					}, CFS_F_DEP | CFS_F_TYP);
				}
			}
			case SimpleSourceFileParser.STATE_TYPE_FUNC_ADDR -> {}
			case SimpleSourceFileParser.STATE_TYPE_FUNC_STRUCT -> {}
			case SimpleSourceFileParser.STATE_TYPE_FUNC_ADDR_REF -> {}
			case SimpleSourceFileParser.STATE_TYPE_FUNC_STRUCT_REF -> {}
			case SimpleSourceFileParser.STATE_TYPE_POSTFIX -> {}
			case SimpleSourceFileParser.STATE_EX_CODE_VAR_DECL -> {}
			default -> {}
			}
		} catch ( BadLocationException e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static final int CFS_F_DEP = 0x01;
	private static final int CFS_F_FNC = 0x02;
	private static final int CFS_F_TYP = 0x04;
	private static final int CFS_F_VAR = 0x08;
	
	private static final int CFS_F_IS_STRUCT = 0x10;
	
	private ICompletionProposal[] completesFromScope(final int offset, final int replaceLen, String nameStart, Set<String> names,
		Function<String,Object> getInfo, final int flags) {
		final int nameStartLen = nameStart.length();
		if ( ( flags & CFS_F_TYP ) != 0 ) {
			names.add("num");
			names.add("unum");
			names.add("fpnum");
			names.add("fpdword");
			names.add("dword");
			names.add("udword");
			names.add("word");
			names.add("uword");
			names.add("byte");
			names.add("ubyte");
		}
		return names.stream().filter(n -> n.startsWith(nameStart)).mapMulti((n, c) -> {
			String str = n.substring(nameStartLen);
			String addInfo = null;
			Object obj = getInfo.apply(n);
			if ( obj instanceof SimpleDependency dep ) {
				if ( ( flags & CFS_F_DEP ) == 0 ) {
					return;
				}
				addInfo = "dependency: "//
					+ ( dep.sourceFile == null ? dep.toString() : +'"' + dep.sourceFile + '"' )//
					+ ( dep.binaryTarget == null ? "" : " \"" + dep.binaryTarget + '"' );
			} else if ( obj instanceof SimpleFunction func ) {
				if ( ( flags & CFS_F_FNC ) == 0 ) {
					return;
				}
				addInfo = "function: " + ( func.dep().sourceFile == null ? func.dep().toString() : func.dep().sourceFile ) + ":"
					+ func.name() + ": " + func.type().toString();
			} else if ( obj instanceof SimpleType type ) {
				if ( ( flags & CFS_F_TYP ) == 0 ) {
					return;
				}
				addInfo = "type: " + type;
			} else if ( obj instanceof VariableVal vv ) {
				if ( ( flags & CFS_F_VAR ) == 0 ) {
					return;
				}
				addInfo = varAddInfo(flags, vv.sv());
			} else if ( obj instanceof SimpleVariable sv ) {
				if ( ( flags & CFS_F_VAR ) == 0 ) {
					return;
				}
				addInfo = varAddInfo(flags, sv);
			} else {
				return;
			}
			CompletionProposal cp = new CompletionProposal(str, offset, replaceLen, str.length(), null, n, null, addInfo);
			c.accept(cp);
		}).toArray(l -> new ICompletionProposal[l]);
	}
	
	private String varAddInfo(final int flags, SimpleVariable sv) {
		String addInfo;
		if ( ( flags & CFS_F_IS_STRUCT ) != 0 ) {
			addInfo = "struct member: " + sv;
		} else if ( ( sv.flags() & SimpleVariable.FLAG_GLOBAL ) != 0 ) {
			addInfo = "global variable: " + sv;
		} else {
			addInfo = "local variable: " + sv;
		}
		return addInfo;
	}
	
	private DocumentTree parent(DocumentTree dt, int wantedState) {
		DocumentTree pdt = dt.parent();
		while ( pdt != null ) {
			if ( pdt.global().state() == wantedState ) {
				break;
			}
			pdt = pdt.parent();
		}
		return pdt;
	}
	
	@Override
	@SuppressWarnings("unused")
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}
	
	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[]{ ':' };
	}
	
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}
	
	@Override
	public String getErrorMessage() {
		return null;
	}
	
	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
	
}
