/*
 * predefined_exports.s
 *
 * this file contains the predefined 
 * constants and an implementation of
 * all predefined functions declared and
 * implemented by the simple-compiler.
 * 
 * note that this file does not contain
 * all predefined constants.
 * 
 * many of the predefined functions are
 * just wrapping functions for interrups.
 * 
 * some functions may not corectly
 * work with a diffrent compiler
 */

/*
 * func exp exit(num exitnum)
 *
 * exits the current program with the
 * given exit number. Only the first byte
 * of the exit number is ensured to be
 * used, the rest of the number should be
 * zero, otherwise they will possible be
 * discarded.
 */
func exp exit(num exitnum) {
	asm exitnum --> X00 ::
		INT INT_EXIT
	>> ;
}

/* 
 * allocates a new block of memory
 * if the block could not be allocated
 * ((num#) -1) will be returned
 */
func exp mem_alloc(num size) --> <byte# data> {
	asm size --> X00 ::
		INT INT_MEMORY_ALLOC
	>> X00 --> data ;
}

/* 
 * reallocates an allocated block of memory
 * if the block could not be resized
 * ((num#) -1) will be returned and the old
 * pointer will remain valid
 */
func exp mem_realloc(byte# data, num new_size) --> <num# new_data> {
	asm data --> X00 new_size --> X01 ::
		INT INT_MEMORY_REALLOC
	>> X01 --> new_data ;
}

/* 
 * frees an allocated block of memory
 * if the block was already freed or
 * never allocated it will be treated like
 * an invalid memory access. Otherwise the
 * block will be freed and all further access
 * to the memory block will be illegal memory
 * access.
 */
func exp mem_free(byte# data) {
	asm data --> X00 ::
		INT INT_MEMORY_FREE
	>> ;
}

/*
 * opens a file-stream with read access
 */
const exp STREAM_MODE_READ <--      UHEX-0000000000000001 ;
/*
 * opens a file-stream with write access
 */
const exp STREAM_MODE_WRITE <--     UHEX-0000000000000002 ;
/*
 * opens a file-stream with append access
 * this flag implicitly sets the STREAM_MODE_WRITE flag
 */
const exp STREAM_MODE_APPEND <--    UHEX-0000000000000004 ;
/*
 * opens a file-stream and creates the file if
 * it does not already exist
 * only possible when STREAM_MODE_WRITE is set
 * not allowed when STREAM_MODE_NEW_FILE is set
 */
const exp STREAM_MODE_CREATE <--    UHEX-0000000000000008 ;
/*
 * 
 * opens a file-stream and creates the
 * file if it does not already exist
 * and fail if the file already exist
 * not allowed when STREAM_MODE_CREATE is set
 */
const exp STREAM_MODE_NEW_FILE <-- UHEX-0000000000000010 ;
/*
 * truncates all existing content from
 * the file.
 * only possible when STREAM_MODE_WRITE is set
 */
const exp STREAM_MODE_TRUNCATE <-- UHEX-0000000000000020 ;

/*
 * this errno indicates that an element was missused
 * (folder as file, reversed or non-link as link)
 */
const exp ERRNO_ELEMENT_WRONG_TYPE    <-- UHEX-0040000000000000 ;
/*
 * this errno indicates that no such element exist
 */
const exp ERRNO_ELEMENT_NOT_EXIST     <-- UHEX-0080000000000000 ;
/*
 * this errno indicates that an element
 * with the given name already exist
 */
const exp ERRNO_ELEMENT_ALREADY_EXIST <-- UHEX-0100000000000000 ;
/*
 * this errno indicates that the file
 * system or a part of it ran out of space
 */
const exp ERRNO_OUT_OF_SPACE          <-- UHEX-0200000000000000 ;
/*
 * this errno indicates that it was
 * used to write on an read-only element
 */
const exp ERRNO_READ_ONLY             <-- UHEX-0400000000000000 ;
/*
 * this errno indicates that an element
 * was locked for this use or that a wrong
 * lock has been used
 */
