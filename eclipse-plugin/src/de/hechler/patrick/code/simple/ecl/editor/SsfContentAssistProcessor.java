package de.hechler.patrick.code.simple.ecl.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import de.hechler.patrick.code.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.code.simple.parser.SimpleTokenStream;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;

public class SsfContentAssistProcessor implements IContentAssistProcessor {
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offset) {
		if ( offset == 0 ) return null;
		SsfPosUpdate spu = SsfPosUpdate.getSPU(viewer.getDocument());
		if ( spu == null ) return null;
		FilePosition.FileToken tok = spu.tree().find(offset);
		final int replaceLen;
		List<String> toks = new ArrayList<>();
		try {
			if ( tok.token() == SimpleTokenStream.NAME ) {
				replaceLen = tok.end().totalChar() - offset;
				int start = tok.start().totalChar();
				toks.add(viewer.getDocument().get(start, offset - start));
			} else {
				replaceLen = 0;
				FilePosition.FileRegion before = tok;
				while ( true ) {
					if ( before instanceof FilePosition.FileToken ft ) {
						if ( ft.token() == SimpleTokenStream.NAME && ft.end().totalChar() == offset ) {
							tok = ft;
							int start = tok.start().totalChar();
							toks.add(viewer.getDocument().get(start, offset - start));
							break;
						}
						if ( ft.token() == SimpleTokenStream.COLON && ft.end().totalChar() >= offset ) {
							toks.add(":");
							break;
						}
						if ( ( ft.token() == SimpleTokenStream.ARR_CLOSE || ft.token() == SimpleTokenStream.DIAMOND )
							&& ft.end().totalChar() >= offset ) {
							toks.add("]");
							break;
						}
						if ( ft.token() == SimpleTokenStream.MAX_TOKEN + 1 ) {
							before = tok;
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
					if ( toks.size() != 1 ) {
						// fast check for multi-String literal and similar
						return null;
					}
					String nameStart = toks.get(0);
					return completesFromScope(offset, replaceLen, nameStart, scope, CFS_F_DEP | CFS_F_VAR | CFS_F_FNC);
				}
			}
			case SimpleSourceFileParser.STATE_VAL_POSTFIX -> {}
			case SimpleSourceFileParser.STATE_TYPE -> {}
			case SimpleSourceFileParser.STATE_TYPE_TYPEDEFED_TYPE -> {}
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
	
	private ICompletionProposal[] completesFromScope(final int offset, final int replaceLen, String nameStart, SimpleScope scope,
		final int flags) {
		final int nameStartLen = nameStart.length();
		Set<String> names = scope.availableNames();
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
			Object obj = scope.nameTypeOrDepOrFuncOrNull(n);
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
			} else {
				SimpleValue value = scope.nameValueOrNull(n, ErrorContext.NO_CONTEXT);
				if ( value instanceof VariableVal vv ) {
					if ( ( flags & CFS_F_VAR ) == 0 ) {
						return;
					}
					SimpleVariable sv = vv.sv();
					if ( ( sv.flags() & SimpleVariable.FLAG_GLOBAL ) != 0 ) {
						addInfo = "global variable: " + sv;
					} else {
						addInfo = "local variable: " + sv;
					}
				} else {
					return;
				}
			}
			CompletionProposal cp = new CompletionProposal(str, offset, replaceLen, str.length(), null, n, null, addInfo);
			System.out.println("replacementString=" + str + " , replacementOffset=" + offset + " , replacementLength="
				+ replaceLen + " , cursorPosition=" + ( offset + str.length() ) + " , displayString=" + n
				+ " , additionalProposalInfo=" + addInfo);
			c.accept(cp);
		}).toArray(l -> new ICompletionProposal[l]);
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
