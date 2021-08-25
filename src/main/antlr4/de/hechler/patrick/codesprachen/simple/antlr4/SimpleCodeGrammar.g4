grammar SimpleCodeGrammar;

r
:
	WS//TODO
;

OFFEN:'offen';
KONST:'konst';

NAME
:
	[a-zA-Z_-]+
;

WS
:
	[ \t\r\n]+ -> skip
;