const exp ERRNO_ELEMENT_LOCKED        <-- UHEX-0800000000000000 ;
/*
 * this errno indicates some IO error
 */
const exp ERRNO_IO_ERR                <-- UHEX-1000000000000000 ;
/*
 * the errno indicates the an misuse
 */
const exp ERRNO_ILLEGAL_ARG           <-- UHEX-2000000000000000 ;
/*
 * the errno indicates the system could not reserve enough memory
 */
const exp ERRNO_OUT_OF_MEMORY         <-- UHEX-4000000000000000 ;
/*
 * this errno indicates an unknown error
 * (it is not used by the system)
 */
const exp ERRNO_ERROR                 <-- UHEX-8000000000000000 ;

/*
 * this is the STREAM-ID of the stdin stream.
 */
const exp STD_IN  <-- 0 ;
/*
 * this is the STREAM-ID of the stdout stream.
 */
const exp STD_OUT <-- 1 ;
/*
 * this is the STREAM-ID of the stdlog/err stream.
 */
const exp STD_LOG <-- 2 ;

/* 
 * opens a new stream with the given mode
 * for the given file
 */
func exp streams_new(char# file, num mode) --> <struct file_stream# stream> {
	asm file --> X00 mode --> X01 ::
		INT INT_STREAMS_NEW
	>> X00 --> stream ;
}

/*
 * write size bytes from data on the stream.
 * if successfull wrote will be the number of bytes actually wrote
 * and -1 if an error occured (than errno will have a non zero value)
 * 
 * the stream is just either a (struct file_stream#) (with write access) or STD_OUT/STD_LOG
 */
func exp stream_write(num stream, num size, byte# data) --> <num wrote, unum errno> {
	asm stream --> X00 size --> X01 data --> X02 ::
		XOR STATUS, STATUS
		INT INT_STREAMS_WRITE
		CMP X01, -1
		// MOV X02, STATUS
		MOV [X04 + 8], STATUS // the X04 register points to the function structure
	>> X01 --> wrote /* X02 --> errno /* instead move the STATUS register manually */ ;
}

/*
 * read size bytes from the stream to data.
 * if successfull read will be the number of bytes actually read
 * and -1 if an error occured (than errno will have a non zero value)
 * if 
 * 
 * the stream is just either a (struct file_stream#) (with read access) or STD_IN
 */
func exp stream_read(num stream, num size, byte# data) --> <num read, unum errno> {
	asm stream --> X00 size --> X01 data --> X02 ::
		XOR STATUS, STATUS
		INT INT_STREAMS_READ
		CMP X01, -1
		// MOV X02, STATUS
		MOV [X04 + 8], STATUS // the X04 register points to the function structure
	>> X01 --> read /* X02 --> errno */ ;
}

/*
 * get a element handle from the file system for the specified file
 * 
 * if the specified element is a folder or link
 * to a folder this operation will fail
 * 
 * if the specified element is a link to a file
 * the link will be followed
 * 
 * if the operation fails for some reason file
 * will be set to ((struct fs_element#) -1)
 * and errno will have a non-zero value
 */
func exp fs_get_file(char# file_path) --> (struct fs_element# file, unum errno) {
	asm file_path --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_GET_FILE
		MOV [X04 + 8], STATUS
	>> X00 --> file /* STATUS --> errno */ ; // just manually set errno
}

/*
 * get a element handle from the file system for the specified folder
 * 
 * if the specified element is a file or link
 * to a file this operation will fail
 * 
 * if the specified element is a link to a folder
 * the link will be followed
 * 
 * if the operation fails for some reason folder
 * will be set to ((struct fs_element#) -1)
 * and errno will have a non-zero value
 */
func exp fs_get_folder(char# folder_path) --> (struct fs_element# folder, unum errno) {
	asm folder_path --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_GET_FOLDER
		MOV [X04 + 8], STATUS
	>> X00 --> folder /* STATUS --> errno */ ; // just manually set errno
}

