package de.hechler.patrick.code.simple.ecl.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public final class DocumentTree implements FilePosition.FileRegion, Iterable<FilePosition.FileRegion> {
	
	private DocumentTree                  parent;
	private int                           parentIndex;
	private FilePosition.FileState        global;
	private List<FilePosition.FileRegion> entries = new ArrayList<>();
	
	public FilePosition.FileState enterState(FilePosition pos, int state) {
		FilePosition.FileState posState = new FilePosition.FileState(pos, state, null, null);
		enterState(posState);
		return posState;
	}
	
	public FilePosition.FileState decideState(FilePosition.FileState undecidedState, int state) {
		if ( undecidedState == this.global ) {
			FilePosition.FileState result = this.global.decide(state);
			this.global = result;
			return result;
		}
		Object last = this.entries.get(this.entries.size() - 1);
		DocumentTree dt = (DocumentTree) last;
		return dt.decideState(undecidedState, state);
	}
	
	public FilePosition.FileState[] decideStates(FilePosition.FileState undecidedState, int[] states) {
		List<FilePosition.FileRegion> entries = this.entries;
		if ( undecidedState == this.global ) {
			FilePosition.FileState[] result = new FilePosition.FileState[states.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = undecidedState.decide(states[i]);
			}
			DocumentTree dt = new DocumentTree();
			this.entries = dt.entries;
			dt.entries = entries;
			for (ListIterator<FilePosition.FileRegion> iter = entries.listIterator(); iter.hasNext();) {
				FilePosition.FileRegion c = iter.next();
				if ( c instanceof DocumentTree cTree ) {
					cTree.parent = dt;
				} else {
					FilePosition.FileToken cTok = (FilePosition.FileToken) c;
					int parentIndex = iter.previousIndex();
					FilePosition cStart = cTok.start();
					int cToken = cTok.token();
					FilePosition cEnd = cTok.end();
					cTok = new FilePosition.FileToken(cStart, cToken, cEnd, dt, parentIndex);
					iter.set(cTok);
				}
			}
			for (int i = 0; i < result.length - 1; i++) {
				dt.enterState(result[i]);
				DocumentTree parent = i == result.length - 2 ? this : new DocumentTree();
				dt.parent = parent;
				dt.parentIndex = parent.entries.size();
				parent.entries.add(dt);
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
		subDt.parentIndex = this.entries.size();
		subDt.global = posState;
		this.entries.add(subDt);
	}
	
	public void exitState(FilePosition pos, int state, Object info, FilePosition.FileState enterResult) {
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
	
	private void finishGlobal(FilePosition pos, int state, Object info, FilePosition.FileState enterResult)
		throws AssertionError {
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
				List<FilePosition.FileRegion> subList = this.entries.subList(size, this.entries.size());
				DocumentTree p = this.parent;
				int oldParentSize = p.entries.size();
				for (int i = 0; i < subList.size(); i++) {
					int pi = oldParentSize + i;
					p.entries.add(switch ( subList.get(i) ) {
					case FilePosition.FileToken tok -> {
						FilePosition s = tok.start();
						int t = tok.token();
						FilePosition e = tok.end();
						FilePosition.FileToken ntok = new FilePosition.FileToken(s, t, e, p, pi);
						yield ntok;
					}
					case DocumentTree dt -> {
						dt.parent = parent;
						dt.parentIndex = pi;
						yield dt;
					}
					});
				}
				subList.clear();
				if ( p.global.end() != null ) {
					p.correctEntryPositions(p.global.end(), oldParentSize);
				}
			}
		}
	}
	
	private int retainSize(int size, int ptc) throws AssertionError {
		for (; size > 0; size--) {
			Object obj = this.entries.get(size - 1);
			switch ( obj ) {
			case DocumentTree dt when dt.global.start().totalChar() < ptc:
				return size;
			case DocumentTree dt:
				break;
			case FilePosition.FileToken ft when ft.start().totalChar() < ptc:
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
			this.entries.add(token.initParent(this, size));
			return;
		}
		Object last = this.entries.get(size - 1);
		switch ( last ) {
		case DocumentTree dt when dt.global.end() == null -> dt.parsedToken(token);
		case DocumentTree dt -> this.entries.add(token.initParent(this, size));
		case FilePosition.FileToken ft -> this.entries.add(token.initParent(this, size));
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
	
	public FilePosition.FileToken find(int offset) {
		List<FilePosition.FileRegion> es = this.entries;
		int low = 0;
		int high = es.size() - 1;
		while ( low <= high ) {
			int mid = ( low + high ) >>> 1;
			FilePosition.FileRegion e = es.get(mid);
			if ( e.start().totalChar() > offset ) {
				high = mid - 1;
				if ( high < 0 ) {
					while ( e.parentIndex() == 0 ) {
						e = e.parent();
					}
					e = e.parent().child(e.parentIndex() - 1);
					if ( e instanceof FilePosition.FileToken ft ) {
						return ft;
					}
					es = ( (DocumentTree) e ).entries;
					low = 0;
					high = es.size();
				}
				continue;
			}
			if ( mid + 1 < es.size() ) {
				// the whitespace is included here to fill the holes
				FilePosition end = es.get(mid + 1).start();
				if ( end != null && end.totalChar() <= offset ) {
					low = mid + 1;
					continue;
				}
			}
			if ( e instanceof FilePosition.FileToken ft ) {
				return ft;
			}
			es = ( (DocumentTree) e ).entries;
			low = 0;
			high = es.size() - 1;
		}
		throw new IllegalArgumentException("offset out of bounds: " + offset);
	}
	
	public int childCount() {
		return this.entries.size();
	}
	
	public FilePosition.FileRegion child(int index) {
		return this.entries.get(index);
	}
	
	@Override
	public DocumentTree parent() {
		return this.parent;
	}
	
	@Override
	public int parentIndex() {
		return parentIndex;
	}
	
	public FilePosition.FileState global() {
		return global;
	}
	
	@Override
	public FilePosition start() {
		return this.global.start();
	}
	
	@Override
	public FilePosition end() {
		return this.global.end();
	}
	
	@Override
	public Iterator<FilePosition.FileRegion> iterator() {
		return this.entries.iterator();
	}
	
}
