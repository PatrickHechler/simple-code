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

`dep '[DEPENDENCY]';`
* the DEPENDENCY is the full path from a included source directory
* file type:
    * the DEPENDENCY is either a symbol-file (`*.psf`)
    * or a simple-code source file (`*.ssc`)
    * or a primitive-code source file (`*.psc`)
* if the DEPENDENCY is a source file (primitive or simple) only the export-symbols will be extracted and used

### function

`func (exp)? [NAME] < ( [PARAMETER] ( , [PARAMETER])*)? > [BLOCK]`

### variable
### structure
