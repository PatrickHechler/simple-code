grammar SimpleCodeGrammar;

r
:
;

SKIP
:
	(
		(
			'/*'
			(
				(
					(
						~'*'
					)
					|
					(
						'*' ~'/'
					)
				)
			)* '*/'
		)
		|
		(
			'//'
			(
				~[\r\n]
			)*
		)
		|
		(
			[ \t\r\n]+
		)
	) -> skip
;