/*
 * get a element handle from the file system for the specified link
 * 
 * if the specified element is a file or
 * folder this operation will fail
 * 
 * if the operation fails for some reason link
 * will be set to ((struct fs_element#) -1)
 * and errno will have a non-zero value
 */
func exp fs_get_file(char# link_path) --> (struct fs_element# link, unum errno) {
	asm link_path --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_GET_LINK
		MOV [X04 + 8], STATUS
	>> X00 --> link /* STATUS --> errno */ ; // just manually set errno
}

/*
 * get a element handle from the file system
 * for the specified element
 * 
 * if the operation fails for some reason element
 * will be set to ((struct fs_element#) -1)
 * and errno will have a non-zero value
 */
func exp fs_get_file(char# path) --> (struct fs_element# element, unum errno) {
	asm path --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_GET_LINK
		MOV [X04 + 8], STATUS
	>> X00 --> element /* STATUS --> errno */ ; // just manually set errno
}

/*
 * duplicates a file system element handle
 * 
 * if the system could not allocate enugh
 * memory for the duplicated file system
 * element handle duplicate will be set to
 * ((struct fs_element#) -1)
 */
func exp fs_duplicate_handle(struct fs_element# orig) --> <struct fs_element# duplicate> {
	asm orig --> X00 ::
		INT INT_FS_DUPLICATE_HANDLE
	>> X00 --> duplicate ;
}

/*
 * replaces the file system element handle with a
 * handle of its parent folder.
 * 
 * if this operation fails errno will be set to a
 * non-zero value and to zero opn success
 */
func exp fs_get_parent(struct fs_element# element) --> <unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_PARENT
		MOV [X04], STATUS
	>> ;
}

/*
 * creates a file system element handle from an id
 * 
 * if this operation fails element will be set to
 * ((struct fs_element#) -1)
 */
func exp fs_from_id(num id) --> <struct fs_element# element, unum errno> {
	asm id --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_FROM_ID
		MOV [X04 + 8], STATUS
	>> X00 --> element ;
}

const exp FS_NO_TIME <-- -1 ;

/*
 * get the create date of a file system element
 */
func exp fs_get_create(struct fs_element# element) --> <unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_CREATE
		MOV [X04], STATUS
	>> ;
}

/*
 * get the last modify date of a file system element
 */
func exp fs_get_last_mod(struct fs_element# element) --> <unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_LAST_MOD
		MOV [X04], STATUS
	>> ;
}

/*
 * get the last meta modify date of a file system element
 */
func exp fs_get_last_meta_mod(struct fs_element# element) --> <unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_LAST_META_MOD
		MOV [X04], STATUS
	>> ;
}

/*
 * set the create date of a file system element
 */
func exp fs_set_create(struct fs_element# element, num time) --> <unum errno> {
	asm element --> X00 time --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_SET_CREATE
		MOV [X04], STATUS
	>> ;
}

/*
 * set the last modify date of a file system element
 */
func exp fs_set_last_mod(struct fs_element# element, num time) --> <unum errno> {
	asm element --> X00 time --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_SET_LAST_MOD
		MOV [X04], STATUS
	>> ;
}

/*
 * set the last meta modify date of a file system element
 */
func exp fs_set_last_meta_mod(struct fs_element# element, num time) --> <unum errno> {
	asm element --> X00 time --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_SET_LAST_META_MOD
		MOV [X04], STATUS
	>> ;
}

const exp FS_NO_LOCK <-- 0 ;

/*
 * get the lock data of an element or FS_NO_LOCK if there is currently no lock
 * 
 * lock_data will be set to -1 if an error occurs
 */
func exp fs_get_lock_data(struct fs_element# element) --> <unum lock_data, unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_LOCK_DATA
		MOV [X04 + 8], STATUS
	>> X00 --> lock_data ;
}

/*
 * get the lock time of the element
 * 
 * if an error occured the value of time is undefined
 * and errno will have a non zero value
 */
func exp fs_get_lock_time(struct fs_element# elemnet) --> <num time, unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_LOCK_TIME
		MOV [X04 + 8], STATUS
	>> X01 --> time ;
}

