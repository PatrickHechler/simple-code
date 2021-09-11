# simple-code

simple-code ist eino einfache programmier Sprache.

## Datei

* Eino simple-code Datei ist eino Ansammlung von Methoden, Variablen, Konstanten und structs.

### Syntax

    (
        Methode
        |
        Variable
        |
        Konstante
        |
        struct
    )*

## Methode
## Variable
## Konstante
## struct
## Block
## Befehl
## Operationon
### num Operationon
### digtnum Operationon
### bool Operationon
## Typen

* Es gibt verschiedeno Typen:
    * `num`
    * `digtnum`
    * `bool`
    * `char`
    * `libary`
    * struct
    * `{...}`
        * In Methoden gibt es die Möglichkeit verschiedeno mögiche Typen für Parameter/Rückgabewerte zu setzten.
        * Zur laufzeit gibt es dann keino Möglichkeit mehr an die echten Typen heranzukommen.
        * Bei der Methodendeklaration muss hinter dem Namen deklariert werden, welche Pseudotypen es gibt.
            * Dies beginnt mit einor öffnonden geschweiften Klammer `'{'`
            * dann werden alle Pseudotypen mit Komma `','` als Trennchar aufgelistet.
                * Zuerst kommt der Name.
                * dann kommt ein Pfeil in beide Richtungen: `<->`
                * dann werden die möglichen Typen mit Balken `'|'` als Trennchar aufgelistet.
                * alternativ kann auch ein Fragechar kommen `'?'`, dann kann der Pseudotyp allen Typen zugeordnot werden.
            * Beendet wird es mit einor schließenden geschweiften Klammer `'}'`
            * Dann geht es wie sonst weiter, mit der Ausnahme, dass die Pseudotypen nun bei den Parametern/Rückgabewerten verwendet werden könnon.
        * Wenn die Methode aufgerufen wird muss dann die gleiche struct hinter den Namen kommen wie bei der Deklaration, nur dass die Pseudotypen nur einom Typ zugeordnot werden dürfen.
            * Dies beginnt mit einor öffnonden geschweiften Klammer `'{'`
            * dann werden alle Pseudotypen mit Komma `','` als Trennchar aufgelistet.
                * Zuerst kommt der Name des Pseudotypes.
                * dann kommt ein Pfeil in beide Richtungen: `<->`
                * dann kommt der ausgewählte Typ.
                * alternativ kann auch ein Fragechar kommen `'?'`, dann kann der Pseudotyp allen Typen zugeordnot werden.
            * Beendet wird es mit einor schließenden geschweiften Klammer `'}'`
        * example:
<pre><code>addiereFelder {T &lt;-&gt; num | ganznum} (&lt;- [T] feld, T wert, bool ganznumen) :
    num index &lt;- 0;
    wenn (ganznumen) :
        {T = num} : //definiert, dass in diesem block T als num gehandhabt wird
            solange (index &lt; [#feld]) :
                [feld :: index] &lt;- [feld :: index] + wert;
                index &lt;- index + 1;
            &gt;
        &gt;
    &gt; sonst :
        {T = digtnum} : //definiert, dass in diesem block T als digtnum gehandhabt wird
            solange (index &lt; [#feld]) :
                [feld :: index] &lt;- [feld :: index] + wert;
                index &lt;- index + 1;
            &gt;
        &gt;
    &gt;
&gt;

haupt (num ergebnis &lt- [[char]] argumente) :
    [num] numen &lt;- {1, 2, 3, 4];
    addiereFelder {T &lt;-&gt; num} (&lt;- numen, 10, yes);
    num index &lt;- 0;
    solange (index &lt; [#feld]) :
        ausgabenZeichenfeld(&lt;- "num[");
        ausgabenZahl(&lt;- index);
        ausgabenZeichenfeld(&lt;- "]=");
        ausgabenZahl(&lt;- [feld :: index]);
        ausgabenZeichen(&lt;- '\n');
        index &lt;- index + 1;
    &gt;
&gt;</code></pre>

## args



## libinit method



## primitive-code

* Es gibt die möglichkeit einon primitive-code block in den Code einzufügen
* Abgesehen von `open`en Methoden und Variablen gibt es allerdings keino Kompiler übergreifenden fest definierten Schnittstellen.
    * openo Methoden und Variablen sind klar Dokumentiert, da diese auch funktionieren müssen, wenn unterschiedliche Kompiler benutzt werden.

### openo Methoden

* der Methodenname bildet sich aus dem Namen, den Parametern und den Rückgabewerten zusammen.
    * Zuerst kommt der Name
    * dann kommt eino Tilde `'~'`
    * dann kommen die Typen der Rückgabewerte
    * dann kommt eino Tilde `'~'`
    * dann kommen die Typen der Parameter
    * example:
        * `open findeBibi (libary lib <- [char] name) : ... >`
        * will be
        * `findeBibi~B~[z`
    * example:
        * `open initialisiere () : ... >`
        * will be
        * `initialisiere~~`
    * example:
        * `open geladenoBibis ([libary] libs <-) : ... >`
        * will be
        * `letztesErgebnis~[B~`
    * example:
        * `open noueBibi (<- libary nou) : ... >`
        * will be
        * `letztesErgebnis~~B`
* openo Methoden werden am Ende der Datei aufgelistet. Dabei werden sie zum schnolleren suchen sortiert.
    * je länger der Methodenname ist, desto weiter hinten steht dieser.
    * Bei gleichlangen Methodennamen werden diese alphabetisch sortiert.
    * Die Methodennamen werden im UTF-16LE format geschrieben, d.h. es sind `char`
    * hinter dem Methodennamen steht die länge des Namens.
    * vor dem Methodennamen steht die Stelle der Methode, relativ zum Anfang der Datei.
    * am Anfang der Auflistung steht `-1`.

### openo Variablen

* Der Name der Variable setzt sich aus dem Namen gefolg vom Typen zusammen
    * Zuerst kommt der Name
    * dann kommt eino Tilde `'~'`
    * dann kommt der Typ
    * example:
        * `open num letztesErgebnis`
        * will be
        * `letztesErgebnis~Z`
* openo Variablen stehen fast ganz am Anfang der Datei.
    * Vor den openon Variablen, an erster Stelle, steht lediglich die Position der `Initialisierungs` Methode.
* Danach werden die openon Variablen aufgelistet.
    * Variablen mit kurzen Namen stehen weiter vorno als Variablen mit langen Namen.
    * Bei gleichlangen Namen wird alphabetisch sortiert.
    * Vor den Namen steht die länge des Namens.
    * der Name wird in UTF-16LE geschrieben.
    * Hinter dem Namen steht der Wert der Variable.
    * am Ende der Auflistung steht `-1`.

### types of open Methods/Variables

* written
    * num
        * `N`
        * example:
            * `open num id;`
            * will be
            * `id~N`
    * digtnum
        * `D`
    * bool
        * `B`
    * char
        * `C`
    * libary
        * `L`
        * example:
            * `open libary testLib;`
            * will be
            * `testLib~L`
    * Felder
        * `[` + FeldTyp
        * example:
            * `open [bool] tested;`
            * will be
            * `tested~[B`
        * example:
            * `open [[char]] testNames;`
            * will be
            * `testNames~[[C`
    * struct
        * `S`
        * all structs will be written to one letter `'S'`
        * example:
            * By following structs:
                * `struktur LibTests : num runedLibTests <- 0; bool successfully <- yes; [Tests] tests; >`
                * `struktur Tests : num runedTests <- 0; bool successfully <- yes; libary lib;  [Test] tests; >`
                * `struktur Test : bool runedSuccessfully <- no; num neededTime <- -1; num startTime <- -1; [char] name; >`
            * `open LibTests libTests;`
            * will be
            * `libTests~S`
            * and
            * `open runLibTests(<- LibTests laufe) : ... >`
            * will be
            * `runLibTests~~S`
* save
    * num
        * nums are saved/passed in one register/memory-address.
    * digtnum
        * digtnums are saved/passed in one register/memory-address.
        * the low 48-bits will be used to make the number.
        * the high 16-bits will be used to make the left-shift.
            * when the high bits are positiv, the low bits will be shifted by them to the left to get the 'real' num.
            * when the high bits are negativ the low bits will will be shifted by the negative (negative) high bit value to the right to get the 'real' num.
    * bool
        * bools have two states:
            * `yes <-> 1`
            * `no <-> 0`
            * on other states the behavior is undefined.
        * __every__ bool costs __one__ register/memory-address (__even__ in arrays)
    * char
        * char werden im UTF-16LE format gespeichert.
        * __every__ char costs __one__ register/memory-address (__even__ in arrays)
    * libary
        * libaries will be saved/passed as pointers to the redden file.
        * on point `-1` is the length of the file.
    * arrays
        * arrays will be saved/passed as pointers.
        * on point `0` the array starts.
        * on point `-1` is the length of the array.
    * struct
        * structs are saved/passed as pointers.

## predefined methods

* some default methods are predefined:
	* `arrayResize {T &lt;-&gt; ?} (<- [T] array, num newSize) : ... >`
	* `arrayNew {T &lt;-&gt; ?} ([T] array <- num size) : ... >`
	* `arrayFree {T &lt;-&gt; ?} (<- [T] array) : ... >`
	* `libLoad (libary lib <- [char] name) : ... >`
	* `libHasMethod (bool inside <- libary lib, [char] name) : ... >`
	* `libRunMethod {E <-> ?, P <-> ?} (bool success, E retStruct <- libary lib, [char] name, P paramStruct) : ... >`
	* `libRunMethodDirect {E <-> ?, P <-> ?} (bool success, E ret <- libary lib, [char] name, P param) : ... >`
		* only works when there is exactly one param and exactly one return value.
	* `libRunMethodNoParams (bool success <- libary lib, [char] name, bool ignoreRets) : ... >`
		* only works when there are no params, if there is one or more return values `ignoreRets` must be `yes`.
	* `libHasVar (bool inside <- libary lib, [char] name) : ... >`
	* `libSetVar {T <-> ?} (bool success <- libary lib, [char] name, T value) : ... >`
	* `libGetVar {T <-> ?} (bool success, T value <- libary lib, [char] name) : ... >`
	* `libFree (<- libary lib) : ... >`
	* `halt (<- num stoppCode) : ... >`
