# simple-code

simple-code is a simple programming language.    
The std-lib is represented by the [std](#std) dependency:    
This dependency is small, but provides some basic functionality like allocating memory, writeing to stdout and some more.

## file

A simple-code file can either be compiled/interpreted.    
If there is a main-function (that is a function marked with `main`) it can be executed.

`( [dependency] | [variable] | [typedef] | [function] )* EOF`

### simple-export-file

`( [dependency] | [variable] | [typedef] | [dep-function] )* EOF`

* note that simple-export-files are parsed with some special rules:
    * only `const` variables are allowed to have an initilizer
        * having a `const` variable without initilizer is still an error
    * dependencies must not use `<ME>` instead of a `NAME`

### dependency

`dep ( [NAME] | <ME> ) [STRING] ( [STRING] )? ;`
* the first STRING is the path relative from a included source directory
* the second optional STRING is the path of the binary used at runtime
    * specifing the same export file with different runtime paths may be a compilation error
    * if not set it will be the calculated from the first STRING
        * if the first STRING ends with `.sexp` the end will be discarded
        * otherwise it will be the first STRING
* to use a exprted symbol from a dependency use `dependency_name` `:` `import_name`
* if instead of a dependency name `<ME>` is used:
    * no runtime path is allowed to be specified
    * the exported symbols can directly be used
    * it is be possible to define multiple (posssible different) `<ME>` dependencies, as long as these rules are satisfied
    * if at the end of the parsing process there are symbols in the simple-export-file which are not declared in the translation unit the compilation fails
    * if a symbol is declared and a incompatible symbol with the same name can be found in the simple-export-file the compilation fails
        * only the compatibility (and existence) of the symbols is checked, they are not forced to be exported by these rules
        * incompatible means (here): different, but the names and the markings, except for `nopad` are ignored
            * note that the value is also compared if the symbol is a variable marked with `const`
            * for example `const num MAX_BUF_LEN <-- 128;` is incompatible to `const num MAX_BUF_LEN <-- 256;`
            * for example `add <num result> <-- (num number_a, num number_b)` is compatible to `exp add <num res> <-- (num a, num b)`, but incompatible to `add <unum result> <-- (unum numberA, unum numberB)`
            * for example `typedef struct { num a; } num_wrapper;` is incompatible to `typedef struct nopad { num a; } num_wrapper;`

### function

`func [FUNC_MARKS_AND_NAME] [TYPE] [BLOCK]`

a function is a block of commands used to do something for example convert the argument parameters to result parameters
* marks:
    * `exp`: the function will be exported to the symbol file
    * `init`: this function will be the initialisation function called when the file is loaded
        * note that the `init` function will be executed even before the `main` function (if the `main` gets executed)
        * note that if a global variable has an initilizer the compiler may generate an implicit `init` function
        * the `init` function has to have specific structure:
            * `( ) --> < >`
    * `main`: the file will be made executable and this function will be the main function.
        * the `main` function has to have specific structure:
            * `( unum , char## ) --> < ubyte >`
            * argument values:
                1. `num` argc: this will be set to the number of arguments passed to the program
                2. `char##` argv: this will point the arguments passed to the program
                    * the first argument will be the program itself
                    * `argv[argc]` will be set to `(char#) 0`
            * return value:
                1. `ubyte` exitnum: the return number of the main function will be the exit number of the process
* note that only one function can be marked with `init`/`main`
* note that if a function is marked with `main` or `init` and has no name the function can not be called by any user generated program code
* the type **must** be a function address type (function structure types are **not** allowed)

### dep-function

`func (exp)? [NAME] [TYPE] ;`

* like `[function]`, but there are no `main`/`init` marks allowed and the `[BLOCK]` is replaced by a `;`
    * `[FUNC_MARKS_AND_NAME]` is replaced by `(exp)? [NAME]` replaces, because it allows anonymus functions when marked with `main`/`init`

### variable

`(const)? (exp)? [NAMED_TYPE] ( <-- [VALUE] )? ;`

a variable can be used by all functions if declared in a file    
if declared in a code block it can be used by the commands in the block after the declaration    
if a non constant initial value is assigned and not declared in a code block the initilizing will be done at the start of the `init` function in the order of declaration    
if a constant initial value is assigned and not declared in a code block the initilizing may (but does not need to) be done at compile time    
if marked with `const` there must be a constant initial value    
* marks:
    * `const`: the variable can not be modified after initialisation
    * `exp`: the variable will be exported to the symbol file
        * only variables declared in a file and **not** in a code block can be marked with `exp`

### typedef

`typedef (exp)? [TYPE] [NAME] ;`

### FUNC_MARKS_AND_NAME

`(exp)? (main | init)? [NAME] | (main | init) [NAME]?`

### NAME

`[a-zA-Z][a-zA-Z_0-9]*`

### NAMED_TYPE_LIST
`( [NAMED_TYPE] ( , [NAMED_TYPE] )* )?`

### NAMED_TYPE_LIST_SEMI_SEP
`( [NAMED_TYPE] ; )*`

### NAMED_TYPE

`[TYPE] [NAME]`

### TYPE

* types:
    * `num` : a signed 64-bit number
    * `unum` : an unsigned 64-bit number
    * `fpnum` : a 64-bit floating point number
    * `fpdword` : a 32-bit floating point number
    * `dword` : a signed 32-bit number
    * `udword` : an unsigned 32-bit number
    * `word` : a signed 16-bit number
    * `uword` : an unsigned 16-bit number
    * `byte` : a signed 8-bit number
    * `ubyte` : an unsigned 8-bit number/character
    * `char` : implicit `typedef ubyte char;`
        * note that it is unspecified if `[DEPENDENCY] : char` is a valid type
    * `[NAME] ( : [NAME] )*` : a type defined with `typedef`
    * `struct (nopad)? { [NAMED_TYPE_LIST_SEMI_SEP] }` : a memory structure
        * marks:
            * `nopad`: do not use padding
                * when using this some entries may not be aligned
    * `[FUNC_TYPE]` : a function address
    * `fstuct ( [NAME] ( : [NAME] )* | [FUNC_TYPE0] )` : a function call structure
    * `[TYPE] #` : a pointer to a value of TYPE
    * `[TYPE] \[ [VALUE]? \]` : an array of values of TYPE

### FUNC_TYPE

`func [NAME] ( : [NAME] )* | [FUNC_TYPE0]`

### FUNC_TYPE0

`(nopad)? [FUNC_TYPE_RESULTS] \( ( [NAMED_TYPE_LIST] ) \)`

* marks:
    * `nopad`: do not use padding

#### FUNC_TYPE_RESULTS

`( < [NAMED_TYPE_LIST] > <-- )?`

### COMMAND

* BLOCK
    * `{ ( [COMMAND] )* }`
* VAR_DECL
	* `(const)? [NAMED_TYPE] ( <-- [VALUE] )? ;`
* ASSIGN
    * `[SHIFT_EXP] <-- [VALUE] ;`
* FUNC_CALL
    * `[SHIFT_EXP] [FUNC_CALL_RESULT] [FUNC_CALL_ARGS] ;`
        * FUNC_CALL_RESULT
            * `( ( < ( [IGNOREABLE_SHIFT_EXP] ( , [IGNOREABLE_SHIFT_EXP] )* )? > ) <-- )?`
            * IGNOREABLE_SHIFT_EXP
                * `[SHIFT_EXP] | \?`
        * FUNC_CALL_ARGS
            * `\( ([VALUE] ( , [VALUE] )* )? \)`
* FUNC_CALL_WITH_FUNCTION_STRUCTURE
    * `call [SHIFT_EXP] [VALUE] ;`
        * the first value is the function address
        * the second value is the function structure
* WHILE
    * `while \( [VALUE] \) [COMMAND]`
* IF
    * `if \( [VALUE] \) [COMMAND] ( else [COMMAND] )?`
* ASSEMBLER
    * `asm [ASM_PARAMS] [ASM_BLOCK] [ASM_RESULTS] ;`
        * ASM_PARAMS
            * `( [STRING] <-- [VALUE] ( , [STRING] <-- [VALUE] )* )?`
        * ASM_BLOCK
            * `::: ( [^>] | > [^>] | >> [^>] )* >>>`
        * ASM_RESULTS
            * `( [SINGLE_ASM_RESULT] ( , [SINGLE_ASM_RESULT] )* )?`
            * SINGLE_ASM_RESULT
                * `[VALUE] <-- [STRING] | [STRING] <-- \?`
        * each STRING represents a native register or address supported by the assembler
        * the `[STRING] <-- [VALUE]` pairs represent the arguments to be passed to the asm code
        * the `[POSTFIX_EXP] <-- [STRING]` pairs represent the results of the asm code
        * the `[STRING] <-- \?` pairs represent the registers modified by the asm code
            * note that registers used as argument are **not** implicitly marked as modified
            * registers used as result are implicitly marked as modified

### VALUE

* VALUE
    * `[COND_EXP]`
* COND_EXP
    * `[LOR_EXP] ( \? [VALUE] ! [COND_EXP] )?`
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
    * `[SHIFT_EXP] ( ( << | >> ) [SHIFT_EXP] )*`
* ADD_EXP
    * `[MUL_EXP] ( ( \+ | - ) [MUL_EXP] )*`
* MUL_EXP
    * `[CAST_EXP] ( ( \* | / | % ) [CAST_EXP] )*`
* CAST_EXP
    * `( \( [TYPE] \) )? [UNARY_EXP]`
* UNARY_EXP
    * `( \+ | - | & | ~ | ! )? [POSTFIX_EXP]`
* POSTFIX_EXP
    * `[DIRECT_EXP] ( # | \[ [VALUE] \] | : [NAME] )*`
* DIRECT_EXP
    * `[STRING]+`
    * `[CHARACTER]`
    * `[NUMBER]`
    * `[NAME]`
    * `\( [VALUE] \)`
* STRING
    * `"([^"\\\r\n\0]|\\(["rnt0\\]|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8}))*"`
* CHARACTER
    * `'([^'\\\r\n\0]|\\['rnt0\\])'`
* NUMBER
    * `[SCALAR_NUM]([Ss]|[Uu])?([QqNn]|[Dd]|[Ww]|[HhBb])?`
        * the sign (first) postfix:
            * `U|u` is only allowed for positive numbers (zero is positive)
            * if emited:
                * positive numbers are unsigned
                * negative numbers are signed
        * if present, the size (second) postfix must not truncate the number (`DEC-256B` is disallowed)
            * `Q|q|N|n`: `num`/`unum`
            * `D|d`: `dword`/`udword`
            * `W|w`: `word`/`uword`
            * `H|h|B|b`: `byte`/`ubyte`
        * if present, the sign (first) postfix must not truncate the number (`DEC-128sb` is disallowed)
    * `[FPNUM]([QqNn]|[Dd])?`
        * the size postfix:
            * `Q|q|N|n`: `fpnum`
            * `D|d`: `fpdword`
    * SCALAR_NUM:
        * `[0-9]+`
            * same as `DEC-[0-9]+`
        * `-[0-9]+`
            * same as `NDEC-[0-9]+`
        * `DEC-[0-9]+`
        * `NDEC-[0-9]+`
        * `HEX-[0-9A-F]+`
        * `UHEX-[0-9A-F]+`
        * `NHEX-[0-9A-F]+`
        * `BIN-[01]+`
        * `NBIN-[01]+`
        * `OCT-[0-7]+`
        * `NOCT-[0-7]+`
    * FPNUM:
        * `-?([0-9]*.[0-9]+|[0-9]+.[0-9]*)`

## comments

`// [^\n]* \n | /\* ( [^\*] | \* [^/] )* \*/`

comments are treated like whitespace, they are allowed everywhere exept in the middle of a token (but betwen any two tokens)

## std
* some symbols are already defined by the compiler
* these are from the automatically imported dependency `std`
* the `std` dependency may be a compile time only dependency.
    * the `std:sys´ dependency **must** be a compile time only dependency.
* if `std` has dependencies, which are not used the binary must not rely on these dependencies
    * the only exception is the `std:sys` dependency
* the `std` dependency **must not** define non constant variables
    * the dependencies of `std` **must not** define non constant variables
        * dependencies of the `std` dependency are dependencies of `std` or a dependency of a dependency of `std`

### std:sys
this dependency may define additional symbols not mentioned here

`func pagesize <unum result> <-- ();`
* returns the size of a memory page (whatever this may be, it will always be a non zero power of two)

`func pageshift <unum result> <-- ();`
* returns the shift of a memory page (whatever this may be)
* the page shift is defined as the bit count of `pagesize:result - 1` (or `pagesize:result = 1 << pageshift:result`)

### functions

#### system functions
`func exit <> <-- (ubyte exitnum)`
* terminates the process with the given exit number
* this function will never return to the caller

#### memory functions
`func mem_alloc <ubyte# addr> <-- (unum length, unum align)`
* allocates `length` bytes, which are aligned to `align` bytes and stores the address of the allocated block in `addr`
* `align` must be a non zero power of two

`func mem_realloc <ubyte# new_addr> <-- (ubyte# old_addr, unum new_length, unum new_align)`
* reallocates the memory block to now store `new_length` bytes aligned to `new_align`
* the new address of the memory block is stored in `new_addr`
* `new_align` must be a non zero power of two

`func mem_free <> <-- (ubyte# addr)`
* frees the given memory block

`func mem_copy <> <-- (ubyte# from, ubyte# to, unum length)`
* copies `length` bytes from `from` to `to`
* if the memory regiones `from..(from+length)` and `to..(to+length)` overlap the new content of `from..(from+length)` is undefined
* otherwise the new content of `from..(from+length)` is the old content of `to..(to+length)`

`func mem_move <> <-- (ubyte# from, ubyte# to, unum length)`
* copies `length` bytes from `from` to `to`
* the new content of `from..(from+length)` is the old content of `to..(to+length)`
    * even if the memory regiones `from..(from+length)` and `to..(to+length)` overlap 

#### interact with the std streams

`func writestr <unum wrote, unum errno> <-- (char# string)`
* prints the given (`\0` terminated) string to stdout
* `wrote` will be the number of bytes written to stdout
* `errno` will be the error number or `0` on success

`func readln <unum read_len, unum errno> <-- (char# buffer, unum max_length)`
* reads the next line from stdin
* at most `max_length-1` bytes will be read
* a `\0` character will be inserted after the line terminator `\n`
    * or at the end of the buffer, if no `\n` was read
* note that if a `\0` was read from stdin, it will not be modified and treated as any other character
    * this can lead to an truncated string, if `read_len` is not checked
* the line terminator `\n` will also be copied to the buffer
    * thus the user can check if the buffer was large enugh or if `readln` should be called again
        * note that the part of the line wich was already read will not be read again
* `read_len` will be the number of bytes read from stdin
    * note that the `\0` is not read from stdin
* `errno` will be the error number or `0` on success

### constants

`const struct {}# NULL <-- (struct {}#) 0`

### types

* `typedef [TYPE] file_handle`
    * the exact type is implementation specific
    * the size and alingment will be at most the size and alignment of `unum`
* `typedef [TYPE] fs_element_handle`
    * the exact type is implementation specific
    * the size and alingment will be at most the size and alignment of `unum`
