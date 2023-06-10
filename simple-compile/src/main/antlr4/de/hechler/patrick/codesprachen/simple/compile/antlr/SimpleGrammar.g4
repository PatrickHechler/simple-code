//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.

/**
 * regex for grammar only:
 * '\s*returns' -> ''
 * '\s*\[([^\[\]"]|("([^"\r\n]|\\.)*"))*\]' -> ''
 * '\s*\{([^\{\}]*|"([^'\\]*|\\.)*"|'([^'\\]*|\\.)*')*\}' -> ''
 */
 grammar SimpleGrammar;

 @parser::header {
//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.

import java.util.function.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.SimpleCompiler;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAsm.*;
import de.hechler.patrick.codesprachen.simple.symbol.objects.*;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.*;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
}

@parser::members {
	private String string(String raw) {
		assert raw.charAt(0) == '"' : raw;
		assert raw.charAt(raw.length() - 1) == '"' : raw;
		return raw.substring(1, raw.length() - 1).translateEscapes();
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
		switch (raw.charAt(1)) {
		case '\\':
			return '\\';
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
		{file.addDependency($dependency.name, $dependency.depend, $dependency.runtime);}
		|
		variable [file]
		{file.addVariable($variable.vari);}
		|
		structure [file]
		{file.addStructure($structure.struct);}
		|
		function [file]
		{file.addFunction($function.func);}
		|
		constant [file]
		{file.addConstant($constant.c);}
	)*
	EOF
;

dependency returns [String name, String depend, String runtime]:
	DEP NAME s = STRING
	{
		$name = $NAME.getText();
		$depend = string($s.getText());
		$runtime = null;
	}
	(
		s = STRING
		{
			$runtime = string($s.getText());
		}
	)?
	SEMI
;
variable [SimplePool pool] returns [SimpleOffsetVariable vari]:
	{boolean export = false;}
	VAR
	(
		EXP
		{export = true;}
	)?
	type [pool] NAME SEMI
	{$vari = new SimpleOffsetVariable($type.t, $NAME.getText(), export);}
;
structure [SimplePool pool] returns [SimpleStructType struct]:
	STRUCT
	{boolean export = false;}
	(
		EXP
	)?
	NAME OPEN_CODE_BLOCK
		namedTypeList [pool]
	CLOSE_CODE_BLOCK
	{$struct = new SimpleStructType($NAME.getText(), export, $namedTypeList.list);}
;
function [SimpleFile file] returns [SimpleFunction func]:
	{
		List<SimpleOffsetVariable> results = null;
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
	{$func = new SimpleFunction(export, main, $NAME.getText(), $commandBlock.cmd, (SimpleFuncPool) pool);}
;
constant [SimpleFile file] returns [SimpleConstant c]:
	{
		boolean export = false;
	}
	CONST
	(
		EXP
		{export = true;}
	)?
	NAME ARROW_LEFT value [file] SEMI
	{$c = SimpleCompiler.createConstant($NAME.getText(), $value.val, export);}
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
		{$val = $val.addExpCond(pool.snapshot(), $p.val, $n.val);}
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
		{$val = $val.addExpLOr(pool.snapshot(), $o.val);}
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
		{$val = $val.addExpLAnd(pool.snapshot(), $o.val);}
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
		{$val = $val.addExpOr(pool.snapshot(), $o.val);}
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
		{$val = $val.addExpXor(pool.snapshot(), $o.val);}
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
		{$val = $val.addExpAnd(pool.snapshot(), $o.val);}
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
		{$val = $val.addExpEq(pool.snapshot(), equal, $o.val);}
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
		{$val = $val.addExpRel(pool.snapshot(), type, $o.val);}
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
		{$val = $val.addExpShift(pool.snapshot(), type, $o.val);}
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
		{$val = $val.addExpAdd(pool.snapshot(), add, $o.val);}
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
		{$val = $val.addExpMul(pool.snapshot(), type, $o.val);}
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
			$val = $e.val.addExpCast(pool.snapshot(), t);
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
	{
		if (type != SimpleValue.EXP_UNARY_NONE) {
			$val = $e.val.addExpUnary(pool.snapshot(), type);
		} else {
			$val = $e.val;
		}
	}
;
expPostfix [SimplePool pool] returns [SimpleValue val]:
	f = expDirect [pool]
	{$val = $f.val;}
	(
		DIAMOND // dereference pointer
		{$val = $val.addExpDerefPointer(pool.snapshot());}
		|
		OPEN_ARRAY_BLOCK o = value [pool] CLOSE_ARRAY_BLOCK
		{$val = $val.addExpArrayRef(pool.snapshot(), $o.val);}
		|
		COLON t = NAME
		{$val = $val.addExpNameRef(pool, $t.getText());}
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
		|
		fn = NAME
		(
			COLON
			sn = NAME
		)?
		{$t = pool.getFuncType($fn.getText(), $sn != null ? $sn.getText() : null);}
	)
	(
		DIAMOND
		{$t = new SimpleTypePointer($t);}
		|
		{SimpleValue val = null;}
		OPEN_ARRAY_BLOCK
		(
			value [pool]
			{val = $value.val;}
		)?
		CLOSE_ARRAY_BLOCK
		{$t = SimpleCompiler.createArray($t, val);}
	)*
;

typePrim returns [SimpleType t]:
	FPNUM
	{$t = SimpleType.FPNUM;}
	|
	UNUM
	{$t = SimpleType.UNUM;}
	|
	NUM
	{$t = SimpleType.NUM;}
	|
	DWORD
	{$t = SimpleType.DWORD;}
	|
	UDWORD
	{$t = SimpleType.UDWORD;}
	|
	WORD
	{$t = SimpleType.WORD;}
	|
	UWORD
	{$t = SimpleType.UWORD;}
	|
	BYTE
	{$t = SimpleType.BYTE;}
	|
	UBYTE
	{$t = SimpleType.UBYTE;}
;

typeStruct [SimplePool pool] returns [SimpleStructType t]:
	STRUCT t1 = NAME ( COLON t2 = NAME )?
	{$t = pool.getStructure($t1.getText(), $t2 != null ? $t2.getText() : null);}
;

typeFunc [SimplePool pool] returns [SimpleType t]:
	{List<SimpleOffsetVariable> results = null;}
	OPEN_SMALL_BLOCK args = namedTypeList [pool] CLOSE_SMALL_BLOCK
	(
		ARROW_RIGTH SMALLER res = namedTypeList [pool] GREATHER
		{results = $res.list;}
	)?
	{$t = new SimpleFuncType($args.list, results);}
	|
	FUNC t1 = NAME ( COLON t2 = NAME )?
	{$t = pool.getFuncType($t1.getText(), $t2.getText());}
;

namedTypeList [SimplePool pool] returns [List<SimpleOffsetVariable> list]:
	{$list = new ArrayList<>();}
	(
		ft = type [pool] fn =  NAME
		{$list.add(new SimpleOffsetVariable($ft.t, $fn.getText()));}
		(
			COMMA ots = type [pool] ons = NAME
			{$list.add(new SimpleOffsetVariable($ots.t, $ons.getText()));}
		)*
		COMMA?
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
	|
	commandAsm [pool]
	{$cmd = $commandAsm.cmd;}
;
commandBlock [SimplePool pool] returns [SimpleCommandBlock cmd]:
	{
		pool = pool.newSubPool();
		$cmd = ((SimpleSubPool)pool).block;
	}
	OPEN_CODE_BLOCK
	(
		command [pool]
		{pool.addCmd($command.cmd);}
	)*
	CLOSE_CODE_BLOCK
	{pool.seal();}
;
commandVarDecl [SimplePool pool] returns [SimpleCommandVarDecl cmd]:
	VAR type [pool] NAME
	{$cmd = SimpleCommandVarDecl.create(pool.snapshot(), $type.t, $NAME.getText());}
	(
		ARROW_LEFT value [pool]
		{$cmd.initValue($value.val);}
	)?
	SEMI
;
commandAssign [SimplePool pool] returns [SimpleCommandAssign cmd]:
	expPostfix [pool] ARROW_LEFT value [pool] SEMI
	{$cmd = SimpleCommandAssign.create(pool.snapshot(), $expPostfix.val, $value.val);}
;
commandFuncCall [SimplePool pool] returns [SimpleCommandFuncCall cmd]:
	CALL
	fn = NAME
	{String sn = null;}
	(
		COLON
		on = NAME
		{sn = $on.getText();}
	)?
	expPostfix [pool] SEMI
	{$cmd = SimpleCommandFuncCall.create(pool.snapshot(), $fn.getText(), sn, $expPostfix.val);}
;
commandWhile [SimplePool pool] returns [SimpleCommandWhile cmd]:
	WHILE OPEN_SMALL_BLOCK value [pool] CLOSE_SMALL_BLOCK
	{SimplePool subPool = pool.newSubPool();}
	command [subPool]
	{
		subPool.addCmd($command.cmd);
		subPool.seal();
		$cmd = SimpleCommandWhile.create(pool.snapshot(), $value.val, ((SimpleSubPool)subPool).block);
	}
;
commandIf [SimplePool pool] returns [SimpleCommandIf cmd]:
	IF OPEN_SMALL_BLOCK value [pool] CLOSE_SMALL_BLOCK
	{
		SimplePool ifPool = pool.newSubPool();
		SimplePool elsePool = null;
	}
	command [ifPool]
	{ifPool.addCmd($command.cmd);}
	(
		ELSE
		{elsePool = pool.newSubPool();}
		command [elsePool]
		{elsePool.addCmd($command.cmd);}
	)?
	{$cmd = SimpleCommandIf.create(pool.snapshot(), $value.val, ((SimpleSubPool)ifPool).block, elsePool == null ? null : ((SimpleSubPool)elsePool).block);}
;
commandAsm [SimplePool pool] returns [SimpleCommandAsm cmd]:
	{
		List<AsmParam> args = new ArrayList<>();
		List<AsmParam> res = new ArrayList<>();
	}
	ASM
	(
		value [pool] ARROW_RIGTH XNN
		{args.add(AsmParam.create($value.val, $XNN.getText()));}
	)*
	ASM_BLOCK
	(
		XNN ARROW_RIGTH value [pool]
		{res.add(AsmParam.create($value.val, $XNN.getText()));}
	)*
	SEMI
	{$cmd = new SimpleCommandAsm(pool.snapshot(), args, $ASM_BLOCK.getText(), res);}
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

XNN :
	'X'
	(
		[0-9A-E] [0-9A-F] 
		|
		'F' [0-9]
	)
;
ASM_BLOCK :
	'::'
	(
		~ ( '>' | '"' | '\'' | '|' )
		|
		'>' ~'>'
		|
		'"'
		(
			~ ( '"' | '\\' )
			|
			'\\' .
		)*
		'"'
		|
		'\''
		(
			~ ( '\'' | '\\' )
			|
			'\\' .
		)*
		'\''
		|
		'|'
		(
			~ ( '>' | ':' )
			|
			'>'
			(
				~ ( '\r' | '\n' )
			)*
			[\r\n]
			|
			':'
			(
				~ ( '|' )
				|
				'|'
				(
					~ ( '>' )
				)
			)*
			'|>'
		) 
	)*
	'>>'
;


DEP : 'dep' ;
STRUCT : 'struct' ;
FUNC : 'func' ;
VAR : 'var' ;
CONST : 'const' ;
EXP : 'exp' ;
MAIN : 'main' ;

CALL : 'call' ;
WHILE : 'while' ;
IF : 'if' ;
ELSE : 'else' ;
ASM : 'asm' ;

FPNUM : 'fpnum' ;
NUM : 'num' ;
UNUM : 'unum' ;
DWORD : 'dword' ;
UDWORD : 'udword' ;
WORD : 'word' ;
UWORD : 'uword' ;
BYTE : 'byte' ;
UBYTE : 'ubyte' | 'char' ;

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
