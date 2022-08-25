# simple-code

simple-code is a simple programming language.

## file

`( dependency | variable | structure | function | constant )* EOF`
* a simple-code file is a collection of dependencies, functions, variables and structures.

### dependency

`dep [NAME] [STRING] ( [STRING] )? ;`
* the first STRING is the path relative from a included source directory
* the second optional STRING is the path of the binary used at runtime
    * if not set it will be the same extracted from the first STRING
        * if the first STRING ends with `.sf` or `.s` the end will be discarded
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

a function is a block of commands used to do something for example convert the argument parameters to return parameters
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
    * `word` : a 16-bit character/number
    * `uword` : an unsigned 16-bit character/number
    * `char` : same as `uword`
    * `byte` : a 8-bit number
    * `ubyte` : an unsigned 8-bit number
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

## predefined

some functions, constants and structures are predefined

### functions

`func exp exit(num exitnum)`

`func exp mem_alloc(num size) --$gt; $lt;byte# data$gt;`

`func exp mem_realloc(byte# data, num new_size) --$gt; $lt;num# new_data$gt;`

`func exp mem_free(byte# data)`

`func exp streams_new(char# file, num mode) --$gt; $lt;struct file_stream# stream$gt;`

`func exp stream_write(num stream, num size, byte# data) --$gt; $lt;num wrote, unum errno$gt;`

`func exp stream_read(num stream, num size, byte# data) --$gt; $lt;num read, unum errno$gt;`

`func exp fs_get_file(char# file_path) --$gt; (struct fs_element# file, unum errno)`

`func exp fs_get_folder(char# folder_path) --$gt; (struct fs_element# folder, unum errno)`

`func exp fs_get_file(char# link_path) --$gt; (struct fs_element# link, unum errno)`

`func exp fs_get_file(char# path) --$gt; (struct fs_element# element, unum errno)`

`func exp fs_duplicate_handle(struct fs_element# orig) --$gt; $lt;struct fs_element# duplicate$gt;`

`func exp fs_get_parent(struct fs_element# element) --$gt; $lt;unum errno$gt;`

`func exp fs_from_id(num id) --$gt; $lt;struct fs_element# element, unum errno$gt;`

`func exp fs_get_create(struct fs_element# element) --$gt; $lt;unum errno$gt;`

`func exp fs_get_last_mod(struct fs_element# element) --$gt; $lt;unum errno$gt;`

`func exp fs_get_last_meta_mod(struct fs_element# element) --$gt; $lt;unum errno$gt;`

`func exp fs_set_create(struct fs_element# element, num time) --$gt; $lt;unum errno$gt;`

`func exp fs_set_last_mod(struct fs_element# element, num time) --$gt; $lt;unum errno$gt;`

`func exp fs_set_last_meta_mod(struct fs_element# element, num time) --$gt; $lt;unum errno$gt;`

`func exp fs_get_lock_data(struct fs_element# element) --$gt; $lt;unum lock_data, unum errno$gt;`

`func exp fs_get_lock_time(struct fs_element# elemnet) --$gt; $lt;num time, unum errno$gt;`

`func exp fs_lock_element(struct fs_element# element, unum lock_data) --$gt; $lt;unum errno$gt;`

`func exp fs_unlock_element(struct fs_element# element) --$gt; $lt;unum errno$gt;`

`func exp fs_delete_element(struct fs_element# element, unum parent_lock) --$gt; $lt;unum errno$gt;`

`func exp fs_move_element(struct fs_element# element, struct fs_element# new_parent, char# new_name, unum old_parent_lock) --$gt; $lt;unum errno$gt;`

`func exp fs_get_flags(struct fs_element# element) --$gt; $lt;num flags, unum errno$gt;`

`func exp fs_mod_flags(struct fs_element# element, udword add_flags, udword rem_falgs) --$gt; $lt;unum errno$gt;`

`func exp fs_folder_child_count(struct fs_element# folder) --$gt; $lt;num child_count, unum errno$gt;`

`func exp fs_child_from_index(struct fs_element# folder, dword index) --$gt; $lt;unum errno$gt;`

