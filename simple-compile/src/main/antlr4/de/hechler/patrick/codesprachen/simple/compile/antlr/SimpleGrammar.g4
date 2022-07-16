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
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.*;
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
		{file.addDependency($dependency.depend);}
		|
		variable
		{file.addVariable($variable.vari);}
		|
		structure
		{file.addStructure($structure.struct);}
		|
		function
		{file.addFunction($function.func);}
	)*
	EOF
;

dependency returns [String depend]:
	DEP STRING SEMI
	{$depend = string($STRING.getText());}
;
variable returns [SimpleVariable vari]:
	VAR EXP? type NAME SEMI
	{$vari = new SimpleVariable($type.t, $NAME.getText());}
;
structure returns [SimpleStructure struct]:
	STRUCT NAME OPEN_CODE_BLOCK
		namedTypeList
	CLOSE_CODE_BLOCK
	{$struct = new SimpleStructure($NAME.getText(), $namedTypeList.list);}
;
function returns [SimpleFunction func]:
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
	OPEN_SMALL_BLOCK args = namedTypeList CLOSE_SMALL_BLOCK
	(
		ARROW_RIGTH SMALLER res = namedTypeList GREATHER
		{results = $res.list;}
	)?
	commandBlock
	{$func = new SimpleFunction(export, main, $NAME.getText(), $args.list, results, $commandBlock.cmd);}
;

value returns [SimpleValue val]:
	expCond
	{$val = $expCond.val;}
;
expCond returns [SimpleValue val]:
	f = expLOr
	{$val = $f.val;}
	(
		QUESTION_MARK p = value COLON n = expCond
		{$val = $val.addExpCond($p.val, $n.val);}
	)?
;
expLOr returns [SimpleValue val]:
	f = expLAnd
	{$val = $f.val;}
	(
		(
			SINGLE_OR
		)
		o = expLAnd
		{$val = $val.addExpLOr($o.val);}
	)*
;
expLAnd returns [SimpleValue val]:
	f = expOr
	{$val = $f.val;}
	(
		(
			SINGLE_AND
		)
		o = expOr
		{$val = $val.addExpLAnd($o.val);}
	)*
;
expOr returns [SimpleValue val]:
	f = expXor
	{$val = $f.val;}
	(
		(
			DOUBLE_OR
		)
		o = expXor
		{$val = $val.addExpOr($o.val);}
	)*
;
expXor returns [SimpleValue val]:
	f = expAnd
	{$val = $f.val;}
	(
		(
			XOR
		)
		o = expAnd
		{$val = $val.addExpXor($o.val);}
	)*
;
expAnd returns [SimpleValue val]:
	f = expEq
	{$val = $f.val;}
	(
		(
			DOUBLE_AND
		)
		o = expEq
		{$val = $val.addExpAnd($o.val);}
	)*
;
expEq returns [SimpleValue val]:
	f = expRel
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
		o = expRel
		{$val = $val.addExpEq(equal, $o.val);}
	)*
;
expRel returns [SimpleValue val]:
	f = expShift
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
		o = expShift
		{$val = $val.addExpRel(type, $o.val);}
	)*
;
expShift returns [SimpleValue val]:
	f = expAdd
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
		o = expAdd
		{$val = $val.addExpShift(type, $o.val);}
	)*
;
expAdd returns [SimpleValue val]:
	f = expMul
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
		o = expMul
		{$val = $val.addExpAdd(add, $o.val);}
	)*
;
expMul returns [SimpleValue val]:
	f = expCast
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
		o = expCast
		{$val = $val.addExpMul(type, $o.val);}
	)*
;
expCast returns [SimpleValue val]:
	{SimpleType t = null;}
	(
		OPEN_SMALL_BLOCK type CLOSE_SMALL_BLOCK
		{t = $type.t;}
	)?
	e = expUnary
	{
		if (t != null) {
			$val = $e.val.addExpCast(t);
		} else {
			$val = $e.val;
		}
	}
;
expUnary returns [SimpleValue val]:
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
	e = expPostfix
	{$val = $e.val.addExpUnary(type);}
;
expPostfix returns [SimpleValue val]:
	f = expDirect
	{$val = $f.val;}
	(
		DIAMOND // dereference pointer
		{$val = $val.addExpDerefPointer();}
		|
		OPEN_ARRAY_BLOCK o = value CLOSE_ARRAY_BLOCK
		{$val = $val.addExpArrayRef($o.val);}
	)*
