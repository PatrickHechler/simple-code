# simple-code

simple-code is a simple programming language.

## file

* a simple-code file is a collection of dependencies, functions, variables and structures.

    (    
        dependency    
        |    
        function    
        |    
        variable    
        |    
        structure    
    )*    
    EOF    

### dependency

`dep '[DEPENDENCY]' ;`
* the DEPENDENCY is the path relative from a included source directory
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
    * `char` : a 16-bit character
    * `byte` : a 8-bit number
    * `struct [NAME]` : a structure of types
    * `[TYPE] \*` : a pointer to a type
    * `[TYPE] \[ \]` : a array of a type
    * `\( ( [NAMED_TYPE_LIST] | ... ) \) ( --> < ( [NAMED_TYPE_LIST] | ... ) > )?` : a function call structure

### COMMAND

* BLOCK
    * `{ ( [COMMAND] ; )* }`
* VAR_DECL
    * `var [TYPE] [NAME] ( <-- [VALUE] )? ;`
* ASSIGN
    * `[VALUE] <-- [VALUE] ;`
* FUNC_CALL
    * `call [NAME] ;`

### VALUE

* add-expression
    * `add-expression ( ( \+ | - ) add-expression )*`
    * a add-expression aritmetically adds or subtracts
* mul-expression
    * `or-expression ( ( * | / | % ) or-expression )*`
    * a mul-expression aritmetically multiplies or divides and uses reminder or result as new value
* or-expression
    * `and-expression ( \| and-expression )*`
* and-expression
    * `un-expression ( & un-expression )*`
* un-expression
    * `( - | ~ )? cast-expression`
    * a un-expression aritmetically negates or binary inverts
* cast-expression
    * `( \( [TYPE] \) )? ref-expression`
    * a cast-expression never changes the value
* ref-expression
    * `direct-expression ( \* | \[ [VALUE] \] )?`
    * a ref-expression extractes a element from a array or dereferences the pointer
* direct-expression
    * `constant-number`
    * `constant-floating-point-number`
    * `[NAME] ( : [NAME] )?`
    * `\( [VALUE] \)`
* constant-number
    * `-?[0-9]+`
    * `HEX-[0-9A-F]+`
    * `UHEX-[0-9A-F]+`
    * `NHEX-[0-9A-F]+`
    * `BIN-[01]+`
    * `NBIN-[01]+`
    * `OCT-[0-7]+`
    * `NOCT-[0-7]+`
* constant-floating-point-number
    * `-?[0-9]*.[0-9]*`

## default functions

### exit
`func exp exit (num enum)`
exits the program

### fpToNum
`func exp fpToNum (num n) --> <fpnum fpn>`

### numToFp
`func exp numToFp (fpnum fpn) --> <fpnum n>`
