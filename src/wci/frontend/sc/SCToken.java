package wci.frontend.sc;

import wci.frontend.Source;
import wci.frontend.Token;

public class SCToken extends Token
{
    /**
     * Constructor.
     * @param source the source from where to fetch the token's characters.
     * @throws Exception if an error occurred.
     */
    protected SCToken(Source source)
        throws Exception
    {
        super(source);
    }
    
    /**
     * Single location to handle string escaping
     * @param textBuffer buffer to store raw text read
     * @param valueBuffer buffer to store logical text read
     * @throws Exception if an error occurred.
     */
    protected void checkEscape(StringBuilder textBuffer, StringBuilder valueBuffer)
    	throws Exception
    {
    	char currentChar = currentChar();
    
		switch(currentChar) {
			
			//\n = add newline to value buffer
			case 'n':
				textBuffer.append(currentChar);
				valueBuffer.append('\n');
				nextChar();		//consume
				break;
			
			//\' = add apostrophe to value buffer
			case '\'':
				textBuffer.append(currentChar);
				valueBuffer.append('\'');
				nextChar();		//consume
				break;
				
			//\" = add quote to value buffer
			case '"':
				textBuffer.append(currentChar);
				valueBuffer.append('"');
				nextChar();		//consume
				break;
				
			//\\ = add backslash to value buffer
			case '\\':
				textBuffer.append(currentChar);
				valueBuffer.append('\\');
				nextChar();		//consume
				break;
			
			//\0 = add null character to value buffer
			case '0':
				textBuffer.append(currentChar);
				valueBuffer.append('\0');
				nextChar();		//consume
				break;

			//\xABCD = add hex value as a char to value buffer
			case 'x': 
				StringBuilder hex = new StringBuilder();
				
				textBuffer.append(currentChar);
				currentChar = nextChar();	//consume 'x'
				
				//read up to 4 valid hex digits
				for (int i = 0; i < 4; i++) {
					//if this is a valid hex digit, store it and continue
					if (isHexDigit(currentChar)) {
						textBuffer.append(currentChar);
						hex.append(currentChar);
						currentChar = nextChar();	//consume digit
					}
					//not a valid hex digit, exit loop
					else {
						break;
					}
				}
				
				//if at least 1 hex digit
				if (hex.length() > 0) {
					//convert hex string into integer, add represented character to value buffer
					char c = (char)Integer.parseInt(hex.toString(), 16);
					valueBuffer.append(c);        						
				}
				//if no valid hex digits
				else {
					//add an 'x' to buffer like it's an unknown escape sequence
					valueBuffer.append('x');
				}

				break;
			default:
				//add char to value buffer
				valueBuffer.append(currentChar);
				break;
		}
    }
	    
    /**
     * Checks if character is a valid hex digit
     * @param c the character to check
     * @return true if c is a character in 0-9 or A-F
     */
    protected boolean isHexDigit(char c) {
    	if (c >= '0' && c <= '9')
    		return true;
    	if (c >= 'A' && c <= 'F')
    		return true;
    	
    	return false;
    }

}
