package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.enums.CompilerCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.CompilerCommandCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.ConstantPoolCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;

import java.nio.file.Path;
import java.nio.charset.Charset;


public class SimpleCompiler extends StepCompiler<SimpleTU> {
	
	private final Charset cs;
	private final Path       srcRoot;
	private final Path[]     lockups;
	
	public SimpleCompiler(Charset cs, Path srcRoot, Path... lockups) {
		this.cs = cs;
		this.srcRoot = srcRoot;
		this.lockups = lockups;
	}
	
   protected SimpleTU init(Path source, Path target) {
		String name = source.getFileName().toString();
		int di = name.lastIndexOf('.');
		String end = name.substring(di + 1);
		String start = di == -1 ? name : name.substring(0, di);
       SimpleTU tu = new SimpleTU(CompileMode.possibleExe, source, soure.resolveSibling(start + ("." + DefMultiCompiler.SIMPLE_SYMBOL_FILE_END)), target);
		try (Reader r = Files.newBufferedReader(tu.source, cs)) {
			ANTLRInputStream in = new ANTLRInputStream();
			in.load(r, 1024, 1024);
			SimpleGrammarLexer lexer = new SimpleGrammarLexer(in);
			CommonTokenStream toks = new CommonTokenStream(lexer);
			SimpleGrammarParser parser = new SimpleGrammarParser(toks);
			parser.setErrorHandler(new BailErrorStrategy());
			pc.file = new SimpleFile(this);
			parser.simpleFile(pc.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    protected void precompile(SimpleTU tu) {
		assert tu.pos == -1L;
		SimpleFunction main = tu.mode == CompileMode.noExe ? null : tu.file.mainFunction();
		tu.pos = 0L;
		tu.expOut = Files.newBufferedWriter(target.exports, cs);
		if (main != null) {
			executableStart(tu);
		} else {
			Files.newOutputStream(target.target, StandardOpenOptions.CREATE, StandardOpenOptions.TRUNCATE_EXISTING).close();
		}
		fillData(tu);
    }

	private void executableStart(SimpleTU target) throws IOException {
		assert target.pos == 0L;
		try (InputStream in = getClass().getResourceAsStream(EXECUTABLE_START_FILE)) {
			try (OutputStream out = Files.newOutputStream(target.target, StandardOpenOptions.CREATE, StandardOpenOptions.TRUNCATE_EXISTING)) {
				byte[] buf = new byte[512];
				while (true) {
					int r = in.read(buf, 0, 512);
					if (r == -1) {
						break;
					}
					out.write(buf, 0, r);
					target.pos += r;
				}
			}
		}
		lm.log(LogMode.files, "made the file executable: ", target.source.toString(), "");
	}
	
	private void fillData(SimpleTU target) throws IOException {
		assert target.pos == Files.size(target.target);
		try (OutputStream out = Files.newOutputStream(target.target, StandardOpenOption.APPEND)) {
				target.outOfMemErrorAddr = target.pos;
				PrimitiveAssembler asm = new PrimitiveAssembler(out, null, new Path[0], true, false);
				Command outOfMem = new Command(Commands.CMD_INT, build(A_NUM, MY_INT_OUT_OF_MEM_ERROR), null);
				Command mov1 = new Command(Commands.CMD_MOV, build(A_SR, X00), build(A_NUM, 1L));
				Command iex = new Command(Commands.CMD_INT, build(A_NUM, INT_EXIT), null);
				Command depLoad = new Command(Commands.CMD_INT, build(A_NUM, MY_INT_DEP_LOAD_ERROR), null);
				long exitLen = mov1.length() + iex.length();
				target.pos += outOfMem.length() + exitLen;
				target.dependencyLoadErrorAddr = target.pos;
				target.pos += depLoad.length() + exitLen;
				asm.assemble(Arrays.asList(outOfMem, mov1, iex, depLoad, mov1, iex), Collections.emptyMap());
			assert (target.pos & 7) == 0;
			byte[] zeros = new byte[8];
			for (SimpleValueDataPointer dv : target.file.dataValues()) {
				dv.addr = target.pos;
				target.binary.appendContent(dv.data, 0, dv.data.length, NO_LOCK);
				target.pos += dv.data.length;
				fill8Zeros(zeros);
				align(target, zeros);
			}
			for (SimpleVariable sv : target.file.vars()) {
				int len = sv.type.byteCount();
				if (zeros.length < len) {
					zeros = new byte[len];
				}
				sv.addr = target.pos;
				target.binary.appendContent(zeros, 0, len, NO_LOCK);
				target.pos += len;
				fill8Zeros(zeros);
				align(target, zeros);
			}
		}
		assert target.pos == Files.size(target.target);
	}
	
	protected void compile(SimpleTU target) {
		SimpleFunction main = target.mode == CompileMode.noExe ? null : target.file.mainFunction();
		if (main != null) {
			correctMainAddress(target);
			compileFunction(target, main);
		}
		for (SimpleFunction sf : target.file.functions()) {
			if (sf == main) continue;
			compileFunction(target, sf);
		}
	}
	
	protected void finish(SimpleTU tu) {
		
   }
   
   private static class SimpleTU extends TranslationUnit {
        
		private final CompileMode mode;
       private final Path symTarget;
       
		private SimpleFile file;
		private Writer expOut;
//		private final List<Command> cmds = new TUCmdList();
		private final List<Command> cmds = new LinkedList<>();
		private long pos = -1L;
		private long outOfMemErrorAddr = -1L;
		private long dependencyLoadErrorAddr = -1L;
		
       private SimpleTU(CompileMode mode, Path source, Path symTarget, Path target) {
			super(source, target);
			this.mode = mode;
			this.symTarget = symTarget;
		}
		
	}
    
    private static enum CopileMode {
        noExe,
        possibleExe,
    }
    
}