/*
 * lock the file system element with the given lock_data
 * 
 * the new lock will be saved in the given struct fs_element:lock value
 */
func exp fs_lock_element(struct fs_element# element, unum lock_data) --> <unum errno> {
	asm elemnet --> X00 lock_data --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_LOCK
		MOV [X04], STATUS
	>> ;
}

/*
 * unlocks the file system elment and set its struct fs_element:lock value to FS_NO_LOCK
 */
func exp fs_unlock_element(struct fs_element# element) --> <unum errno> {
	asm element --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_UNLOCK
		MOV [X04], STATUS
	>> ;
}

/*
 * deletes the given file system element
 * 
 * on success this operation also frees the given element
 */
func exp fs_delete_element(struct fs_element# element, unum parent_lock) --> <unum errno> {
	asm elemnet --> X00 parent_lock --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_DELETE
		MOV [X04], STATUS
	>> ;
}

/*
 * moves an file system element
 * 
 * if the element should not change the parent folder set new_parent to -1
 * 
 * if the name should remain unmodified set new_name to -1
 */
func exp fs_move_element(struct fs_element# element, struct fs_element# new_parent, char# new_name, unum old_parent_lock) --> <unum errno> {
	asm element --> X00 new_parent --> X01 new_name --> X02 old_parent_lock --> X03 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_MOVE
		MOV [X04], STATUS
	>> ;
}

/*
 * get the falgs of the file system element
 * 
 * if this operation fails flags will be set to -1
 */
func exp fs_get_flags(struct fs_element# element) --> <num flags, unum errno> {
	asm element -> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_GET_FLAGS
		MOV [X04 + 8], STATUS
	>> X01 --> flags ;
}

/*
 * this flags marks a file system element as a folder
 * a link marked with this flag is a link to a folder
 */
const exp FS_FLAG_FOLDER         <-- UHEX-0000001 ;
/*
 * this flags marks a file system element as a file
 * a link marked with this flag is a link to a file
 */
const exp FS_FLAG_FILE           <-- UHEX-0000002 ;
/*
 * this flags marks a file system element as a link
 * a link is also marked with the FS_FLAG_FOLDER xor FS_FLAG_FILE
 */
const exp FS_FLAG_LINK           <-- UHEX-0000004 ;
/*
 * this flags marks a file system element as read-only
 */
const exp FS_FLAG_READ_ONLY      <-- UHEX-0000008 ;
/*
 * this flags marks a file system element as executable
 */
const exp FS_FLAG_EXECUTABLE     <-- UHEX-0000010 ;
/*
 * this flags marks a file system element as hidden
 */
const exp FS_FLAG_HIDDEN         <-- UHEX-0000020 ;
/*
 * this flags marks a file system folder as sorted
 * 
 * this flag is currently ignnored
 */
const exp FS_FLAG_FOLDER_SORTED  <-- UHEX-0000040 ;
/*
 * this flags marks a file system file as encrypted
 * 
 * this flag is currently ignnored
 */
const exp FS_FLAG_FILE_ENCRYPTED <-- UHEX-0000080 ;

/*
 * modifies the flags of an element
 * 
 * the flags FS_FLAG_FOLDER, FS_FLAG_FILE and FS_FLAG_LINK are not allowed to be modified
 * 
 * add_flags and rem_flags are not allowed to contain common bits
 * (add_flags & rem_flags) has to be zero
 */
func exp fs_mod_flags(struct fs_element# element, udword add_flags, udword rem_falgs) --> <unum errno> {
	asm element --> X00 ::
		MVDW X01, X06 // skip the AND X0[12], UHEX-00000000FFFFFFFF
		MVDW X02, X07
		XOR STATUS, STATUS
		INT INT_FS_ELEMENT_MOD_FLAGS
		MOV [X04], [STATUS]
	>> ;
}

/*
 * get the child count of an folder
 * 
 * on success child_count will be set to the child count of the given folder
 * otherwise child_count will be set to -1 and errno will have a non-zero value
 */
