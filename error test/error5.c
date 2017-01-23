//returning value from void function
void test(int x) {
	return x;
}

void main() {
	int x = 1;
	test(x);
	printf("%d\n", x);
}
