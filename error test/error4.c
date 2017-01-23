//not specifying return value in function
int test(int x) {
	return;
}

void main() {
	int x;
	x = test(1);
	printf("%d\n", x);
}
