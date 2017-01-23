//assigning void function to value
void test(int x) {
	printf("%d\n", x);
}

void main() {
	int x = 1;
	x = test(x);
	printf("%d\n", x);
}
