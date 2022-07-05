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
}

@parser::members {
	private String string(String raw) {
		assert raw.charAt(0) == '"';
		assert raw.charAt(raw.length() - 1) == '"' : raw = raw.substring(1, raw.length() - 1);
		StringBuilder build = new StringBuilder(raw.length());
		for (int index = 0; index != -1;) {
			int newIndex = raw.indexOf('\\', index);
			build.append(raw, index, (newIndex == -1 ? raw.length() : newIndex) - index);
			index = newIndex;
		}
		return build.toString();
	}
}

simpleFile [SimpleFile file]:
	(
		dependency
		{file.addDependency($dependency.depend);}
		|
		variable
		|
		structure
		|
		function
	)*
	EOF
;

dependency returns [String depend]:
	DEP STRING SEMI
	{$depend = string($STRING.getText());}
;
variable:
	VAR EXP? type NAME SEMI
;
structure:
	STRUCT NAME OPEN_CODE_BLOCK
		namedTypeList
	CLOSE_CODE_BLOCK
;
function:
	FUNC EXP? MAIN? NAME
	OPEN_SMALL_BLOCK namedTypeList CLOSE_SMALL_BLOCK
	( ARROW_RIGTH SMALLER namedTypeList GREATHER )?
	commandBlock
;

value:
	expCond
;
expCond:
	expLOr
	(
		QUESTION_MARK value COLON expCond
	)?
;
expLOr:
	expLAnd
	(
		(
			SINGLE_OR
		)
		expLAnd
	)*
;
expLAnd:
	expOr
	(
		(
			SINGLE_AND
		)
		expOr
	)*
;
expOr:
	expXor
	(
		(
			DOUBLE_OR
		)
		expXor
	)*
;
expXor:
	expAnd
	(
		(
			XOR
		)
		expAnd
	)*
;
expAnd:
	expEq
	(
		(
			DOUBLE_AND
		)
		expEq
	)*
;
expEq:
	expRel
	(
		(
			EQUAL
			|
			NOT_EQUAL
		)
		expRel
	)*
;
expRel:
	expShift
	(
		(
			GREATHER
			|
			GREATHER_EQUAL
			|
			SMALLER_EQUAL
			|
			SMALLER
		)
		expShift
	)*
;
expShift:
	expAdd
	(
		(
			SHIFT_LEFT
			|
			SHIFT_LOGIC_RIGTH
			|
			SHIFT_ARITMETIC_RIGTH
		)
		expAdd
	)*
;
expAdd:
	expMul
	(
		(
			PLUS
			|
			MINUS
		)
		expMul
	)*
;
expMul:
	expCast
	(
		(
			STAR
			|
			DIVIDE
			|
			MODULO
		)
		expCast
	)*
;
expCast:
	(
		OPEN_SMALL_BLOCK type CLOSE_SMALL_BLOCK
	)?
	expUnary
;
expUnary:
	(
		PLUS
		|
		MINUS
		|
		SINGLE_AND
		|
		BITWISE_NOT
		|
		BOOLEAN_NOT
		
	)?
	expPostfix
;
expPostfix:
	expDirect
	(
		DIAMOND // dereference pointer
		|
		OPEN_ARRAY_BLOCK value CLOSE_ARRAY_BLOCK
	)*
;
expDirect:
	STRING+
	|
	CHARACTER
	|
	NUMBER_DEC
	|
	NUMBER_HEX
	|
	NUMBER_NHEX
	|
	NUMBER_UHEX
	|
	NUMBER_BIN
	|
	NUMBER_NBIN
	|
	NUMBER_OCT
	|
	NUMBER_NOCT
	|
	NUMBER_FP
	|
	NAME
	(
		COLON NAME
	)*
	|
	OPEN_SMALL_BLOCK value CLOSE_SMALL_BLOCK
;

type:
	(
		typePrim
		|
		typeStruct
		|
		typeFunc
	)
	(
		DIAMOND // pointer
		|
		OPEN_ARRAY_BLOCK
		value?
		CLOSE_ARRAY_BLOCK
	)*
;

typePrim:
	NUM
	|
	FPNUM
	|
	DWORD
	|
	WORD
	|
	BYTE
;

typeStruct:
	STRUCT NAME
;

typeFunc:
	OPEN_SMALL_BLOCK namedTypeList CLOSE_SMALL_BLOCK
	(
		ARROW_RIGTH SMALLER namedTypeList GREATHER
	)?
;

namedTypeList:
	(
		type NAME
		(
			COMMA type NAME
		)*
	)?
;

command:
	commandBlock
	|
	commandVarDecl
	|
	commandAssign
	|
	commandFuncCall
	|
	commandWhile
	|
	commandIf
;
commandBlock:
	OPEN_CODE_BLOCK
	(
		command
	)*
	CLOSE_CODE_BLOCK
;
commandVarDecl:
	VAR type NAME
	(
		ARROW_LEFT value
	)?
	SEMI
;
commandAssign:
	expPostfix ARROW_LEFT value SEMI
;
commandFuncCall:
	CALL POSTFIX_EXP
;
commandWhile:
	WHILE OPEN_SMALL_BLOCK value CLOSE_SMALL_BLOCK command
;
commandIf:
	IF OPEN_SMALL_BLOCK value CLOSE_SMALL_BLOCK command
	(
		ELSE command
	)?
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
