/*
 * predefined_strucutres.s
 *
 * this file contains all structures
 * predefined by the simple-compiler
 */

struct fs_element {
    num id,
    num lock
}

struct file_stream {
    struct file* file,
    num pos
}