`func exp fs_child_from_name(struct fs_elemnet# folder, char# name) --$gt; $lt;unum errno$gt;`

`func exp fs_create_folder(struct fs_elemnet# parent, char# name) --$gt; $lt;unum errno$gt;`

`func exp fs_create_folder(struct fs_elemnet# parent, char# name) --$gt; $lt;unum errno$gt;`

`func exp fs_create_folder(struct fs_elemnet# parent, char# name, struct fs_element# target) --$gt; $lt;unum errno$gt;`

`func exp fs_file_length(struct fs_element# file) --$gt; $lt;num length, unum errno$gt;`

`func exp fs_file_hash(struct fs_element# file, byte# data) --$gt; $lt;unum errno$gt;`

`func exp fs_file_read(struct fs_elemnet# file, num position, num length, byte# buffer) --$gt; $lt;unum errno$gt;`

`func exp fs_file_write(struct fs_elemnet# file, num position, num length, byte# buffer) --$gt; $lt;unum errno$gt;`

`func exp fs_file_append(struct fs_elemnet# file, num length, byte# buffer) --$gt; $lt;unum errno$gt;`

`func exp fs_file_truncate(struct fs_element# file, num new_length) --$gt; $lt;unum errno$gt;`

`func exp fs_link_get_target(struct fs_element# link) --$gt; $lt;unum errno$gt;`

`func exp fs_link_set_target(struct fs_element# link, struct fs_element# new_target) --$gt; $lt;unum errno$gt;`

`func exp fs_get_lock() --$gt; $lt;unum used_lock$gt;`

`func exp fs_set_lock(unum use_lock)`

`func exp fs_lock(unum lock_data) --$gt; $lt;unum errno$gt;`

`func exp fs_unlock() --$gt; $lt;unum errno$gt;`

`func exp time_get() --$gt; $lt;num time$gt;`

`func exp time_wait(num nanoseconds, num seconds) --$gt; $lt;num remain_nanoseconds, num remain_seconds, num success$gt;`

`func exp random() --$gt; $lt;num value$gt;`

`func exp mem_copy(byte# target, byte# source, num length)`

`func exp mem_move(byte# target, byte# source, num length)`

`func exp mem_bset(byte# memory, ubyte value, num byte_count)`

`func exp mem_set(num# memory, num value, num num_count)`

`func exp string_length(char# string) --$gt; $lt;num length$gt;`

`func exp string_compare(char# a, char# b) --$gt; $lt;num result$gt;`

`func exp string_from_number(num number, char# buffer, num base, num buffer_size) --$gt; $lt;num size, char# new_buffer, num new_buffer_size, unum errno$gt;`

`func exp string_from_fpnumber(fpnum fpnumber, char# buffer, num buffer_size) --$gt; $lt;num size, char# new_buffer, num new_buffer_size, unum errno$gt;`

`func exp string_to_number(char# string_number, num base) --$gt; $lt;num number, num success$gt;`

`func exp string_to_fpnumber(char# string_number) --$gt; $lt;fpnum fpnumber, num success$gt;`

`func exp string_formatt(char# source, char# target, num target_size, num argument_count, num# arguments) --$gt; $lt;num string_length, char# new_target, num new_target_size, unum errno$gt;`

`func exp string_to_u8(char# string, ubyte# u8buffer, num buffer_size) --$gt; $lt;ubyte# u8string, num new_buffer_size, num u8string_length$gt;`

`func exp string_from_u8(ubyte# u8string, char# buffer, num buffer_size) --$gt; $lt;char# string, num new_buffer_size, num string_length$gt;`

`func exp fs_load_file(char# file) --$gt; $lt;byte# data, num size, unum errno$gt;`

`func exp fs_get_file(char# file) --$gt; $lt;byte# data, num size, num loaded_file, unum errno$gt;`

### constants

`const exp INT_ERRORS_ILLEGAL_INTERRUPT`

`const exp INT_ERRORS_UNKNOWN_COMMAND`

`const exp INT_ERRORS_ILLEGAL_MEMORY`

