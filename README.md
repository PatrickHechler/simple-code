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
        * if the first STRING ends with `.ssc`, `.ssf` or `.psf` the end will be discarded
        * otherwise it will be the first STRING
* file type:
    * the DEPENDENCY is treated as simple-source-code-file when it ends with `.s`
        * note that if the dependnecy is a source file it is not allowed to be under a lockup directory
        * a source dependency needs to be currently compiled
        * note that a source code dependency is not allowed to have a runtime path set
    * the DEPENDENCY is treated as primitive-symbol-file if it ends with `.psf`
    * the DEPENDENCY is treated as simple-export-file when it ends with `.sf`
    * otherwise the DEPENDENCY is treated as simple-export-file
* to use a import from a dependency use `dependency_name` `:` `import_name`

### function

`func (exp)? (main)? [NAME] \( [NAMED_TYPE_LIST] \) ( --> < [NAMED_TYPE_LIST] > )? [BLOCK]`

a function is a block of commands used to do something for example convert the argument parameters to result parameters
* marks:
    * `exp`: the function will be exported to the symbol file
    * `main`: the file will be made executable and this function will be the main function.
        * the `main` function has to have specific argument and return parameters:
            * argument params:
                1. `num argc`: this will be set to the number of arguments passed to the program
                2. `char## argv`: this will point the arguments passed to the program
                    * the first argument will be the program itself
            * return param:
                1. `num exitnum`: the return number of the main function will be the exit number of the process

### variable

`var (exp)? [TYPE] [NAME] ;` 

a variable can be used by all functions
* marks:
    * `exp`: the variable will be exported to the symbol file

### constant

`const (exp)? [NAME] <-- [VALUE] ;`

a constant is always of the type: signed 64-bit number (`num`)

a constant can be used like a value of type `num`

* marks:
    * `exp`: the constant will be exported to the symbol file

### structure

`struct [NAME] { [NAMED_TYPE_LIST] }`

### NAME

`[a-zA-Z][a-zA-Z_0-9]*`

### NAMED_TYPE_LIST
`( [PARAMETER] ( , [PARAMETER] )* )?`

### PARAMETER

`[TYPE] [NAME]`

### TYPE

* types:
    * `num` : a 64-bit number
    * `unum` : an unsigned 64-bit number
    * `fpnum` : a 64-bit floating point number
    * `dword` : a 32-bit number
    * `udword` : an unsigned 32-bit number
    * `word` : a 16-bit number
    * `uword` : an unsigned 16-bit number
    * `byte` : a 8-bit number
    * `ubyte` : an unsigned 8-bit number/character
    * `char` : same as `ubyte`
    * `struct [NAME]` : a structure of types
    * `[TYPE] #` : a pointer to a type
    * `[TYPE] \[ [VALUE]? \]` : a array of a type
    * `\( ( [NAMED_TYPE_LIST] | ... ) \) ( --> < ( [NAMED_TYPE_LIST] | ... ) > )?` : a function call structure

### COMMAND

* BLOCK
    * `{ ( [COMMAND] )* }`
* VAR_DECL
    * `var [TYPE] [NAME] ( <-- [VALUE] )? ;`
* ASSIGN
    * `[POSTFIX_EXP] <-- [VALUE] ;`
* FUNC_CALL
    * `call [NAME] ( : [NAME] )? [POSTFIX_EXP] ;`
* WHILE
    * `while \( [VALUE] \) [COMMAND]`
* IF
    * `if \( [VALUE] \) [COMMAND] ( else [COMMAND] )?`
* ASSEMBLER
    * `asm ( [XNN] <-- [VALUE] )* [PRIM_CODE_BLOCK] ( [VALUE] <-- [XNN] )* ;`
    * XNN
        * `X[0-9A-E][0-9A-F] | XF[0-9]`
    * PRIM_CODE_BLOCK
        * `:: ( [^>"'\|] | > [^>] | " ( [^"\\] | \\ . )* " | ' ( [^'\\] | \\ . )* ' | \| ( [^>:] | > [^\r\n]* [\r\n] | : ( [^|] | \| [^>] )* \|> ) )* >>`

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
    * `[REL_EXP] ( ( > | >= | <= | < ) [REL_EXP] )*`
* SHIFT_EXP
    * `[SHIFT_EXP] ( ( << | >> | >>> ) [SHIFT_EXP] )*`
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
