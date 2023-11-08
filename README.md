# simple-code

simple-code is a simple programming language.

## file

`( dependency | variable | structure | function | constant )* EOF`
* a simple-code file is a collection of dependencies, functions, variables and structures.

### dependency

`dep [NAME] [STRING] ( [STRING] )? ;`
* the first STRING is the path relative from a included source directory
* the second optional STRING is the path of the binary used at runtime
    * if not set it will be the extracted from the first STRING
        * if the first STRING ends with `.sf`, `.sexp` the end will be discarded
        * otherwise it will be the first STRING
* file type:
    * the DEPENDENCY is treated as simple-source-code-file when it ends with `.sf`
        * note that if the dependnecy is a source file it is not allowed to be under a lockup directory
        * a source dependency needs to be currently compiled
        * note that a source code dependency is not allowed to have a runtime path set
    * the DEPENDENCY is treated as simple-export-file when it ends with `.sexp`
    * if the DEPENDENCY ends with `.*` the compiler will use the simple-source-code-file with a `.sf` ending if it compiles currently the targeted source file
    * if the DEPENDENCY ends with `.*` the compiler will use the simple-export-file with a `.sexp` ending if it currently does not compile the targeted source file
    * otherwise the DEPENDENCY is treated as simple-export-file
* to use a import from a dependency use `dependency_name` `:` `import_name`

### function

`func [FUNC_MARKS_AND_NAME] \( [NAMED_TYPE_LIST] \) ( --&gt; &lt; [NAMED_TYPE_LIST] &gt; )? [BLOCK]`

a function is a block of commands used to do something for example convert the argument parameters to result parameters
* marks:
    * `exp`: the function will be exported to the symbol file
    * `main`: the file will be made executable and this function will be the main function.
        * the `main` function has to have specific structure:
            * `( num , char## ) --&gt; &lt; ubyte &gt;`
            * argument params:
                1. `num` argc: this will be set to the number of arguments passed to the program
                2. `char##` argv: this will point the arguments passed to the program
                    * the first argument will be the program itself
            * return param:
                1. `ubyte` exitnum: the return number of the main function will be the exit number of the process

### variable

`var (exp)? [NAMED_TYPE] ( &lt;-- [VALUE] )? ;`

a variable can be used by all functions if declared in a file    
if declared in a code block it can be used by the commands in the block after the declaration
if a non constant initial value is assigned and not declared in a code block the initilizing will be done at the start of the `init` function
if a constant initial value is assigned and not declared in a code block the initilizing will be done at compile time
* marks:
    * `exp`: the variable will be exported to the symbol file

### constant

`const (exp)? [NAME] &lt;-- [VALUE] ;`

a constant is always of the type: signed 64-bit number (`num`)    
a constant can be used like a value of type `num`
* marks:
    * `exp`: the constant will be exported to the symbol file

### structure

`struct [NAME] { [NAMED_TYPE_LIST] }`

### FUNC_MARKS_AND_NAME

`exp ( main | init )? [NAME] | ( main | init ) [NAME]?`

### NAME

`[a-zA-Z][a-zA-Z_0-9]*`

### NAMED_TYPE_LIST
`( [NAMED_TYPE] ( , [NAMED_TYPE] )* )?`

### NAMED_TYPE

`[TYPE] [NAME]`

### TYPE

* types:
    * `num` : a 64-bit number
    * `unum` : an unsigned 64-bit number
    * `fpnum` : a 64-bit floating point number
    * `fpdword` : a 32-bit floating point number
    * `dword` : a 32-bit number
    * `udword` : an unsigned 32-bit number
    * `word` : a 16-bit number
    * `uword` : an unsigned 16-bit number
    * `byte` : a 8-bit number
    * `ubyte` : an unsigned 8-bit number/character
    * `char` : same as `ubyte`
    * `struct [NAME]` : a memory structure
    * `[TYPE] #` : a pointer to a value of type
    * `[TYPE] \[ [VALUE]? \]` : an array of values of a type
    * `addr? [FUNC_TYPE]` : a function address
    * `cstruct [FUNC_TYPE]` : a function call structure

### FUNC_TYPE

`( &lt; ( [NAMED_TYPE_LIST] | ... ) &gt; &lt;-- )? ( ( [NAMED_TYPE_LIST] | ... ) \)`

### COMMAND

* BLOCK
    * `{ ( [COMMAND] )* }`
* VAR_DECL
    * `var [TYPE] [NAME] ( &lt;-- [VALUE] )? ;`
