//misspelled function name
int test(int x) {
	return x;
}

void main() {
	int x = 1;
	x = tes(x);
	printf("%d\n", x);
}
