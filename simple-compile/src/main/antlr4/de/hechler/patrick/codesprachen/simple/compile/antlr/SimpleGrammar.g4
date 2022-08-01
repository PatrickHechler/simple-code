/**
 * regex for grammar only:
 * '\s*returns' -> '' 
 * '\s*\[([^\[\]"]|("([^"\r\n]|\\")*"))*\]' -> '' 
 * '\s*{[^({}]|("([^"\r\n]|\\")*")|({[^({}]|("([^"\r\n]|\\")*")|({[^({}]|("([^"\r\n]|\\")*"))*}))*}))*}' -> '' 
 */
 grammar SimpleGrammar;

 @parser::header {
import java.util.function.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.*;
}

@parser::members {
	private String string(String raw) {
		assert raw.charAt(0) == '"' : raw;
		assert raw.charAt(raw.length() - 1) == '"' : raw;
		raw = raw.substring(1, raw.length() - 1);
		StringBuilder build = new StringBuilder(raw.length());
		for (int index = 0;;) {
			int newIndex = raw.indexOf('\\', index);
			build.append(raw, index, (newIndex == -1 ? raw.length() : newIndex) - index);
			index = newIndex;
			if (index != -1) {
				build.append(character0(raw.substring(index, index + 2)));
			} else break;
		}
		return build.toString();
	}
	private char character(String raw) {
		assert raw.charAt(0) == '\'';
		assert raw.charAt(raw.length() - 1) == '\'';
		return character(raw.substring(1, raw.length() - 2));
	}
	private char character0(String raw) {
		if (raw.length() == 1) {
			assert raw.charAt(0) != '\\';
			return raw.charAt(0);
		}
		assert raw.charAt(0) == '\\' : raw;
		switch (raw.charAt(0)) {
		case '\'':
			return '\'';
		case 'r':
			return '\r';
		case 'n':
			return '\n';
		case 't':
			return '\t';
		case '0':
			return '\0';
		default:
			throw new AssertionError(raw);
		}
	}
}

simpleFile [SimpleFile file]:
	(
		dependency
		{file.addDependency($dependency.name, $dependency.depend);}
		|
		variable [file]
		{file.addVariable($variable.vari, $variable.export);}
		|
		structure [file]
		{file.addStructure($structure.struct);}
		|
		function [file]
		{file.addFunction($function.func);}
	)*
	EOF
;

dependency returns [String name, String depend]:
	DEP NAME STRING SEMI
	{
		$name = $NAME.getText();
		$depend = string($STRING.getText());
	}
;
variable [SimplePool pool] returns [SimpleVariable vari, boolean export]:
	{$export = false;}
	VAR
	(
		EXP
		{$export = true;}
	)?
	type [pool] NAME SEMI
	{$vari = new SimpleVariable($type.t, $NAME.getText());}
;
structure [SimplePool pool] returns [SimpleStructType struct]:
	STRUCT NAME OPEN_CODE_BLOCK
		namedTypeList [pool]
	CLOSE_CODE_BLOCK
	{$struct = new SimpleStructType($NAME.getText(), $namedTypeList.list);}
;
function [SimpleFile file] returns [SimpleFunction func]:
	{
		List<SimpleVariable> results = null;
		boolean export = false;
		boolean main = false;
	}
	FUNC
	(
		EXP
		{export = true;}
	)?
	(
		MAIN
		{main = true;}
	)?
	NAME
	OPEN_SMALL_BLOCK args = namedTypeList [file] CLOSE_SMALL_BLOCK
	(
		ARROW_RIGTH SMALLER res = namedTypeList [file] GREATHER
		{
			results = $res.list;
		}
	)?
	{SimplePool pool = file.newFuncPool($args.list, results);}
	commandBlock [pool]
	{$func = new SimpleFunction(export, main, $NAME.getText(), $args.list, results, $commandBlock.cmd);}
;

value [SimplePool pool] returns [SimpleValue val]:
	expCond [pool]
	{$val = $expCond.val;}