* ASSIGN
    * `[POSTFIX_EXP] &lt;-- [VALUE] ;`
* FUNC_CALL
    * `call [NAME] ( : [NAME] )? [POSTFIX_EXP] ;`
* WHILE
    * `while \( [VALUE] \) [COMMAND]`
* IF
    * `if \( [VALUE] \) [COMMAND] ( else [COMMAND] )?`
* ASSEMBLER
    * `asm ( [XNN] &lt;-- [VALUE] )* [PRIM_CODE_BLOCK] ( [VALUE] &lt;-- [XNN] )* ;`
    * XNN
        * `X[0-9A-E][0-9A-F] | XF[0-9]`
    * PRIM_CODE_BLOCK
        * `:: ( [^&gt;"'\|] | &gt; [^&gt;] | " ( [^"\\] | \\ . )* " | ' ( [^'\\] | \\ . )* ' | \| ( [^&gt;:] | &gt; [^\r\n]* [\r\n] | : ( [^|] | \| [^&gt;] )* \|&gt; ) )* &gt;&gt;`

### VALUE

* VALUE
    * `[COND_EXP]`
* COND_EXP
    * `[LOR_EXP] ( \? [VALUE] : [COND_EXP] )?`
* LOR_EXP
    * `[LAND_EXP] ( ( \|\| ) [LAND_EXP] )*`
* LAND_EXP
    * `[OR_EXP] ( ( && ) [OR_EXP] )*`
* OR_EXP
    * `[XOR_EXP] ( ( \| ) [XOR_EXP] )*`
* XOR_EXP
    * `[AND_EXP] ( ( ^ ) [AND_EXP] )*`
* AND_EXP
    * `[EQ_EXP] ( ( & ) [EQ_EXP] )*`
* EQ_EXP
    * `[REL_EXP] ( ( != | == ) [REL_EXP] )*`
* REL_EXP
    * `[REL_EXP] ( ( &gt; | &gt;= | &lt;= | &lt; ) [REL_EXP] )*`
* SHIFT_EXP
    * `[SHIFT_EXP] ( ( &lt;&lt; | &gt;&gt; | &gt;&gt;&gt; ) [SHIFT_EXP] )*`
* ADD_EXP
    * `[MUL_EXP] ( ( \+ | - ) [MUL_EXP] )*`
* MUL_EXP
    * `[CAST_EXP] ( ( \* | / | % ) [CAST_EXP] )*`
* CAST_EXP
    * `( \( [TYPE] \) )? [UNARY_EXP]`
* UNARY_EXP
    * `( \+ | - | & | ~ | ! )? [POSTFIX_EXP]`
* POSTFIX_EXP
    * `[DIRECT_EXP] ( # | \[ [VALUE] \] )*`
* DIRECT_EXP
    * `[STRING]+`
    * `[CHARACTER]`
    * `[NUM]`
    * `[FPNUM]`
    * `[NAME] ( : [NAME] )*`
    * `\( [VALUE] \)`
* STRING
    * `"([^"\\]|\\["rnt0\\])*"`
* CHARACTER
    * `'([^'\\]|\\['rnt0\\])'`
* NUM
    * `-?[0-9]+`
    * `HEX-[0-9A-F]+`
    * `UHEX-[0-9A-F]+`
    * `NHEX-[0-9A-F]+`
    * `BIN-[01]+`
    * `NBIN-[01]+`
    * `OCT-[0-7]+`
    * `NOCT-[0-7]+`
* FPNUM
    * `-?([0-9]*.[0-9]+|[0-9]+.[0-9]*)`

## comments

`// [^\n]* \n | /\* ( [^\*] | \* [^/] )* \*/`

comments are treated like whitespace, they are allowed everywhere exept in the middle of a token (but betwen any two tokens)

## std

some functions, variables, constants and structures are predefined

these are from the automatically imported dependency `std`

the `std` dependency must be a compile time only dependency, `std` is not allowed to also be a runtime dependency.

(to call for exampe a function, use `call std:streams_write write_args;`)

### functions

the compiler is allowed to inline all, some or none of the predefined functions

### variables

`exp num errno;`
* the compiler must ensure that the `errno` variable has always the same value as the primitive `ERRNO` register
    * the compiler should just redirect all operations on the given variable to the `ERRNO` register.
        * the compiler is encuraged to not reserve any additional memory for the `errno` variable, but is free to do so
    * this simplifies the error handling when mixin simple-code and primitive-code

### constants

### structures

there are currently no structures in the `std` dependency
