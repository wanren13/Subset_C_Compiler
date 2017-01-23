package wci.frontend.sc.tokens;

import static wci.frontend.Source.EOF;
import static wci.frontend.sc.SCErrorCode.UNEXPECTED_EOF;
import static wci.frontend.sc.SCTokenType.ERROR;
import static wci.frontend.sc.SCTokenType.STRING;
import wci.frontend.Source;
import wci.frontend.sc.SCToken;


public class SCStringToken extends SCToken {
    /**
     * Constructor.
     * @param source the source from where to fetch the token's characters.
     * @throws Exception if an error occurred.
     */
    public SCStringToken(Source source)
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
        textBuffer.append('"');

        // Get string characters.
        do {
        	//if we read a backslash, check what the following escaped char is
        	if (currentChar == '\\') {
        		currentChar = nextChar();
        		     			
        		//pass escape-sequence handling off to central function
        		checkEscape(textBuffer, valueBuffer);

        		//entire escape sequence is consumed by checkEscape
        	}
        	
        	// get new currentChar
        	currentChar = currentChar();
        	
            //keep reading until a quote is read or EOF
            if ((currentChar != '"') && (currentChar != EOF)) {
                textBuffer.append(currentChar);
                valueBuffer.append(currentChar);
                currentChar = nextChar();  // consume character
            }
        } while ((currentChar != '"') && (currentChar != EOF));

        //string ended with quote
        if (currentChar == '"') {
            nextChar();  // consume final quote
            textBuffer.append('"');

            type = STRING;
            value = valueBuffer.toString();
        }
        //string ended at EOF
        else {
            type = ERROR;
            value = UNEXPECTED_EOF;
        }

        text = textBuffer.toString();
    }
}