`const exp INT_ERRORS_ARITHMETIC_ERROR`

`const exp INT_EXIT`

`const exp INT_MEMORY_ALLOC`

`const exp INT_MEMORY_REALLOC`

`const exp INT_MEMORY_FREE`

`const exp INT_STREAMS_NEW_IN`

`const exp INT_STREAMS_NEW_OUT`

`const exp INT_STREAMS_NEW_APPEND`

`const exp INT_STREAMS_NEW_IN_OUT`

`const exp INT_STREAMS_NEW_IN_APPEND`

`const exp INT_STREAMS_WRITE`

`const exp INT_STREAMS_READ`

`const exp INT_STREAMS_CLOSE_STREAM`

`const exp INT_FS_GET_FILE`

`const exp INT_FS_GET_FOLDER`

`const exp INT_FS_GET_LINK`

`const exp INT_FS_IS_FILE`

`const exp INT_FS_IS_FOLDER`

`const exp INT_FS_IS_LINK`

`const exp INT_FS_ELEMENT_GET_PARENT`

`const exp INT_FS_ELEMENT_GET_PARENT_ID`

`const exp INT_FS_ELEMENT_FROM_ID`

`const exp INT_FS_ELEMENT_GET_CREATE`

`const exp INT_FS_ELEMENT_GET_LAST_MOD`

`const exp INT_FS_ELEMENT_GET_LAST_META_MOD`

`const exp INT_FS_ELEMENT_SET_CREATE`

`const exp INT_FS_ELEMENT_SET_LAST_MOD`

`const exp INT_FS_ELEMENT_SET_LAST_META_MOD`

`const exp INT_FS_ELEMENT_GET_LOCK_DATA`

`const exp INT_FS_ELEMENT_GET_LOCK_TIME`

`const exp INT_FS_ELEMENT_LOCK`

`const exp INT_FS_ELEMENT_UNLOCK`

`const exp INT_FS_ELEMENT_DELETE`

`const exp INT_FS_ELEMENT_MOVE`

`const exp INT_FS_ELEMENT_GET_FLAGS`

`const exp INT_FS_ELEMENT_MOD_FLAGS`

`const exp INT_FS_FOLDER_CHILD_COUNT`

`const exp INT_FS_FOLDER_GET_CHILD_OF_INDEX`

`const exp INT_FS_FOLDER_GET_CHILD_OF_NAME`

`const exp INT_FS_FOLDER_ADD_FOLDER`

`const exp INT_FS_FOLDER_ADD_FILE`

`const exp INT_FS_FOLDER_ADD_LINK`

`const exp INT_FS_FILE_LENGTH`

`const exp INT_FS_FILE_HASH`

`const exp INT_FS_FILE_READ`

`const exp INT_FS_FILE_WRITE`

`const exp INT_FS_FILE_APPEND`

`const exp INT_FS_FILE_REM_CONTENT`

`const exp INT_FS_FILE_TRUNCATE`

`const exp INT_FS_LINK_GET_TARGET`

`const exp INT_FS_LINK_SET_TARGET`

`const exp INT_FS_FILE_CREATE`

`const exp INT_FS_FOLDER_CREATE`

`const exp INT_FS_LINK_CREATE`

`const exp INT_FS_LOCK`

`const exp INT_FS_UNLOCK`

`const exp INT_FS_BLOCK`

`const exp INT_FS_UNBLOCK`

`const exp INT_TIME_GET`

`const exp INT_TIME_WAIT`

`const exp INT_RANDOM`

`const exp INT_MEMORY_COPY`

`const exp INT_MEMORY_MOVE`

`const exp INT_MEMORY_BSET`

`const exp INT_MEMORY_SET`

`const exp INT_STRING_LENGTH`

`const exp INT_STRING_COMPARE`

`const exp INT_NUMBER_TO_STRING`

`const exp INT_FPNUMBER_TO_STRING`

`const exp INT_STRING_TO_NUMBER`

`const exp INT_STRING_TO_FPNUMBER`

`const exp INT_STRING_FORMAT`

`const exp INT_LOAD_FILE`

`const exp INTERRUPT_COUNT`

