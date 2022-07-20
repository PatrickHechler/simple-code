|> the start of an simple file with executable function
~IF #~ME
	#ILLEGAL_MEM_POS -1
	#DEP_LOAD_ERR_POS -1
	#OUT_OF_MEM_ERR_POS -1
	#DEP_LOAD_ERR_MSG_POS -1
	#OUT_OF_MEM_ERR_MSG_POS -1
	#START_ERR_MSG_POS -1
~ELSE
	#READ_SYM "[THIS]" #ADD~ME >
~ENDIF
	|> save X00, X01 in X10, X11
		MOV X10, X00
		MOV X11, X01
	|> skip first stack init if not needed
		CMP SP, -1
		JMPNE skipStackInit
	|> allocate the stack
		MOV X00, 1024
		INT INT_MEMORY_ALLOC
		CMP X00, -1
		JMPEQ startError
		MOV SP, X00
	@skipStackInit
	|> make stack grow-able
		#OFF ( INT_ERROR_ILLEGAL_MEMORY * 8 )
		#REL_POS ( ILLEGAL_MEM_POS - --POS-- )
		LEA [INTP + OFF], REL_POS
		#OFF ~DEL
		#REL_POS ~DEL
	|> resize interrupt table
		#EXP~INT_OUT_OF_MEM_ERR ( INTERRUPT_COUNT )
		#EXP~INT_DEP_LOAD_ERR ( INT_OUT_OF_MEM_ERR + 1 )
		#EXP~INTERRUPT_COUNT ( INT_DEP_LOAD_ERR + 1 )
		#LEN ( INTERRUPT_COUNT * 8 )
		MOV X00, INTP
		MOV X01, LEN
		INT INT_MEMORY_REALLOC
		CMP X01, -1
		JMPEQ startError
	|> add my interrupts
		#OFF ( INT_OUT_OF_MEM_ERR * 8 )
		#REL_POS ( OUT_OF_MEM_ERR_POS - --POS-- )
		LEA [X01 + OFF], REL_POS
		#OFF ( INT_DEP_LOAD_ERR * 8 )
		#REL_POS ( DEP_LOAD_ERR_POS - --POS-- )
		LEA [X01 + OFF], REL_POS
		MOV INTP, X01
		MOV INTCNT, INTERRUPT_COUNT
	|> call main
		MOV X00, 16
		INT INT_MEMORY_ALLOC
		CMP X00, -1
		JMPEQ startError
		MOV X04, X00
		MOV [X04], X10
		MOV [X04 + 8], X11
	#EXP~MAIN_ADDRESS_REL_POS --POS--
	@main |> dummy main
		CALL main
	#EXP~MAIN_ADDRESS ( --POS-- - 8 )
	~IF ( ( MAIN_ADDRESS - 8 ) != MAIN_ADDRESS_REL_POS )
		~ERROR { "error: (" MAIN_ADDRESS " - " 8 ") != " MAIN_ADDRESS_REL_POS }
	~ENDIF
		MOV X00, [X04]
		INT INT_EXIT
#STACK_SIZE_POS --POS--
: 1024 >
: > |> align
~IF #~ME
	#EXP~ILLEGAL_MEM_POS --POS--
~ENDIF
	|> check if stack overflow
		#PUSH_CMD HEX-24
		#CALL_CMD HEX-20
		MOV X00, [X09]
		LSH X00, 56
		CMP X00, PUSH_CMD
		JMPEQ stackOverflow
		CMP X00, CALL_CMD
		JMPEQ stackOverflow
		#OFF ( INT_ERROR_ILLEGAL_MEMORY * 8 )
		MOV [INTP + OFF], -1
		INT INT_ERROR_ILLEGAL_MEMORY
		#OFF ~DEL
	@stackOverflow
		MOV X00, SP
		#REL_POS ( STACK_SIZE_POS - --POS-- )
		MOV X03, [IP + REL_POS]
		SUB X00, X03
		#REL_POS ( STACK_SIZE_POS - --POS-- )
		MVAD X02, X03, 1024
		MOV X01, X02
		INT INT_MEMORY_REALLOC
		CMP X01, -1
		JMPEQ outOfMem
		#REL_POS ( STACK_SIZE_POS - --POS-- )
		MOV [IP + REL_POS], X02
		#REL_POS ~DEL
		ADD X01, X03
		MOV SP, X01
		IRET
~IF #~ME
	#EXP~DEP_LOAD_ERR_POS --POS--
~ENDIF
|> TODO: write error msg
	MOV X00, 6
	INT INT_EXIT
~IF #~ME
	#EXP~OUT_OF_MEM_ERR_POS --POS--
~ENDIF
@outOfMem
|> TODO: write error msg
	MOV X00, 5
	INT INT_EXIT
@startError
|> TODO: write error msg
	MOV X00, 4
	INT INT_EXIT
|> error messages:
~IF #~ME
	#EXP~DEP_LOAD_ERR_MSG_POS --POS--
~ENDIF
: 'UTF-16LE' "error while loading a dependency!\0" >
: > |> align
~IF #~ME
	#EXP~OUT_OF_MEM_ERR_MSG_POS --POS--
~ENDIF
: 'UTF-16LE' "I went out of memory (stack overflow?)!\0" >
: > |> align
~IF #~ME
	#EXP~START_ERR_MSG_POS --POS--
~ENDIF
: 'UTF-16LE' "failed to initialize the program!\0" >