func exp fs_folder_child_count(struct fs_element# folder) --> <num child_count, unum errno> {
	asm folder --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_FOLDER_CHILD_COUNT
		MOV [X04 + 8], STATUS
	>> X01 --> child_count ;
}

/*
 * get the child of the given index from the given parent folder
 * 
 * this operation just replaces the struct fs_elemnet:id value with the di of the child elemnet
 */
func exp fs_child_from_index(struct fs_element# folder, dword index) --> <unum errno> {
	asm folder --> X00 ((udword) index) --> X01 :: // negative values will be to large anyway
		XOR STATUS, STATUS
		INT INT_FS_FOLDER_GET_CHILD_OF_INDEX
		MOV [X04], STATUS
	>> ;
}

/*
 * get the child with the given name from the given parent folder
 * 
 * this operation just replaces the struct fs_elemnet:id value with the di of the child elemnet
 */
func exp fs_child_from_name(struct fs_elemnet# folder, char# name) --> <unum errno> {
	asm folder --> X00 name --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_FOLDER_GET_CHILD_OF_NAME
		MOV [X04], STATUS
	>> ;
}

/*
 * creates a new folder and adds it to the given folder
 * as child elemnet
 * 
 * this operation also replaces to struct fs_element:id
 * value with the id of the child element and sets the
 * struct fs_element:lock value to FS_NO_LOCK
 */
func exp fs_create_folder(struct fs_elemnet# parent, char# name) --> <unum errno> {
	asm folder --> X00 name --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_FOLDER_ADD_FOLDER
		MOV [X04], STATUS
	>> ;
}

/*
 * creates a new file and adds it to the given folder
 * as child elemnet
 * 
 * this operation also replaces to struct fs_element:id
 * value with the id of the child element and sets the
 * struct fs_element:lock value to FS_NO_LOCK
 */
func exp fs_create_folder(struct fs_elemnet# parent, char# name) --> <unum errno> {
	asm folder --> X00 name --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_FOLDER_ADD_FILE
		MOV [X04], STATUS
	>> ;
}

/*
 * creates a new link to the given target and adds it
 * to the given folder as child elemnet
 * 
 * this operation also replaces to struct fs_element:id
 * value with the id of the child element and sets the
 * struct fs_element:lock value to FS_NO_LOCK
 */
func exp fs_create_folder(struct fs_elemnet# parent, char# name, struct fs_element# target) --> <unum errno> {
	asm folder --> X00 name --> X01 target --> X02 ::
		XOR STATUS, STATUS
		INT INT_FS_FOLDER_ADD_LINK
		MOV [X04], STATUS
	>> ;
}

/*
 * get the lenght of the file
 */
func exp fs_file_length(struct fs_element# file) --> <num length, unum errno> {
	asm file --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_FILE_LENGTH
		MOV [X04 + 8], STATUS
	>> X01 --> length ;
}

/*
 * calculate the SHA-256 hash code of the file
 */
func exp fs_file_hash(struct fs_element# file, byte# data) --> <unum errno> {
	asm file --> X00 data --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_FILE_HASH
		MOV [X04], STATUS
	>> ;
}

/*
 * read a spezified part of the given file
 * 
 * the sum of position and length is not allowed to be
 * greather than the length of the file
 * 
 * position and length are not allowed to be negative
 */
func exp fs_file_read(struct fs_elemnet# file, num position, num length, byte# buffer) --> <unum errno> {
	asm file --> X00 position --> X01 length --> X02 buffer --> X03 ::
		XOR STATUS, STATUS
		INT INT_FS_FILE_READ
		MOV [X04], STATUS
	>> ;
}

/*
 * overwrite a spezified part of the given file
 * 
 * the sum of position and length is not allowed to be
 * greather than the length of the file
 * 
 * position and length are not allowed to be negative
 */
func exp fs_file_write(struct fs_elemnet# file, num position, num length, byte# buffer) --> <unum errno> {
	asm file --> X00 position --> X01 length --> X02 buffer --> X03 ::
		XOR STATUS, STATUS
		INT INT_FS_FILE_WRITE
		MOV [X04], STATUS
	>> ;
}

