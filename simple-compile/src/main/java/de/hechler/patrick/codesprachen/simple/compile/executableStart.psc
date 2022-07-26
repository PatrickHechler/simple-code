|:
|   the start of an simple file with executable function
|   
|   this file exports the following symbols:
|       INT_OUT_OF_MEM_ERR
|           the interrupt number of the out of memory error interrupt
|       INT_DEP_LOAD_ERR
|           the interrupt number of the dependency load error interrupt
|       INTERRUPT_COUNT
|           the interrupt count which includes the self defined interrupts by this start
|       MAIN_ADDRESS
|           the position where the (relative) main address should be written
|       MAIN_ADDRESS_REL_POS
|           the position from where the main address should be relativized
|>
~IF #~ME
    |> set the self import constants to any value
	#ILLEGAL_MEM_POS -1
	#DEP_LOAD_ERR_POS -1
	#OUT_OF_MEM_ERR_POS -1
	#DEP_LOAD_ERR_MSG_POS -1
	#OUT_OF_MEM_ERR_MSG_POS -1
	#START_ERR_MSG_POS -1
	#ILLEGAL_MEM_ERR_MSG_POS -1
	#ILLEGAL_MEM_ERR_MSG_LEN -1
~ELSE
	~READ_SYM "[THIS]" #ADD~ME 1 >
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
		#OFF ( INT_ERRORS_ILLEGAL_MEMORY * 8 )
		#REL_POS ( ILLEGAL_MEM_POS - --POS-- )
		LEA [INTP + OFF], REL_POS
		#OFF ~DEL
		#REL_POS ~DEL
		#EXP~INT_OUT_OF_MEM_ERR ( INTERRUPT_COUNT )
		#EXP~INT_DEP_LOAD_ERR ( INT_OUT_OF_MEM_ERR + 1 )
		#EXP~INTERRUPT_COUNT ( INT_DEP_LOAD_ERR + 1 )
	|> resize interrupt table
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
	|> @illegalMemory
		#REL_POS ( ILLEGAL_MEM_ERR_MSG_POS - --POS-- )
		LEA X12, REL_POS
		#REL_POS ~DEL
		MOV X11, ILLEGAL_MEM_ERR_MSG_LEN
		MOV X10, 9
		JMP fail
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
	#REL_POS ( DEP_LOAD_ERR_MSG_POS - --POS-- )
	LEA X12, REL_POS
	#REL_POS ~DEL
	MOV X11, DEP_LOAD_ERR_MSG_LEN
	MOV X10, 9
	JMP fail
~IF #~ME
	#EXP~OUT_OF_MEM_ERR_POS --POS--
~ENDIF
@outOfMem
	#REL_POS ( OUT_OF_MEM_ERR_MSG_POS - --POS-- )
	LEA X12, REL_POS
	#REL_POS ~DEL
	MOV X11, OUT_OF_MEM_ERR_MSG_LEN
	MOV X10, 8
	JMP fail
@startError
	#REL_POS ( OUT_OF_MEM_ERR_MSG_POS - --POS-- )
	LEA X12, REL_POS
	#REL_POS ~DEL
	MOV X11, OUT_OF_MEM_ERR_MSG_LEN
	MOV X10, 4
|>	JMP fail
@fail
	MOV X00, STD_LOG
	MOV X01, X11
	MOV X02, X12
	INT INT_STREAMS_WRITE
	MOV X00, X10
	INT INT_EXIT
|> error messages:
~IF #~ME
	#EXP~DEP_LOAD_ERR_MSG_POS --POS--
~ENDIF
: CHARS 'UTF-16LE' "[ERROR]: a dependency could not be loaded!" >
~IF #~ME
	#EXP~DEP_LOAD_ERR_MSG_LEN ( --POS-- - DEP_LOAD_ERR_MSG_POS )
~ENDIF
: > |> align
~IF #~ME
	#EXP~OUT_OF_MEM_ERR_MSG_POS --POS--
~ENDIF
: CHARS 'UTF-16LE' "[ERROR]: I went out of memory (stack overflow?)!" >
~IF #~ME
	#EXP~OUT_OF_MEM_ERR_MSG_LEN ( --POS-- - OUT_OF_MEM_ERR_MSG_POS )
~ENDIF
: > |> align
~IF #~ME
	#EXP~START_ERR_MSG_POS --POS--
~ENDIF
: CHARS 'UTF-16LE' "[ERROR]: failed to initialize the program!" >
~IF #~ME
	#EXP~START_ERR_MSG_LEN ( --POS-- - START_ERR_MSG_POS )
~ENDIF
: > |> align
~IF #~ME
	#EXP~ILLEGAL_MEM_ERR_MSG_POS --POS--
~ENDIF
: CHARS 'UTF-16LE' "[ERROR]: illegal memory access!" >
~IF #~ME
	#EXP~ILLEGAL_MEM_ERR_MSG_LEN ( --POS-- - ILLEGAL_MEM_ERR_MSG_POS )
~ENDIF
: > |> align at the end
