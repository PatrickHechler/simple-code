# simple-code

simple-code ist eine einfache programmier Sprache.

## Datei

* Eine simple-code Datei ist eine Ansammlung von Methoden, Variablen, Konstanten und Strukturen.

### Syntax

    (
        Methode
        |
        Variable
        |
        Konstante
        |
        Struktur
    )*

## Methode

* Eine Methode ist ein ausführbarer codeblock, welcher Parameter und Rückgabewerte haben kann.
* Am Ende wird die Methude die mit `haupt` markiert ist gestartet.

### Syntax

    (
        'haupt'
    )?
    NAME
    '('
    (
        (
            TYP
            NAME
            (
                ','
                TYP
                NAME
            )
        )?
        '<-'
        (
            TYP
            NAME
            (
                ','
                TYP
                NAME
            )
        )?
    )?
    ')'
    BLOCK

## Variable

* Eine Variable enthält einen Wert und einen Namen und gehöhrt einem Typ an.
* Angesprochen werden Variablen mit ihren Namen.
* Der Wert kann gehohlt, verändert und gesetzt werden.
* Variablen die noch nicht gesetzt wurden müssen erst gesetzt werden, bevor mit ihnen irgendetwas anderes gemacht wird.
* Variablen können bei ihrer Deklaration mit einem Wert initialisiert werden.
* Variablen die nicht innerhalb einer Methode Deklariert werden, sondern direkt in der Datei, können mit `offen` markiert werden.
    * mit offen markierte Variablen können benutzt werden, wenn die Kompilierte Datei als Bibliothek geladen wird

### Typ

Variablen können verschiedenen Typen angehöhren:
* `zahl`
* `kommazahl`
* `zeichen`
* `jane`
* Struktur
* Felder der Typen
    * Felder können beliebig tief verschachtelt werden und sind dann einfach Felder von Feldern.

### Syntax

    (
        'offen'
        //nur möglich wenn nicht in Methode/Block, sondern in Datei
    )?
    TYP
    NAME
    (
        '<-'
        WERT
    )?
    ';'

## Konstante

* Eine Konstante enthält wie eine Variable einen Wert und einen Namen und gehöhrt ebenfalls einem Typ an.
* Der Wert von Konstanten wird immer bei der Deklaration gesetzt.
* Der Wert einer Konstante ist allerdings konstant, d.h. er kann nicht gesetzt oder verändert werden.
* Angesprochen werden Konstanten wie Variablen mit ihren Namen.
* Konstanten müssen bei ihrer Deklaration mit einem Wert initialisiert werden.
    * Dieser Wert muss allerdings konstant sein.

### Typ

Konstanten können einigen Typen angehöhren:
* `zahl`
* `kommazahl`
* `zeichen`
* `jane`

### Syntax

    TYP
    NAME
    '='
    KONST_WERT
    ';'

## Struktur

* Eine Struktur ist eine Ansammlung von Variablen.

### Syntax

    'struktur'
    ':'
    (
        TYP
        NAME
        ';'
    )*
    '>'

## Block

* Blöcke sind die sachen die Befehle enthalten können und so ausführbar gemacht werden können.
* Variablen und Konstanten die innerhalb eines Blockes deklariert werden, sind außerhalb von diesem Block unbenutzbar.

### Syntax

    ':'
    (
        Befehl
        |
        Block
        |
        Konstante
        |
        Variable
    )*
    '>'

## Befehl

* Ein Befehl ist das was am ende Aktionen ausführen lässt.

### Methodenaufruf

* Methodenbefehle werden benutzt, um Methoden aufzurufen.

#### Syntax

    NAME
    '('
    (
        (
            WERT
            '->>'
            NAME
            (
                ','
                WERT
                '->>'
                NAME
            )*
        )?
        '<-'
        (
            WERT
            '->>'
            NAME
            (
                ','
                WERT
                '->>'
                NAME
            )*
        )?
    )?
    ')'

### Setzbefehl

* Ein Setztbefehl wird verwendet, wenn man Variablen setzten/verändern möchte.

#### Syntax

    NAME
    '<-'
    WERT

