package wci.frontend.sc.tokens;

import static wci.frontend.Source.EOF;
import static wci.frontend.sc.SCErrorCode.INVALID_CHARACTER_LITERAL;
import static wci.frontend.sc.SCErrorCode.UNEXPECTED_EOF;
import static wci.frontend.sc.SCTokenType.CHARACTER;
import static wci.frontend.sc.SCTokenType.ERROR;
import wci.frontend.Source;
import wci.frontend.sc.SCToken;

public class SCCharacterToken extends SCToken {
    /**
     * Constructor.
     * @param source the source from where to fetch the token's characters.
     * @throws Exception if an error occurred.
     */
    public SCCharacterToken(Source source)
        throws Exception
    {
        super(source);
    }

    /**
     * Extract a Pascal string token from the source.
     * @throws Exception if an error occurred.
     */
    protected void extract()
        throws Exception
    {
        StringBuilder textBuffer = new StringBuilder();
        StringBuilder valueBuffer = new StringBuilder();

        char currentChar = nextChar();  // consume initial quote
        textBuffer.append('\'');

       	//if char starts with a backslash, check what the following escaped char is
       	if (currentChar == '\\') {
        	currentChar = nextChar();	//consume \
        	textBuffer.append(currentChar);
     			
        	//pass escape-sequence handling off to central function
        	checkEscape(textBuffer, valueBuffer);

        	//entire escape sequence is consumed by checkEscape
        }
       	else {
       		textBuffer.append(currentChar);
       		valueBuffer.append(currentChar);
       		currentChar = nextChar();
       	}

        //character string ended with quote
        if (currentChar == '\'') {
            nextChar();  // consume final quote
            textBuffer.append('\'');

            type = CHARACTER;
            value = valueBuffer.toString().charAt(0);
        }
        //character string ended at EOF
        else if (currentChar == EOF) {
            type = ERROR;
            value = UNEXPECTED_EOF;
        }
        //character string has extra characters
        else {
        	/*while (currentChar != '\'')
        		currentChar = nextChar();
        	nextChar();*/
        	type = ERROR;
        	value = INVALID_CHARACTER_LITERAL;
        }

        text = textBuffer.toString();
    }
}