/*
 * append some data to the end of file
 * 
 * length is not allowed to be negative
 */
func exp fs_file_append(struct fs_elemnet# file, num length, byte# buffer) --> <unum errno> {
	asm file --> X00 length --> X01 buffer --> X02 ::
		XOR STATUS, STATUS
		INT INT_FS_FILE_APPEND
		MOV [X04], STATUS
	>> ;
}

/*
 * removes all content from the file past the new_length
 * 
 * this operation fails if new_length is greather or equal
 * to the current length of the file or negative
 */
func exp fs_file_truncate(struct fs_element# file, num new_length) --> <unum errno> {
	asm file --> X00 new_length --> X01 >>
		XOR STATUS, STATUS
		INT INT_FS_FILE_TRUNCATE
		MOV [X04], STATUS
	>> ;
}

/*
 * get the target element of the link
 * 
 * this operation replaces the struct fs_element:id value
 * with the id of the target element and sets the
 * struct fs_element: lock value to FS_NO_LOCK
 */
func exp fs_link_get_target(struct fs_element# link) --> <unum errno> {
	asm link --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_LINK_GET_TARGET
		MOV [X04], STATUS
	>> ;
}

/*
 * set the target element of the given link
 */
func exp fs_link_set_target(struct fs_element# link, struct fs_element# new_target) --> <unum errno> {
	asm link --> X00 new_target --> X01 ::
		XOR STATUS, STATUS
		INT INT_FS_LINK_SET_TARGET
		MOV [X04], STATUS
	>> ;
}

/*
 * get the currently used lock of the file system
 * 
 * not that this is possibly not the lock wich
 * locks the file system
 */
func exp fs_get_lock() --> <unum used_lock> {
	asm ::
		MOV [X04], FS_LOCK
	>> ;
}

/*
 * set the lock which should be used by file system
 * operations for the file system lock
 *
 * note that this operation does not change the file
 * systems lock
 */
func exp fs_set_lock(unum use_lock) {
	asm ::
		MOV FS_LOCK, X06
	>> ;
}

/*
 * lock the file system with the given lock data
 */
func exp fs_lock(unum lock_data) --> <unum errno> {
	asm lock_data --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_LOCK
		MOV [X04], STATUS
	>> ;
}

/*
 * unlock the file system with the currently used lock
 */
func exp fs_unlock() --> <unum errno> {
	asm lock_data --> X00 ::
		XOR STATUS, STATUS
		INT INT_FS_UNLOCK
		MOV [X04], STATUS
	>> ;
}

/*
 * get the current system time
 *
 * if the system time is unavailable time will be set to -1
 */
func exp time_get() --> <num time> {
	asm ::
		INT INT_TIME_GET
	>> X00 --> time ;
}

/*
 * wait the given time
 * 
 * if success is zero remain_nanoseconds/remain_seconds may be greather zero
 * otherwise it is ensured that the program waited at least the requested
 * amount of time (maby more)
 */
func exp time_wait(num nanoseconds, num seconds) --> <num remain_nanoseconds, num remain_seconds, num success> {
	asm nanoseconds --> X00 seconds --> X01 ::
		INT INT_TIME_WAIT
	>> X00 --> remain_nanoseconds X01 --> remain_seconds X02 --> success ;
}

/*
 * create a random number
 */
func exp random() --> <num value> {
	asm ::
		INT INT_RANDOM
	>> X00 --> value ;
}

/*
 * copy some memory from source to target
 * 
 * the target block is not allowed to overlap with the source block
 */
func exp mem_copy(byte# target, byte# source, num length) {
	asm target --> X00 source --> X01 length --> X02 ::
		INT INT_MEMORY_COPY
	>> ;
}

/*
 * copy some memory from source to target
 * 
 * the target block is allowed to overlap with the source block
 */
func exp mem_move(byte# target, byte# source, num length) {
	asm target --> X00 source --> X01 length --> X02 ::
		INT INT_MEMORY_MOVE
	>> ;
}

