package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import static de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext.NO_CONTEXT;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;

public record FuncType(List<SimpleVariable> resMembers, List<SimpleVariable> argMembers, int flags)
	implements SimpleType {
	
	public static final int FLAG_EXPORT = 0x0100;
	public static final int FLAG_MAIN = 0x0200;
	public static final int FLAG_INIT = 0x0400;
	public static final int FLAG_FUNC_ADDRESS = 0x0800;
	public static final int FLAG_NOPAD = StructType.FLAG_NOPAD;
	public static final int ALL_FLAGS = FLAG_EXPORT | FLAG_MAIN | FLAG_INIT | FLAG_FUNC_ADDRESS | FLAG_NOPAD;
	public static final int STRUCTURAL_FLAGS = FLAG_NOPAD | FLAG_FUNC_ADDRESS;
	public static final int FLAG_NO_STRUCT = FLAG_FUNC_ADDRESS;
	
	public static final FuncType MAIN_TYPE = create(List.of(//
		new SimpleVariable(NativeType.UNUM, "argc", null, 0), //
		new SimpleVariable(PointerType.create(PointerType.create(NativeType.UBYTE, NO_CONTEXT), NO_CONTEXT), "argv",
			null, 0)//
	), List.of(new SimpleVariable(NativeType.UBYTE, "exitnum", null, 0)), FLAG_MAIN, NO_CONTEXT);
	
	public static final FuncType INIT_TYPE = create(List.of(), List.of(), FLAG_INIT, NO_CONTEXT);
	
	static {
		if ( Integer.bitCount(ALL_FLAGS) != 4 ) {
			throw new AssertionError("bit count of FuncType.ALL_FLAGS is not 4");
		}
	}
	
	public static FuncType create(List<SimpleVariable> resMembers, List<SimpleVariable> argMembers, int flags,
		ErrorContext ctx) {
		argMembers = List.copyOf(argMembers);
		if ( ( flags & ALL_FLAGS ) != flags ) {
			throw new IllegalArgumentException("illegal flags value: " + Integer.toHexString(flags));
		}
		for (int i = 0, s = argMembers.size(); i < s; i++) {
			String name = argMembers.get(i).name();
			StructType.checkDupName(argMembers, i + 1, name, ctx);
			StructType.checkDupName(resMembers, 0, name, ctx);
		}
		for (int i = 0, s = resMembers.size(); i < s; i++) {
			String name = resMembers.get(i).name();
			StructType.checkDupName(resMembers, i + 1, name, ctx);
		}
		return new FuncType(resMembers, argMembers, flags);
	}
	
	public FuncType asFStruct() {
		if ( ( this.flags & FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new IllegalStateException("I am already a fstruct");
		}
		return new FuncType(this.resMembers, this.argMembers, this.flags & ~FLAG_FUNC_ADDRESS);
	}
	
	@Override
	public long size() {
		return Math.max(StructType.size(this.resMembers, this.flags), StructType.size(this.argMembers, this.flags));
	}
	
	@Override
	public int align() {
		return Math.max(StructType.align(this.resMembers, this.flags), StructType.align(this.argMembers, this.flags));
	}
	
	@Override
	public SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError {
		if ( !equals(type) ) {
			SimpleType.castErrImplicit(this, type, ctx);
		}
		return this;
	}
	
	@Override
	public void checkCastable(SimpleType type, ErrorContext ctx, boolean explicit) throws CompileError {
		if ( !equals(type) ) {
			if ( explicit ) SimpleType.castErrExplicit(this, type, ctx);
			SimpleType.castErrImplicit(this, type, ctx);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( ( this.flags & FLAG_EXPORT ) != 0 ) {
			sb.append("exp ");
		}
		if ( ( this.flags & FLAG_MAIN ) != 0 ) {
			sb.append("main ");
		}
		if ( ( this.flags & FLAG_INIT ) != 0 ) {
			sb.append("init ");
		}
		if ( ( this.flags & FLAG_FUNC_ADDRESS ) == 0 ) {
			sb.append("fstruct ");
		}
		if ( ( this.flags & FLAG_NOPAD ) != 0 ) {
			sb.append("nopad ");
		}
		sb.append('<');
		append(sb, this.resMembers);
		sb.append("> <-- (");
		append(sb, this.resMembers);
		return sb.append(')').toString();
	}
	
	private static void append(StringBuilder sb, List<SimpleVariable> m) {
		if ( !m.isEmpty() ) {
			sb.append(m.get(0));
			for (int i = 1, s = m.size(); i < s; i++) {
				sb.append(", ").append(m.get(i));
			}
		}
	}
	
	@Override
	public String toStringSingleLine() {
		return toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.flags;
		result = StructType.hashCode(result, this.argMembers);
		result = StructType.hashCode(result, this.resMembers);
		return result;
	}
	
	public SimpleVariable member(String name, ErrorContext ctx, boolean allowFuncAddr) {
		if ( !allowFuncAddr && ( this.flags & FLAG_FUNC_ADDRESS ) != 0 ) {
			throw new CompileError(ctx, "a function address is no structure!");
		}
		for (SimpleVariable sv : this.resMembers) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		for (SimpleVariable sv : this.argMembers) {
			if ( name.equals(sv.name()) ) {
				return sv;
			}
		}
		throw new CompileError(ctx,
			"the structure has no member with the given name! name: " + name + " " + toStringSingleLine());
	}
	
	public void checkHasMember(String name, ErrorContext ctx, boolean allowFuncAddr) {
		member(name, ctx, allowFuncAddr);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		FuncType other = (FuncType) obj;
		if ( this.flags != other.flags ) return false;
		if ( !StructType.equals(this.argMembers, other.argMembers) ) return false;
		return StructType.equals(this.resMembers, other.resMembers);
	}
	
	public boolean isInvokableBy(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		FuncType other = (FuncType) obj;
		assert ( this.flags & FLAG_FUNC_ADDRESS ) != 0;
		if ( ( this.flags & STRUCTURAL_FLAGS & ~FLAG_FUNC_ADDRESS ) != ( other.flags & STRUCTURAL_FLAGS ) ) {
			return false;
		}
		if ( !StructType.equals(this.argMembers, other.argMembers) ) return false;
		return StructType.equals(this.resMembers, other.resMembers);
	}
	
	public boolean equalsIgnoreNonStructuralFlags(Object obj) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;
		FuncType other = (FuncType) obj;
		if ( ( this.flags & STRUCTURAL_FLAGS ) != ( other.flags & STRUCTURAL_FLAGS ) ) return false;
		if ( !StructType.equals(this.argMembers, other.argMembers) ) return false;
		return StructType.equals(this.resMembers, other.resMembers);
	}
	
}