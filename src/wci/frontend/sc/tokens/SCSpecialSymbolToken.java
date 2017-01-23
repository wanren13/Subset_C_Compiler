package wci.frontend.sc.tokens;

import static wci.frontend.sc.SCErrorCode.INVALID_CHARACTER;
import static wci.frontend.sc.SCTokenType.ERROR;
import static wci.frontend.sc.SCTokenType.SPECIAL_SYMBOLS;
import wci.frontend.Source;
import wci.frontend.sc.SCToken;

public class SCSpecialSymbolToken extends SCToken {
    /**
     * Constructor.
     * @param source the source from where to fetch the token's characters.
     * @throws Exception if an error occurred.
     */
    public SCSpecialSymbolToken(Source source)
        throws Exception
    {
        super(source);
    }

    /**
     * Extract a Pascal special symbol token from the source.
     * @throws Exception if an error occurred.
     */
    protected void extract()
        throws Exception
    {
        char currentChar = currentChar();

        text = Character.toString(currentChar);
        type = null;

        switch (currentChar) {

            // Single-character special symbols.
            case '+':  case '-':  case '*':  case '/':  case '%':
            case ',':  case ';':  case '\'': case '(':  case ')':  
            case '[':  case ']':  case '{':  case '}':  {
                nextChar();  // consume character
                break;
            }

            // = or ==
            case '=': {
                currentChar = nextChar();  // consume '=';

                if (currentChar == '=') {
                    text += currentChar;
                    nextChar();  // consume '='
                }

                break;
            }

            // < or <=
            case '<': {
                currentChar = nextChar();  // consume '<';

                if (currentChar == '=') {
                    text += currentChar;
                    nextChar();  // consume '='
                }
                
                break;
            }

            // > or >=
            case '>': {
                currentChar = nextChar();  // consume '>';

                if (currentChar == '=') {
                    text += currentChar;
                    nextChar();  // consume '='
                }

                break;
            }
            
            // ! or !=
            case '!': {
                currentChar = nextChar();  // consume '!';

                if (currentChar == '=') {
                    text += currentChar;
                    nextChar();  // consume '='
                }

                break;
            }
            
            // ||
            case '|': {
                currentChar = nextChar();  // consume '|';

                if (currentChar == '|') {
                    text += currentChar;
                    nextChar();  // consume '|'
                }

                break;
            }
            
         // &&
            case '&': {
                currentChar = nextChar();  // consume '&;

                if (currentChar == '&') {
                    text += currentChar;
                    nextChar();  // consume '&'
                }

                break;
            }

            default: {
                nextChar();  // consume bad character
                type = ERROR;
                value = INVALID_CHARACTER;
            }
        }

        // Set the type if it wasn't an error.
        if (type == null) {
            type = SPECIAL_SYMBOLS.get(text);
        }
    }
}