;
expCond [SimplePool pool] returns [SimpleValue val]:
	f = expLOr [pool]
	{$val = $f.val;}
	(
		QUESTION_MARK p = value [pool] COLON n = expCond [pool]
		{$val = $val.addExpCond(pool, $p.val, $n.val);}
	)?
;
expLOr [SimplePool pool] returns [SimpleValue val]:
	f = expLAnd [pool]
	{$val = $f.val;}
	(
		(
			SINGLE_OR
		)
		o = expLAnd [pool]
		{$val = $val.addExpLOr(pool, $o.val);}
	)*
;
expLAnd [SimplePool pool] returns [SimpleValue val]:
	f = expOr [pool]
	{$val = $f.val;}
	(
		(
			SINGLE_AND
		)
		o = expOr [pool]
		{$val = $val.addExpLAnd(pool, $o.val);}
	)*
;
expOr [SimplePool pool] returns [SimpleValue val]:
	f = expXor [pool]
	{$val = $f.val;}
	(
		(
			DOUBLE_OR
		)
		o = expXor [pool]
		{$val = $val.addExpOr(pool, $o.val);}
	)*
;
expXor [SimplePool pool] returns [SimpleValue val]:
	f = expAnd [pool]
	{$val = $f.val;}
	(
		(
			XOR
		)
		o = expAnd [pool]
		{$val = $val.addExpXor(pool, $o.val);}
	)*
;
expAnd [SimplePool pool] returns [SimpleValue val]:
	f = expEq [pool]
	{$val = $f.val;}
	(
		(
			DOUBLE_AND
		)
		o = expEq [pool]
		{$val = $val.addExpAnd(pool, $o.val);}
	)*
;
expEq [SimplePool pool] returns [SimpleValue val]:
	f = expRel [pool]
	{$val = $f.val;}
	(
		{boolean equal;}
		(
			EQUAL
			{equal = true;}
			|
			NOT_EQUAL
			{equal = false;}
		)
		o = expRel [pool]
		{$val = $val.addExpEq(pool, equal, $o.val);}
	)*
;
expRel [SimplePool pool] returns [SimpleValue val]:
	f = expShift [pool]
	{$val = $f.val;}
	(
		{int type;}
		(
			GREATHER
			{type = SimpleValue.EXP_GREATHER;}
			|
			GREATHER_EQUAL
			{type = SimpleValue.EXP_GREATHER_EQUAL;}
			|
			SMALLER_EQUAL
			{type = SimpleValue.EXP_SMALLER_EQUAL;}
			|
			SMALLER
			{type = SimpleValue.EXP_SMALLER;}
		)
		o = expShift [pool]
		{$val = $val.addExpRel(pool, type, $o.val);}
	)*
;
expShift [SimplePool pool] returns [SimpleValue val]:
	f = expAdd [pool]
	{$val = $f.val;}
	(
		{int type;}
		(
			SHIFT_LEFT
			{type = SimpleValue.EXP_SHIFT_LEFT;}
			|
			SHIFT_LOGIC_RIGTH
			{type = SimpleValue.EXP_SHIFT_LOGIC_RIGTH;}
			|
			SHIFT_ARITMETIC_RIGTH
			{type = SimpleValue.EXP_SHIFT_ARITMETIC_RIGTH;}
		)
		o = expAdd [pool]
		{$val = $val.addExpShift(pool, type, $o.val);}
	)*
;
expAdd [SimplePool pool] returns [SimpleValue val]:
	f = expMul [pool]
	{$val = $f.val;}
	(
		{boolean add;}
		(
			PLUS
			{add = true;}
			|
			MINUS
			{add = false;}
		)
		o = expMul [pool]
		{$val = $val.addExpAdd(pool, add, $o.val);}
	)*
