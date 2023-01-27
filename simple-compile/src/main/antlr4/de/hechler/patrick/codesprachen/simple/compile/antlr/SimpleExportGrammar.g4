 grammar SimpleExportGrammar;

 @parser::header {
import java.util.function.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAsm.*;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.*;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.*;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable.*;
}

simpleExports returns [SimpleExportable[] imported] :
	{
		Map<String, SimpleStructType> structs = new HashMap<>();
		List<SimpleExportable> imps = new ArrayList<>();
	}
	(
		export [structs]
		{imps.add($export.imp);}
		( LINE | EOF )
	)*
	{$imported = SimpleExportable.correctImports(structs, imps);}
	EOF
;

export [Map<String, SimpleStructType> structs] returns [SimpleExportable imp] :
	constExport
	{$imp = $constExport.imp;}
	|
	structExport
	{
		Object obj = structs.put($structExport.imp.name, $structExport.imp);
		assert obj == null;
		$imp = $structExport.imp;
	}
	|
	varExport
	{$imp = $varExport.imp;}
	|
	functionExport
	{$imp = $functionExport.imp;}
;


structExport returns [SimpleStructType imp]:
	STRUCT NAME_OR_NUMBER STRUCT varList STRUCT
	{$imp = new SimpleStructType($NAME_OR_NUMBER.getText(), true, $varList.list);}
;

varList returns [List<SimpleVariable> list]:
	{$list = new ArrayList<>();}
	(
		variable [null]
		{$list.add($variable.v);}
		(
			VAR_SEP
			variable [null]
			{$list.add($variable.v);}
		)*
	)?
;

constExport returns [SimpleExportable imp] :
	CONST name = NAME_OR_NUMBER CONST number = NAME_OR_NUMBER 
	{
		long num = Long.parseUnsignedLong($number.getText(), 16);
		$imp = new SimpleConstant($name.getText(), num, true);
	}
;

varExport returns [SimpleExportable imp] :
	VAR NAME_OR_NUMBER VAR variable [$NAME_OR_NUMBER.getText()]
	{$imp = $variable.v;}
;

functionExport returns [SimpleExportable imp] :
	FUNC addr = NAME_OR_NUMBER FUNC name = NAME_OR_NUMBER functionType
	{$imp = new SimpleFunction(Long.parseUnsignedLong($addr.getText(), 16), $name.getText(), (SimpleFuncType) $functionType.t);}
;

variable [String number] returns [SimpleVariable v] :
	NAME_OR_NUMBER NAME_TYPE_SEP type
	{
		if (number == null) {
			$v = new SimpleVariable($type.t, $NAME_OR_NUMBER.getText(), false);
		} else {
			$v = new SimpleVariable(Long.parseUnsignedLong(number, 16), $type.t, $NAME_OR_NUMBER.getText());
		}
	}
;

type returns [SimpleType t] :
	(
		primType
		{$t = $primType.t;}
		|
		structType
		{$t = $structType.t;}
		|
		functionType
		{$t = $functionType.t;}
	)
	(
		POINTER
		{$t = new SimpleTypePointer($t);}
		|
		ARRAY NAME_OR_NUMBER ARRAY
		{$t = new SimpleTypeArray($t, Integer.parseUnsignedInt($NAME_OR_NUMBER.getText(), 16));}
		|
		UNKNOWN_SIZE_ARRAY
		{$t = new SimpleTypeArray($t, -1);}
	)*
;

functionType returns [SimpleType t] :
	FUNC args = varList FUNC res = varList FUNC
	{$t = new SimpleFuncType($args.list, $res.list);}
;

structType returns [SimpleType t] :
	NAME_OR_NUMBER
	{$t = new SimpleFutureStructType($NAME_OR_NUMBER.getText());}
;

primType returns [SimpleType t] :
	PRIM_FPNUM
	{$t = SimpleType.NUM;}
	|
	PRIM_NUM
	{$t = SimpleType.NUM;}
	|
	PRIM_UNUM
	{$t = SimpleType.UNUM;}
	|
	PRIM_DWORD
	{$t = SimpleType.DWORD;}
	|
	PRIM_UDWORD
	{$t = SimpleType.UDWORD;}
	|
	PRIM_WORD
	{$t = SimpleType.WORD;}
	|
	PRIM_UWORD
	{$t = SimpleType.WORD;}
	|
	PRIM_BYTE
	{$t = SimpleType.BYTE;}
	|
	PRIM_UBYTE
	{$t = SimpleType.UBYTE;}
;

UNKNOWN_SIZE_ARRAY : ']' ;
ARRAY :              '[' ;
POINTER :            '#' ;

PRIM_FPNUM :        '.fp' ;
PRIM_NUM :          '.n' ;
PRIM_UNUM :         '.un' ;
PRIM_DWORD :        '.dw' ;
PRIM_UDWORD :       '.udw' ;
PRIM_WORD :         '.w' ;
PRIM_UWORD :        '.uw' ;
PRIM_BYTE :         '.b' ;
PRIM_UBYTE :        '.ub' ;

FUNC :              '~f' ;
VAR :               '~v' ;
STRUCT :            '~s' ;
CONST :             '~c' ;

NAME_TYPE_SEP :     ':' ;
VAR_SEP :           ',' ;

NAME_OR_NUMBER : [a-zA-Z_0-9]* ;

LINE : [\r\n]+ ;
