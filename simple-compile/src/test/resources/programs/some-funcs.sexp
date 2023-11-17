
typedef struct {
	ubyte[16] bytes;
} uuid;

typedef struct {
	uuid uuid;
	unum name_length;
	char# name;
} my_custom_type;

func add <num result> <-- (num a, num b);

func deref <num value> <-- (num# addr);

func addu <unum result> <-- (unum a, unum b);

func derefu <unum value> <-- (unum# addr);