;
expMul [SimplePool pool] returns [SimpleValue val]:
	f = expCast [pool]
	{$val = $f.val;}
	(
		{int type;}
		(
			STAR
			{type = SimpleValue.EXP_MULTIPLY;}
			|
			DIVIDE
			{type = SimpleValue.EXP_DIVIDE;}
			|
			MODULO
			{type = SimpleValue.EXP_MODULO;}
		)
		o = expCast [pool]
		{$val = $val.addExpMul(pool, type, $o.val);}
	)*
;
expCast [SimplePool pool] returns [SimpleValue val]:
	{SimpleType t = null;}
	(
		OPEN_SMALL_BLOCK type [pool] CLOSE_SMALL_BLOCK
		{t = $type.t;}
	)?
	e = expUnary [pool]
	{
		if (t != null) {
			$val = $e.val.addExpCast(pool, t);
		} else {
			$val = $e.val;
		}
	}
;
expUnary [SimplePool pool] returns [SimpleValue val]:
	{int type = SimpleValue.EXP_UNARY_NONE;}
	(
		PLUS
		{type = SimpleValue.EXP_UNARY_PLUS;}
		|
		MINUS
		{type = SimpleValue.EXP_UNARY_MINUS;}
		|
		SINGLE_AND
		{type = SimpleValue.EXP_UNARY_AND;}
		|
		BITWISE_NOT
		{type = SimpleValue.EXP_UNARY_BITWISE_NOT;}
		|
		BOOLEAN_NOT
		{type = SimpleValue.EXP_UNARY_BOOLEAN_NOT;}
	)?
	e = expPostfix [pool]
	{$val = $e.val.addExpUnary(pool, type);}
;
expPostfix [SimplePool pool] returns [SimpleValue val]:
	f = expDirect [pool]
	{$val = $f.val;}
	(
		DIAMOND // dereference pointer
		{$val = $val.addExpDerefPointer(pool);}
		|
		OPEN_ARRAY_BLOCK o = value [pool] CLOSE_ARRAY_BLOCK
		{$val = $val.addExpArrayRef(pool, $o.val);}
	)*
;
expDirect [SimplePool pool] returns [SimpleValue val]:
	(
		{List<String> strs = new ArrayList<>();}
		(
			t = STRING
			{strs.add(string($t.getText()));}
		)+
		{
			SimpleValueDataPointer dataVal = new SimpleStringValue(strs);
			pool.registerDataValue(dataVal);
			$val = dataVal;
		}
	)
	|
	t = CHARACTER
	{$val = new SimpleCharacterValue(character($t.getText()));}
	|
	t = NUMBER_DEC
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText()));}
	|
	t = NUMBER_HEX
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText().substring(4), 16));}
	|
	t = NUMBER_NHEX
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText().substring(4), 16));}
	|
	t = NUMBER_UHEX
	{$val = new SimpleNumberValue(64, Long.parseUnsignedLong($t.getText().substring(5), 16));}
	|
	t = NUMBER_BIN
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText().substring(4), 2));}
	|
	t = NUMBER_NBIN
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText().substring(4), 2));}
	|
	t = NUMBER_OCT
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText().substring(4), 7));}
	|
	t = NUMBER_NOCT
	{$val = new SimpleNumberValue(64, Long.parseLong($t.getText().substring(4), 7));}
	|
	t = NUMBER_FP
	{$val = new SimpleFPNumberValue(Double.parseDouble($t.getText()));}
	|
	t = NAME
	{
		$val = pool.newNameUseValue($t.getText());
	}
	(
		COLON t = NAME
		{$val = $val.addExpNameRef(pool, $t.getText());}
	)*
	|
	OPEN_SMALL_BLOCK v = value [pool] CLOSE_SMALL_BLOCK
	{$val = $v.val;}
;

type [SimplePool pool] returns [SimpleType t]:
	(
		typePrim
		{$t = $typePrim.t;}
		|
		typeStruct [pool]
		{$t = $typeStruct.t;}
		|
		typeFunc [pool]
		{$t = $typeFunc.t;}
	)
	(
		DIAMOND // pointer
		{$t = new SimpleTypePointer($t);}
		|
		{SimpleValue val = null;}
		OPEN_ARRAY_BLOCK
		(
			value [pool]
			{val = $value.val;}
		)?
		CLOSE_ARRAY_BLOCK
		{$t = SimpleTypeArray.create($t, val);}
	)*
