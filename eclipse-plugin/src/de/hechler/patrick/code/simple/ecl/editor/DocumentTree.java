package de.hechler.patrick.code.simple.ecl.editor;

import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.code.simple.ecl.editor.FilePosition.FileState;

public class DocumentTree implements FileRegion {
	
	private DocumentTree           parent;
	private FilePosition.FileState global;
	private List<Object>           entries = new ArrayList<>();
	
	public FilePosition.FileState enterState(FilePosition pos, int state) {
		FilePosition.FileState posState = new FilePosition.FileState(pos, state, null, null);
		enterState(posState);
		return posState;
	}
	
	public FilePosition.FileState decideState(FilePosition.FileState undecidedState, int state) {
		if ( undecidedState == this.global ) {
			FileState result = this.global.decide(state);
			this.global = result;
			return result;
		}
		Object last = this.entries.get(this.entries.size() - 1);
		DocumentTree dt = (DocumentTree) last;
		return dt.decideState(undecidedState, state);
	}
	
	public FilePosition.FileState[] decideStates(FilePosition.FileState undecidedState, int[] states) {
		List<Object> entries = this.entries;
		if ( undecidedState == this.global ) {
			FilePosition.FileState[] result = new FilePosition.FileState[states.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = undecidedState.decide(states[i]);
			}
			DocumentTree dt = new DocumentTree();
			this.entries = dt.entries;
			dt.entries = entries;
			for (int i = 0; i < result.length - 1; i++) {
				dt.enterState(result[i]);
				DocumentTree parent = i == result.length - 2 ? this : new DocumentTree();
				parent.entries.add(dt);
				dt.parent = parent;
				dt = parent;
			}
			this.global = result[result.length - 1];
			return result;
		}
		Object last = entries.get(entries.size() - 1);
		DocumentTree dt = (DocumentTree) last;
		return dt.decideStates(undecidedState, states);
	}
	
	private void enterState(FilePosition.FileState posState) {
		if ( this.global == null ) {
			this.global = posState;
			return;
		}
		if ( this.global.end() != null ) {
			throw new AssertionError("I am already complete");
		}
		int size = this.entries.size();
		if ( size == 0 ) {
			addNewSubTree(posState);
			return;
		}
		Object last = this.entries.get(size - 1);
		switch ( last ) {
		case DocumentTree dt when dt.global.end() == null -> dt.enterState(posState);
		case DocumentTree dt -> addNewSubTree(posState);
		case FilePosition.FileToken ft -> addNewSubTree(posState);
		default -> throw new AssertionError(last.getClass());
		}
	}
	
	private void addNewSubTree(FilePosition.FileState posState) {
		DocumentTree subDt = new DocumentTree();
		subDt.parent = this;
		subDt.enterState(posState);
		this.entries.add(subDt);
	}
	
	public void exitState(FilePosition pos, int state, Object info, FileState enterResult) {
		int size = this.entries.size();
		if ( size == 0 ) {
			finishGlobal(pos, state, info, enterResult);
			return;
		}
		Object last = this.entries.get(size - 1);
		switch ( last ) {
		case DocumentTree dt when dt.global.end() == null -> dt.exitState(pos, state, info, enterResult);
		case DocumentTree dt -> finishGlobal(pos, state, info, enterResult);
		case FilePosition.FileToken ft -> finishGlobal(pos, state, info, enterResult);
		default -> throw new AssertionError(last.getClass());
		}
	}
	
	private void finishGlobal(FilePosition pos, int state, Object info, FileState enterResult) throws AssertionError {
		if ( this.global != enterResult ) throw new AssertionError();
		this.global = this.global.finish(pos, state, info);
	}
	
	public void rememberExitedState(FilePosition.FileState start, FilePosition pos, int state, Object info) {
		if ( start == this.global ) {
			this.global = start.finish(pos, state, info);
			correctEntryPositions(pos, 1);// all rules should have at least one token (or have a nested rule)
			return;
		}
		int stc = start.start().totalChar();
		for (int i = this.entries.size(); --i >= 0;) {
			Object obj = this.entries.get(i);
			if ( obj instanceof DocumentTree dt ) {
				if ( dt.global.start().totalChar() <= stc ) {
					dt.rememberExitedState(start, pos, state, info);
					return;
				}
			} // else its a token, we want a state
		}
		throw new AssertionError("could not find the start");
	}
	
	private void correctEntryPositions(FilePosition pos, int minRetain) throws AssertionError {
		int size = this.entries.size();
		if ( size != 0 ) {
			size = retainSize(size, pos.totalChar());
			if ( size < minRetain ) {
				throw new AssertionError("size < minRetain");
			}
			if ( size != this.entries.size() ) {
				List<Object> subList = this.entries.subList(size, this.entries.size());
				int minParentRetain = this.parent.entries.size();
				this.parent.entries.addAll(subList);
				subList.clear();
				this.parent.correctEntryPositions(pos, minParentRetain);
			}
		}
	}
	
	private int retainSize(int size, int ptc) throws AssertionError {
		for (; size > 0; size--) {
			Object obj = this.entries.get(size - 1);
			switch ( obj ) {
			case DocumentTree dt when dt.global.start().totalChar() >= ptc:
				return size;
			case DocumentTree dt:
				break;
			case FilePosition.FileToken ft when ft.start().totalChar() >= ptc:
				return size;
			case FilePosition.FileToken ft:
				break;
			default:
				throw new AssertionError(obj.getClass());
			}
		}
		return size;
	}
	
	public void parsedToken(FilePosition.FileToken token) {
		int size = this.entries.size();
		if ( size == 0 ) {
			this.entries.add(token);
			return;
		}
		Object last = this.entries.get(size - 1);
		switch ( last ) {
		case DocumentTree dt when dt.global.end() == null -> dt.parsedToken(token);
		case DocumentTree dt -> this.entries.add(token);
		case FilePosition.FileToken ft -> this.entries.add(token);
		default -> throw new AssertionError(last.getClass());
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new StringBuilder());
		return sb.toString();
	}
	
	private void toString(StringBuilder sb, StringBuilder indent) {
		sb.append(indent).append(this.global).append(": {\n");
		indent.append("  ");
		for (Object obj : this.entries) {
			if ( obj instanceof DocumentTree dt ) {
				dt.toString(sb, indent);
			} else {
				sb.append(indent).append(obj).append('\n');
			}
		}
		indent.replace(indent.length() - 2, indent.length(), "");
		sb.append(indent).append("}\n");
	}
	
}
