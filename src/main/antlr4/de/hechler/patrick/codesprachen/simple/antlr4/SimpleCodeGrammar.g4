grammar SimpleCodeGrammar;

datei
:
(
	(
		methode
	)
	|
	(
		konstante
	)
	|
	(
		struktur
	)
)*
EOF
;

methode
:
	(
		OFFEN
	)? NAME SIMPLE_KLAMMER_AUF
	(
		(
			(
				art NAME
			)
			(
				KOMMA art NAME
			)*
		)? PFEIL
		(
			(
				art NAME
			)
			(
				KOMMA art NAME
			)*
		)?
	)? SIMPLE_KLAMMER_ZU block
;

block
:
	BLOCK_KLAMMER_AUF
	(
		befehl
	)* BLOCK_KLAMMER_ZU
;

befehl
:
	(
		(
			art NAME
			(
				PFEIL wert
			)?
		)
		|
		(
			NAME PFEIL wert
		)
		|
		(
			NAME SIMPLE_KLAMMER_AUF
			(
				wert
				(
					KOMMA wert
				)* PFEIL wert
				(
					KOMMA wert
				)*
			)? SIMPLE_KLAMMER_ZU
		)
	) BEFEHLS_ENDE
;

wert
:
	(
		konstWert
	)
	|
	(
		WERT_ZEICHENKETTE
	)
	|
	(
		LADE wert
	)
	|
	(
		SIMPLE_KLAMMER_AUF wert operant wert SIMPLE_KLAMMER_ZU
	)
;

operant
:
	(
		PLUS
	)
	|
	(
		MINUS
	)
	|
	(
		MAL
	)
	|
	(
		GETEILT
	)
	|
	(
		MODULO
	)
	|
	(
		UND
	)
	|
	(
		UNDUND
	)
	|
	(
		ODER
	)
	|
	(
		ODERODER
	)
	|
	(
		ENTWEDERODER
	)
	|
	(
		ZAHLNICHT
	)
	|
	(
		JANENICHT
	)
;

struktur
:
	STRUKTUR NAME BLOCK_KLAMMER_AUF
	(
		art NAME
	)* BLOCK_KLAMMER_ZU
;

konstante
:
	KONST konstArt NAME PFEIL konstWert
;

konstWert
:
	(
		WERT_POS_ZAHL
	)
	|
	(
		WERT_POS_KOMMAZAHL
	)
	|
	(
		WERT_ZEICHEN
	)
	|
	(
		WERT_JA
	)
	|
	(
		WERT_NE
	)
;

art
:
	(
		konstArt
	)
	|
	(
		BIBLIOTHEK
	)
	|
	(
		NAME
	)
	|
	(
		ARRAY_KLAMMER_AUF art ARRAY_KLAMMER_ZU
	)
;

konstArt
:
	(
		ZAHL
	)
	|
	(
		KOMMAZAHL
	)
	|
	(
		ZEICHEN
	)
	|
	(
		JANE
	)
;

WERT_POS_KOMMAZAHL
:
	(
		'0x' [0-9a-fA-F]+ ',' [0-9a-fA-F]*
	)
	|
	(
		'0o' [0-7]+ ',' [0-7]*
	)
	|
	(
		'0b' [01]+ ',' [01]*
	)
	|
	(
		[1-9] [0-9]* ',' [0-9]*
	)
	|
	(
		'0' ',' [0-9]*
	)
;

WERT_POS_ZAHL
:
	(
		'0x' [0-9a-fA-F]+
	)
	|
	(
		'0o' [0-7]+
	)
	|
	(
		'0b' [01]+
	)
	|
	(
		[1-9] [0-9]*
	)
	|
	(
		'0'
	)
;

WERT_ZEICHEN
:
	'\''
	(
		(
			~['\\]
		)
		|
		(
			'\\' ['\\rnt]
		)
	) '\''
;

WERT_ZEICHENKETTE
:
	'"'
	(
		(
			~["\\]
		)
		|
		(
			'\\' ["\\rnt]
		)
	)* '"'
;

WERT_JA
:
	'ja'
;

WERT_NE
:
	'ne'
;

STRUKTUR
:
	'struktur'
;

ZEICHEN
:
	'zeichen'
;

LADE
:
	'lade'
;

BIBLIOTHEK
:
	'bibliothek'
;

JANE
:
	'jane'
;

KOMMAZAHL
:
	'kommazahl'
;

ZAHL
:
	'zahl'
;

OFFEN
:
	'offen'
;

KONST
:
	'konst'
;

ZAHLNICHT
:
	'~'
;

JANENICHT
:
	'!'
;

ENTWEDERODER
:
	'^'
;

ODERODER
:
	'||'
;

ODER
:
	'|'
;

UNDUND
:
	'&&'
;

UND
:
	'&'
;

MODULO
:
	'%'
;

GETEILT
:
	'/'
;

MAL
:
	'*'
;

MINUS
:
	'-'
;

PLUS
:
	'+'
;

BEFEHLS_ENDE
:
	';'
;

KOMMA
:
	','
;

PFEIL
:
	'<-'
;

ARRAY_KLAMMER_AUF
:
	'['
;

ARRAY_KLAMMER_ZU
:
	']'
;

SIMPLE_KLAMMER_AUF
:
	'('
;

SIMPLE_KLAMMER_ZU
:
	')'
;

BLOCK_KLAMMER_AUF
:
	'{'
;

BLOCK_KLAMMER_ZU
:
	'}'
;

NAME
:
	[a-zA-Z_-]+
;

WS
:
	[ \t\r\n]+ -> skip
;