`const exp MAX_VALUE`

`const exp MIN_VALUE`

`const exp STD_IN`

`const exp STD_OUT`

`const exp STD_LOG`

`const exp FS_STREAM_OFFSET_FILE`

`const exp FS_STREAM_OFFSET_POS`

`const exp FS_ELEMENT_OFFSET_ID`

`const exp FS_ELEMENT_OFFSET_LOCK`

`const exp LOCK_NO_READ_ALLOWED`

`const exp LOCK_NO_WRITE_ALLOWED_LOCK`

`const exp LOCK_NO_DELETE_ALLOWED_LOCK`

`const exp LOCK_NO_META_CHANGE_ALLOWED_LOCK`

`const exp LOCK_SHARED_LOCK`

`const exp LOCK_LOCKED_LOCK`

`const exp LOCK_NO_LOCK`

`const exp FLAG_FOLDER`

`const exp FLAG_FILE`

`const exp FLAG_LINK`

`const exp FLAG_READ_ONLY`

`const exp FLAG_EXECUTABLE`

`const exp FLAG_HIDDEN`

`const exp FLAG_FOLDER_SORTED`

`const exp FLAG_FILE_ENCRYPTED`

`const exp FP_NAN`

`const exp FP_MAX_VALUE`

`const exp FP_MIN_VALUE`

`const exp FP_POS_INFINITY`

`const exp FP_NEG_INFINITY`

`const exp STATUS_LOWER`

`const exp STATUS_GREATHER`

`const exp STATUS_EQUAL`

`const exp STATUS_CARRY`

`const exp STATUS_ZERO`

`const exp STATUS_NAN`

`const exp STATUS_ALL_BITS`

`const exp STATUS_SOME_BITS`

`const exp STATUS_NONE_BITS`

`const exp STATUS_ELEMENT_WRONG_TYPE`

`const exp STATUS_ELEMENT_NOT_EXIST`

`const exp STATUS_ELEMENT_ALREADY_EXIST`

`const exp STATUS_OUT_OF_SPACE`

`const exp STATUS_READ_ONLY`

`const exp STATUS_ELEMENT_LOCKED`

`const exp STATUS_IO_ERR`

`const exp STATUS_ILLEGAL_ARG`

`const exp STATUS_OUT_OF_MEMORY`

`const exp STATUS_ERROR`

`const exp REGISTER_MEMORY_START`

`const exp REGISTER_MEMORY_LAST_ADDRESS`

`const exp STREAM_MODE_READ`

`const exp STREAM_MODE_WRITE`

`const exp STREAM_MODE_APPEND`

`const exp STREAM_MODE_CREATE`

`const exp STREAM_MODE_NEW_FILE`

`const exp STREAM_MODE_TRUNCATE`

`const exp ERRNO_ELEMENT_WRONG_TYPE`

`const exp ERRNO_ELEMENT_NOT_EXIST`

`const exp ERRNO_ELEMENT_ALREADY_EXIST`

`const exp ERRNO_OUT_OF_SPACE`

`const exp ERRNO_READ_ONLY`

`const exp ERRNO_ELEMENT_LOCKED`

`const exp ERRNO_IO_ERR`

`const exp ERRNO_ILLEGAL_ARG`

`const exp ERRNO_OUT_OF_MEMORY`

`const exp ERRNO_ERROR`

`const exp STD_IN`

`const exp STD_OUT`

`const exp STD_LOG`

`const exp FS_FLAG_FOLDER`

`const exp FS_FLAG_FILE`

`const exp FS_FLAG_LINK`

`const exp FS_FLAG_READ_ONLY`

`const exp FS_FLAG_EXECUTABLE`

`const exp FS_FLAG_HIDDEN`

`const exp FS_FLAG_FOLDER_SORTED`

`const exp FS_FLAG_FILE_ENCRYPTED`

### structures

<code><pre>
struct fs_element {
    num id,
    num lock
}
</pre></code>

<code><pre>
struct file_stream {
    struct file* file,
    num pos
}
</pre></code>

# TODOs
* make compiler support for the predefined stuff