/*
 * set a block of memory to the same byte-vale
 */
func exp mem_bset(byte# memory, ubyte value, num byte_count) {
	asm memory --> X00 byte_count --> X02 ::
		MVB X07, X01
		INT INT_MEMORY_BSET
	>> ;
}

/*
 * set a memory block to the same int-64 value
 * 
 * note that num_count is in 64-bit unints
 */
func exp mem_set(num# memory, num value, num num_count) {
	asm memory --> X00 value --> X01 num_count --> X02 ::
		INT INT_MEMORY_SET
	>> ;
}

/*
 * calculate the length of a string
 */
func exp string_length(char# string) --> <num length> {
	asm string --> X00 ::
		INT INT_STRING_LENGTH
	>> X00 --> length ;
}

/*
 * compare two strings
 */
func exp string_compare(char# a, char# b) --> <num result> {
	asm a --> X00 b --> X01 ::
		INT INT_STRING_COMPARE
	>> X00 --> result ;
}

/*
 * creates a string representation from the given number with the given
 * number base (10 for decimal, 16 for hexadecimal, ...)
 * 
 * if buffer_size is smaller than the string representation would be
 * the buffer the buffer will be resized to the size of the string
 * and saved in new_buffer
 * 
 * size will be set to the byte_count of the string (with the '\0' character)
 * 
 * base has to be a number from 2 to 36 (both inclusive)
 */
func exp string_from_number(num number, char# buffer, num base, num buffer_size) --> <num size, char# new_buffer, num new_buffer_size, unum errno> {
	asm number --> X00 buffer --> X01 base --> X02 buffer_size --> X03 ::
		XOR STATUS, STATUS
		INT INT_NUMBER_TO_STRING
		MOV [X04 + 16], STATUS
	>> X00 --> size X01 --> new_buffer X03 --> new_buffer_size ;
}

/*
 * creates a string representation from the given floating point
 * number
 * 
 * if buffer_size is smaller than the string representation would be
 * the buffer the buffer will be resized to the size of the string
 * and saved in new_buffer
 * 
 * size will be set to the byte_count of the string (with the '\0' character)
 */
func exp string_from_fpnumber(fpnum fpnumber, char# buffer, num buffer_size) --> <num size, char# new_buffer, num new_buffer_size, unum errno> {
	asm fpnumber --> X00 buffer --> X01 buffer_size --> X02 ::
		XOR STATUS, STATUS
		INT INT_FPNUMBER_TO_STRING
		MOV [X04 + 16], STATUS
	>> X00 --> size X01 --> new_buffer X02 --> new_buffer_size ;
}

/*
 * get the number which is represented by the given string with
 * the given base
 * 
 * on success success will have a non-zero value and if the operation
 * fails success will be zero (illegal string or base)
 * 
 * base is only allowed to be from 2 to 36 (both inclusive)
 */
func exp string_to_number(char# string_number, num base) --> <num number, num success> {
	asm string_number --> X00 base --> X01 ::
		INT INT_STRING_TO_NUMBER
	>> X00 --> number X01 --> success ;
}

/*
 * get the number which is represented by the given string with
 * the given base
 * 
 * on success success will have a non-zero value and if the operation
 * fails success will be zero (illegal string or base)
 * 
 * base is only allowed to be from 2 to 36 (both inclusive)
 */
func exp string_to_fpnumber(char# string_number) --> <fpnum fpnumber, num success> {
	asm string_number --> X00 ::
		INT INT_STRING_TO_FPNUMBER
	>> X00 --> fpnumber X01 --> success ;
}


