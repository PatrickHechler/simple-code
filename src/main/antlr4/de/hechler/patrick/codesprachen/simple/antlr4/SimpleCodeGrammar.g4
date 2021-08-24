grammar SimpleCodeGrammar;

r
:
	WS//TODO
;

NAME
:
	[a-zA-Z_-]+
;

WS
:
	[ \t\r\n]+ -> skip
;