### Blockbefehl

* Ein Blockbefehl ist ein Befehl, welcher über min. einen Block verfügt.

#### WennBlockbefehl

* Ein WennBlockbefehl prüft eine Bedingung und überspringt den darauffolgenden Block, wenn die Bedingung nicht zutrifft.

##### Syntax

    'wenn'
    '('
    JANE_WERT
    ')'
    BLOCK

#### SolangeBlockbefehl

* Ein SolangeBlockbefehl lässt einen Block ausführen solange eine Bedingung zutrifft.
* Die Bedingung wird immer am Anfang jeder Iteration überprüft.

##### Syntax

    'solange'
    '('
    JANE_WERT
    ')'
    BLOCK

#### Syntax

    WennBlockbefehl
    |
    SolangeBlockbefehl

### Syntax

    Methodenaufruf
    |
    Setzbefehl
    |
    Blockbefehl

## Name

* Namen werden benutzt um Variablen/Konstanten/Methoden/Strukturen anzusprechen

### Syntax

    [A-Za-z_]+

## Operationen

* Um aus Werten andere Werte zu machen, können Operationen benutzt werden.
* Operationen haben immer Ein- und einen Ausgangswert

### logische Operationen

* logische Operationen können mit `zahl`en und `jane`s verwendet werden.
* bei zahlen werden alle bits einzeln betrachtet `0` bits werden als `ne` und `1` bits werden als `ja` Interpretiert.

#### und Operation

* die und Operation gibt nur `ja` zurück wenn beide Eingangswerte `ja` sind.

##### Syntax

     Wert
	 '&'
	 WERT

#### oder Operation

* die oder Operation gibt `ja` zurück wenn mindestens ein Eingangswert `ja` ist.

##### Syntax

     Wert
	 '|'
	 WERT

#### entweder oder Operation

* die entweder oder Operation gibt `ja` zurück wenn exakt ein Eingangswert `ja` ist.

##### Syntax

     Wert
	 '^'
	 WERT

#### nicht Operation

* die nicht Operation gibt `ja` zurück wenn der Eingangswert `ne` ist.

##### Syntax

	 '!'
	 WERT

### schiebe Operationen

* schiebe Operationen sind lediglich mit `zahl`en möglich.

#### links schub Operation

* die links schub Operation schiebt die bits einer `zahl` um `n` bits nach links.
* dies entspricht einer multiplikation mit `2^n`.

##### Syntax

    WERT
	'<<'
	WERT

#### arithmetische rechts schub Operation

* die arithmetische rechts schub Operation schiebt die bits einer `zahl` um `n` bits nach rechts wobei das höchstwertigste bit erhalten bleibt.
* dies entspricht einer division mit `2^n`.

##### Syntax

    WERT
	'|>>'
	WERT

#### logische rechts schub Operation

* die logische rechts schub Operation schiebt die bits einer `zahl` um `n` bits nach rechts.
* dies entspricht einer division mit `2^n`, wenn die `zahl` positiv ist.

##### Syntax

    WERT
	'>>'
	WERT

### arithmetische Operationen

* arithmetische Operation bearbeitet `zahl`en und `kommazahl`en

#### plus Operation

* die plus Operation addiert zwei Werte und gibt deren Summe zurück

##### Syntax

    WERT
	'+'
	WERT

#### minus Operation

* die minus Operation zieht vom ersten Wert den zweiten ab und gibt das Ergebnis zurück

##### Syntax

    WERT
	'-'
	WERT

#### mal Operation

* die mal Operation multipliziert zwei Werte und gibt das Ergebnis zurück

##### Syntax

    WERT
	'*'
	WERT

#### geteilt Operation

* die geteilt Operation dividiert zwei Werte und gibt das Ergebnis zurück

##### Syntax

    WERT
	'/'
	WERT

#### modulo Operation

* die modulo Operation dividiert zwei `zahl`en und gibt den Rest zurück.
* die modulo Operation ist die einzige arithmetische Operation die nur mit `zahl`en und nicht mit `kommazahl`en funktioniert.

##### Syntax

    WERT
	'%'
	WERT