;
expDirect returns [SimpleValue val]:
	(
		{List<String> strs = new ArrayList<>();}
		(
			t = STRING
			{strs.add(string($t.getText()));}
		)+
		{$val = new SimpleStringValue(strs);}
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
	{$val = new SimpleFPNumberValue(64, Double.parseDouble($t.getText()));}
	|
	t = NAME
	{$val = new SimpleVariableUseValue($t.getText());}
	(
		COLON t = NAME
		{$val = $val.addExpNameRef($t.getText());}
	)*
	|
	OPEN_SMALL_BLOCK v = value CLOSE_SMALL_BLOCK
	{$val = $v.val;}
;

type returns [SimpleType t]:
	(
		typePrim
		{$t = $typePrim.t;}
		|
		typeStruct
		{$t = $typeStruct.t;}
		|
		typeFunc
		{$t = $typeFunc.t;}
	)
	(
		DIAMOND // pointer
		{$t = $t.pointer();}
		|
		{SimpleValue val = null;}
		OPEN_ARRAY_BLOCK
		(
			value
			{val = $value.val;}
		)?
		CLOSE_ARRAY_BLOCK
		{$t = $t.array(val);}
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

typeStruct returns [SimpleStructType t]:
	STRUCT NAME
	{return new SimpleStructType($NAME.getText());}
;

typeFunc returns [SimpleType t]:
	{List<SimpleVariable> results = null;}
	OPEN_SMALL_BLOCK args = namedTypeList CLOSE_SMALL_BLOCK
	(
		ARROW_RIGTH SMALLER res = namedTypeList GREATHER
		{results = $res.list;}
	)?
	{$t = new SimpleFuncType($args.list, results);}
;

namedTypeList returns [List<SimpleVariable> list]:
	{$list = new ArrayList<>();}
	(
		ft = type fn =  NAME
		{$list.add(new SimpleVariable($ft.t, $fn.getText()));}
		(
			COMMA ots = type ons = NAME
			{$list.add(new SimpleVariable($ots.t, $ons.getText()));}
		)*
	)?
;

command returns [SimpleCommand cmd]:
	commandBlock
	{$cmd = $commandBlock.cmd;}
	|
	commandVarDecl
	{$cmd = $commandVarDecl.cmd;}
	|
	commandAssign
	{$cmd = $commandAssign.cmd;}
	|
	commandFuncCall
	{$cmd = $commandFuncCall.cmd;}
	|
	commandWhile
	{$cmd = $commandWhile.cmd;}
	|
	commandIf
	{$cmd = $commandIf.cmd;}
;
commandBlock returns [SimpleCommandBlock cmd]:
	{List<SimpleCommand> cmds = new ArrayList<>();}
	OPEN_CODE_BLOCK
	(
		command
		{cmds.add($command.cmd);}
	)*
	CLOSE_CODE_BLOCK
	{$cmd = SimpleCommandBlock.create(cmds);}
;
commandVarDecl returns [SimpleCommandVarDecl cmd]:
	VAR type NAME
	{$cmd = SimpleCommandVarDecl.create($type.t, $NAME.getText());}
	(
		ARROW_LEFT value
		{$cmd.initValue($value.val);}
	)?
	SEMI
;
commandAssign returns [SimpleCommandAssign cmd]:
	expPostfix ARROW_LEFT value SEMI
	{$cmd = SimpleCommandAssign.create($expPostfix.val, $value.val);}
;
commandFuncCall returns [SimpleCommandFuncCall cmd]:
	CALL expPostfix SEMI
	{$cmd = SimpleCommandFuncCall.create($expPostfix.val);}
;
commandWhile returns [SimpleCommandWhile cmd]:
	WHILE OPEN_SMALL_BLOCK value CLOSE_SMALL_BLOCK
	{SimpleCommand whileCmd;}
	(
		command
		{whileCmd = $command.cmd;}
		|
		SEMI
		{whileCmd = null;}
	)
	{$cmd = SimpleCommandWhile.create($value.val, whileCmd);}
;
commandIf returns [SimpleCommandIf cmd]:
	IF OPEN_SMALL_BLOCK value CLOSE_SMALL_BLOCK ic = command
	{SimpleCommand elseCmd = null;}
	(
		ELSE ec = command
		{elseCmd = $ec.cmd;}
	)?
	{$cmd = SimpleCommandIf.create($value.val, $ic.cmd, elseCmd);}
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

NAME : [a-zA-Z_]+ ;