;

typePrim returns [SimpleType t]:
	NUM
	{$t = SimpleType.NUM;}
	|
	FPNUM
	{$t = SimpleType.FPNUM;}
	|
	DWORD
	{$t = SimpleType.DWORD;}
	|
	WORD
	{$t = SimpleType.WORD;}
	|
	BYTE
	{$t = SimpleType.BYTE;}
;

typeStruct [SimplePool pool] returns [SimpleStructType t]:
	STRUCT NAME
	{$t = pool.getStructure($NAME.getText());}
;

typeFunc [SimplePool pool] returns [SimpleType t]:
	{List<SimpleVariable> results = null;}
	OPEN_SMALL_BLOCK args = namedTypeList [pool] CLOSE_SMALL_BLOCK
	(
		ARROW_RIGTH SMALLER res = namedTypeList [pool] GREATHER
		{results = $res.list;}
	)?
	{$t = new SimpleFuncType($args.list, results);}
;

namedTypeList [SimplePool pool] returns [List<SimpleVariable> list]:
	{$list = new ArrayList<>();}
	(
		ft = type [pool] fn =  NAME
		{$list.add(new SimpleVariable($ft.t, $fn.getText()));}
		(
			COMMA ots = type [pool] ons = NAME
			{$list.add(new SimpleVariable($ots.t, $ons.getText()));}
		)*
	)?
;

command [SimplePool pool] returns [SimpleCommand cmd]:
	commandBlock [pool]
	{$cmd = $commandBlock.cmd;}
	|
	commandVarDecl [pool]
	{$cmd = $commandVarDecl.cmd;}
	|
	commandAssign [pool]
	{$cmd = $commandAssign.cmd;}
	|
	commandFuncCall [pool]
	{$cmd = $commandFuncCall.cmd;}
	|
	commandWhile [pool]
	{$cmd = $commandWhile.cmd;}
	|
	commandIf [pool]
	{$cmd = $commandIf.cmd;}
;
commandBlock [SimplePool pool] returns [SimpleCommandBlock cmd]:
	{
		List<SimpleCommand> cmds = new ArrayList<>();
		pool = pool.newSubPool();
	}
	OPEN_CODE_BLOCK
	(
		command [pool]
		{cmds.add($command.cmd);}
	)*
	CLOSE_CODE_BLOCK
	{$cmd = SimpleCommandBlock.create(pool, cmds);}
;
commandVarDecl [SimplePool pool] returns [SimpleCommandVarDecl cmd]:
	VAR type [pool] NAME
	{$cmd = SimpleCommandVarDecl.create(pool, $type.t, $NAME.getText());}
	(
		ARROW_LEFT value [pool]
		{$cmd.initValue($value.val);}
	)?
	SEMI
;
commandAssign [SimplePool pool] returns [SimpleCommandAssign cmd]:
	expPostfix [pool] ARROW_LEFT value [pool] SEMI
	{$cmd = SimpleCommandAssign.create(pool, $expPostfix.val, $value.val);}
;
commandFuncCall [SimplePool pool] returns [SimpleCommandFuncCall cmd]:
	CALL fn = NAME
	{String sn = null;}
	(
		on = NAME
		{sn = $on.getText();}
	)?
	expPostfix [pool] SEMI
	{$cmd = SimpleCommandFuncCall.create(pool, $fn.getText(), sn, $expPostfix.val);}
;
commandWhile [SimplePool pool] returns [SimpleCommandWhile cmd]:
	WHILE OPEN_SMALL_BLOCK value [pool] CLOSE_SMALL_BLOCK
	{SimpleCommand whileCmd;}
	(
		command [pool]
		{whileCmd = $command.cmd;}
		|
		SEMI
		{whileCmd = null;}
	)
	{$cmd = SimpleCommandWhile.create(pool, $value.val, whileCmd);}