func exp string_formatt(char# source, char# target, num target_size, num argument_count, num# arguments) --> <num string_length, char# new_target, num new_target_size, unum errno> {
	asm /* source --> X06 target --> X07 target_size --> X08 argument_count --> X09   arguments --> X0A */ ::
		|> X00..X03 can freely be used others has to be saved
		MOV X00, X09
		DEC X00
		LSH X00, 3
		MOV X02, X00
		PUSH X02 |> push backup data size
		INT INT_MEMORY_ALLOC
		CMP X00, -1
		JMPEQ outOfMem
		#X04_ADDRESS ( REGISTER_MEMORY_START + ( 8 * ( 6 + 4 ) ) )
		MOV X01, X04_ADDRESS
		ADD X02, 8
		INT INT_MEMORY_COPY
		PUSH X00 |> push backup data
		|> push args for formatt (X02, X01, X00)
		PUSH X08
		PUSH X07
		PUSH X06
		|> copy arguments
		#X04_ADDRESS ( REGISTER_MEMORY_START + ( 8 * ( 6 + 3 ) ) )
		MOV X00, X03_ADDRESS
		MOV X01, X0A
		ADD X02, 8
		INT INT_MEMORY_MOVE |> if the arguments already point to the register memory space
		|:	X00       : X03_ADDRESS
		|	[X01+n]   : arguments (orig)
		|	X02       : argument_size
		|	X03..XNN  : arguments (copied)
		|	[SP - 8]  : source
		|	[SP - 16] : target
		|	[SP - 24] : buffer_size
		|	[SP - 32] : backup data
		|	[SP - 40] : backup data size
		|:>
		POP X00
		POP X01
		POP X02
		JMP formatt
	@outOfMem
		INT INT_OUT_OF_MEM_ERROR
	@formatt
		XOR STATUS, STATUS
		INT INT_STRING_FORMATT
		|> set the result values
		MOV X03, [SP - 32] |> X03 <-- backup data
		MOV X03, [X03] |> X03 <-- function structure
		MOV [X03], X00
		MOV [X03 + 8], X01
		MOV [X03 + 16], X02
		MOV [X03 + 24], STATUS
		MOV X00, X04_ADDRESS
		POP X01
		POP X02
		INT INT_MEMORY_MOVE
	>> 
}

/*
 * convert a UTF-16 string to a UTF-8 string
 * 
 * u8string will be the converted UTF-8 string
 * new_buffer_size will be the new size of the buffer
 * length will be the offset of the '\0' character
 */
func exp string_to_u8(char# string, ubyte# u8buffer, num buffer_size) --> <ubyte# u8string, num new_buffer_size, num u8string_length> {
	asm string --> X00 u8buffer --> X01 buffer_size --> X02 ::
		INT INT_STR_TO_U8STR
	>> X01 --> u8string X02 --> new_buffer_size X03 --> u8string_length ;
}

/*
 * convert a UTF-8 string to a UTF-16 string
 * 
 * string will be the converted UTF-16 string
 * new_buffer_size will be the new size of the buffer
 * length will be the offset of the '\0' character
 */
func exp string_from_u8(ubyte# u8string, char# buffer, num buffer_size) --> <char# string, num new_buffer_size, num string_length> {
	asm string --> X00 u8buffer --> X01 buffer_size --> X02 ::
		INT INT_STR_TO_U8STR
	>> X01 --> u8string X02 --> new_buffer_size X03 --> u8string_length ;
}

/*
 * load the data of a file to the memory
 * 
 * after the files data are no longer needed they should be freed
 */
func exp fs_load_file(char# file) --> <byte# data, num size, unum errno> {
	asm file --> X00 ::
		XOR STATUS, STATUS
		INT INT_LOAD_FILE
		MOV [X04 + 16], STATUS
	>> X00 --> data X01 --> size ;
}

/*
 * get the data of a file in the memory space
 * 
 * note that all chare the same file data and no copies are made
 * 
 * the system will remember that the file has already been loaded
 * and a later call of this function with the same file will return
 * the same memory address (and the same size value)
 * 
 * if the file was previusly loaded with this function loaded_file
 * will be zero, if the function loaded the file loaded_file will
 * be set to a non zero value
 */
func exp fs_get_file(char# file) --> <byte# data, num size, num loaded_file, unum errno> {
	asm file --> X00 ::
		XOR STATUS, STATUS
		INT INT_GET_FILE
		MOV [X04 + 24], STATUS
	>> X00 --> data X01 --> size X02 --> loaded_file ;
}
