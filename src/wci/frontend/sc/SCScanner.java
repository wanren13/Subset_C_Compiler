package wci.frontend.sc;

import static wci.frontend.Source.*;
import static wci.frontend.sc.SCErrorCode.*;
import wci.frontend.*;
import wci.frontend.sc.tokens.*;

public class SCScanner extends Scanner {
	/**
	 * Constructor
	 * @param source the source to be used with this scanner.
	 */
	public SCScanner(Source source)
	{
		super(source);
	}

	/**
	 * Extract and return the next Pascal token from the source.
	 * @return the next token.
	 * @throws Exception if an error occurred.
	 */
	protected Token extractToken()
			throws Exception
			{
		skipWhiteSpace();

		Token token;
		char currentChar = currentChar();

		// Construct the next token.  The current character determines the
		// token type.
		if (currentChar == EOF) {
			token = new EofToken(source);
		}
		else if (Character.isLetter(currentChar)) {
			token = new SCWordToken(source);
		}
		else if (Character.isDigit(currentChar)) {
			token = new SCNumberToken(source);
		}
		else if (currentChar == '"') {
			token = new SCStringToken(source);
		}
		else if (currentChar == '\'') {
			token = new SCCharacterToken(source);
		}
		else if (SCTokenType.SPECIAL_SYMBOLS
				.containsKey(Character.toString(currentChar))) {
			token = new SCSpecialSymbolToken(source);
		}
		else {
			token = new SCErrorToken(source, INVALID_CHARACTER,
					Character.toString(currentChar));
			nextChar();  // consume character
		}

		return token;
			}

	/**
	 * Skip whitespace characters by consuming them.  A comment is whitespace.
	 * @throws Exception if an error occurred.
	 */
	private void skipWhiteSpace()
			throws Exception
			{
		char currentChar = currentChar();
		
		boolean isDivide = false;

		while ((Character.isWhitespace(currentChar) || currentChar == '/') && !isDivide) {

			// Start of a comment?
			if (currentChar == '/') {

				// "//"style comment
				if(peekChar() == '/'){
					currentChar = nextChar(); //consume the 1st /
					do {
						currentChar = nextChar(); //consume rest /'s                        
					} while ((currentChar != EOL) && (currentChar != EOF));
				}

				// "/* */" style comment
				else if(peekChar() == '*'){
					boolean stop = false;  
					currentChar = nextChar();  // consume the 1st /
					currentChar = nextChar();  // consume *
					do {
						currentChar = nextChar();
						if(currentChar == '*' && peekChar() == '/'){
							stop = true;
							currentChar = nextChar(); // consume *
							currentChar = nextChar(); // consume /
						}
					} while (!stop && (currentChar != EOF));
				}
				
				else
					isDivide = true;
			}

			// whitespace
			else {
				currentChar = nextChar();  // consume whitespace character
			}
		}
			}
}