;
commandIf [SimplePool pool] returns [SimpleCommandIf cmd]:
	IF OPEN_SMALL_BLOCK value [pool] CLOSE_SMALL_BLOCK
	ic = command [pool]
	{SimpleCommand elseCmd = null;}
	(
		ELSE
		ec = command [pool]
		{elseCmd = $ec.cmd;}
	)?
	{$cmd = SimpleCommandIf.create(pool, $value.val, $ic.cmd, elseCmd);}
;

STRING :
	'"'
	(
		~('"' | '\\')
		|
		'\\' ('\\' | '"' | 'r' | 'n' | 't' | '0')
	)*
	'"'
;
CHARACTER :
	'\''
	(
		~('\'' | '\\')
		|
		'\\' ('\\' | '\'' | 'r' | 'n' | 't' | '0')
	)*
	'\''
;

NUMBER_DEC  :    '-'? [0-9]+ ;
NUMBER_HEX  :  'HEX-' [0-9A-F]+ ;
NUMBER_NHEX : 'NHEX-' [0-9A-F]+ ;
NUMBER_UHEX : 'UHEX-' [0-9A-F]+ ;
NUMBER_BIN  :  'BIN-' [01]+ ;
NUMBER_NBIN : 'NBIN-' [01]+ ;
NUMBER_OCT  :  'OCT-' [0-7]+ ;
NUMBER_NOCT : 'NOCT-' [0-7]+ ;

NUMBER_FP :
	'-'?
	(
		[0-9]* '.' [0-9]+
		|
		[0-9]+ '.' [0-9]*
	)
;

DEP : 'dep' ;
STRUCT : 'struct' ;
FUNC : 'func' ;
VAR : 'var' ;
EXP : 'exp' ;
MAIN : 'main' ;

CALL : 'call' ;
WHILE : 'while' ;
IF : 'if' ;
ELSE : 'else' ;

NUM : 'num' ;
FPNUM : 'fpnum' ;
DWORD : 'dword' ;
WORD : 'word' | 'char' ;
BYTE : 'byte' ;

OPEN_CODE_BLOCK   : '{' ;
CLOSE_CODE_BLOCK  : '}' ;
OPEN_SMALL_BLOCK   : '(' ;
CLOSE_SMALL_BLOCK  : ')' ;
OPEN_ARRAY_BLOCK  : '[' ;
CLOSE_ARRAY_BLOCK : ']' ;

SEMI : ';' ;

COMMA : ',' ;

ARROW_RIGTH : '-->' ;
ARROW_LEFT : '<--' ;

SMALLER  : '<' ;
GREATHER : '>' ;

STAR : '*' ;
COLON : ':' ;

PLUS : '+' ;
MINUS : '-' ;

QUESTION_MARK : '?' ;
DOUBLE_OR : '||' ;
DOUBLE_AND : '&&' ;
SINGLE_OR : '|' ;
XOR : '^' ;
SINGLE_AND : '&' ;
EQUAL : '==' ;
NOT_EQUAL : '!=' ;
GREATHER_EQUAL : '>=' ;
SMALLER_EQUAL : '<=' ;
SHIFT_LEFT : '<<' ;
SHIFT_LOGIC_RIGTH : '>>>' ;
SHIFT_ARITMETIC_RIGTH : '>>' ;
DIVIDE : '/' ;
MODULO : '%' ;
BOOLEAN_NOT : '!' ;
BITWISE_NOT : '~' ;
DIAMOND : '#' ;

COMMENT_1 : '//' ( ~ [\r\n] )* [\r\n] -> skip ;
COMMENT_2 :
	'/*'
	(
		~'*'
		|
		'*' ~'/'
	)*
	'*/'
	-> skip
;
WS : [ \t\r\n]+ -> skip ;

NAME : [a-zA-Z][a-zA-Z_0-9]* ;
