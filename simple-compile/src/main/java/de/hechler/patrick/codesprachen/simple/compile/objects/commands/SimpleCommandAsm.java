package de.hechler.patrick.codesprachen.simple.compile.objects.commands;

import java.util.List;

import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimplePool;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;

public class SimpleCommandAsm implements SimpleCommand {
	
	public final SimplePool pool;
	public final AsmParam[] asmArguments;
	public final String     asmCode;
	public final AsmParam[] asmResults;
	
	public SimpleCommandAsm(SimplePool pool, List <AsmParam> asmArguments, String asmCode, List <AsmParam> asmResults) {
		this(pool, toa(asmArguments), asmCode, toa(asmResults));
	}
	
	private static AsmParam[] toa(List <AsmParam> list) {
		return list.toArray(new AsmParam[list.size()]);
	}
	
	public SimpleCommandAsm(SimplePool pool, AsmParam[] asmArguments, String asmCode, AsmParam[] asmResults) {
		this.pool = pool;
		this.asmArguments = asmArguments;
		this.asmCode = asmCode;
		this.asmResults = asmResults;
	}
	
//	private static final long calcLen(List <Command> asmCode) {
//		long len = 0L;
//		boolean alignable = false, alignMode = true;
//		for (int i = 0, s = asmCode.size(); i < s; i ++ ) {
//			Command cmd = asmCode.get(i);
//			if (cmd.getClass() != Command.class) {
//				if (alignable && alignMode) {
//					if ( (len & 7) != 0) {
//						len += 8 - (len & 7);
//					}
//				}
//				alignable = false;
//			} else if (cmd instanceof CompilerCommandCommand) {
//				CompilerCommandCommand ccc = (CompilerCommandCommand) cmd;
//				switch (ccc.directive) {
//				case align:
//					alignMode = true;
//					break;
//				case notAlign:
//					alignMode = false;
//					break;
//				case assertPos:
//				case setPos:
//					throw new InternalError("unsupported compiler/assembler command: " + ccc.directive.name() + " should not occur here");
//				default:
//					throw new InternalError("unknown compiler/assembler command: " + ccc.directive.name());
//				}
//			} else if (cmd instanceof ConstantPoolCommand) {
//				alignable = true;
//			} else {
//				throw new InternalError("unknown command class: " + cmd.getClass());
//			}
//			len += cmd.length();
//		}
//		return len;
//	}
	
	@Override
	public SimplePool pool() {
		return pool;
	}
	
	public static class AsmParam {
		
		public final SimpleValue value;
		public final int         register;
		
		public AsmParam(SimpleValue value, int register) {
			this.value = value;
			this.register = register;
		}
		
		public static AsmParam create(SimpleValue val, String xnn) {
			assert xnn.matches("^(X[0-9A-E][0-9A-F]|XF[0-9])$");
			int reg = PrimAsmConstants.X_ADD + Integer.parseInt(xnn.substring(1), 16);
			return new AsmParam(val, reg);
		}
		
	}
	
}
