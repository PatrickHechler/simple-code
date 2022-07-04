# simple-code

simple-code is a simple programming language.

## file

`( dependency | variable | structure | function )* EOF`
* a simple-code file is a collection of dependencies, functions, variables and structures.

### dependency

`dep [STRING] ;`
* the STRING is the path relative from a included source directory
* file type:
    * the DEPENDENCY is either a symbol-file (`*.psf`)
    * or a simple-code source file (`*.s`)
    * or a primitive-code source file (`*.psc`)
* if the DEPENDENCY is a source file (primitive or simple) only the export-symbols will be extracted and used

### function

`func (exp)? (main)? [NAME] \( [NAMED_TYPE_LIST] \) ( --> < [NAMED_TYPE_LIST] > )? [BLOCK]`

a function is a block of commands used to do something for example convert the argument parameters to return parameters
* marks:
    * `exp`: the function will be exported to the symbol file
    * `main`: the file will be made executable and this function will be the main function.
        * the `main` function has to have specific argument and return parameters:
            * argument params:
                1. `num argc`: this will be set to the number of arguments passed to the program
                2. `char** argv`: this will point the arguments passed to the programm
                    * the first argument will be the programm itsef
            * return param:
                1. `num enum`: the return number of the main function will be the exit number of the process

### variable

`var (exp)? [TYPE] [NAME] ;` 
a variable can be used by all functions
* marks:
    * `exp`: the variable will be exported to the symbol file


### structure

`struct [NAME] { [NAMED_TYPE_LIST] }`

### NAME

`[a-zA-Z_]*`

### NAMED_TYPE_LIST
`( [PARAMETER] ( , [PARAMETER] )* )?`

### PARAMETER

`[TYPE] [NAME]`

### TYPE

* types:
    * `num` : a 64-bit number
    * `fpnum` : a 64-bit floating point number
    * `dword` : a 32-bit number
    * `word` : a 16-bit character/number
    * `char` : same as `word`
    * `byte` : a 8-bit number
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
    * `call [POSTFIX_EXP] ;`
* WHILE
    * `while \( [VALUE] \) [COMMAND]`
* IF
    * `if \( [VALUE] \) [COMMAND] ( else [COMMAND] )?`

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

## predefined functions

### exit
`func exp exit (num enum)`
exits the program

### fpToNum
`func exp fpToNum (num n) --> <fpnum fpn>`

### numToFp
`func exp numToFp (fpnum fpn) --> <fpnum n>`

## predefined  structures

### file
<code><pre>
struct file {
    num id,
    num lock,
    num reserved1,
    num reserved2
}
</pre></code>

### file stream
<code><pre>
struct file_stream {
    struct file* file,
    num pos
}
</pre></code>
