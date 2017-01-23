package wci.frontend.sc;

import java.util.HashSet;
import java.util.Hashtable;

import wci.frontend.TokenType;

public enum SCTokenType implements TokenType {
	//control structures
	IF, ELSE,
    
	//loop structures
	DO, FOR, WHILE,
	
	//types
    VOID, INT, FLOAT, CHAR, 
    
    //statements
    RETURN,

    // Special symbols.
    
    //multiplicative operators    
    STAR("*"), SLASH("/"), PERCENT("%"), BITWISE_AND("&"), // for generating "&&"
    LOGICAL_AND("&&"),
    
    //additive operators
    PLUS("+"), MINUS("-"), BITWISE_OR("|"),// for generating "||"
    LOGICAL_OR("||"),
    
    //unary operators
    NOT("!"),
    
    //assignment
    EQUALS("="),
    
    //other
    COMMA(","), SEMICOLON(";"), QUOTE("\""), APOSTROPHE("'"), DOT("."),
    
    //comparison    
    NOT_EQUALS("!="), EQUALS_EQUALS("=="), LESS_THAN("<"), LESS_EQUALS("<="),
    GREATER_EQUALS(">="), GREATER_THAN(">"),
    
    //parens
    LEFT_PAREN("("), RIGHT_PAREN(")"), LEFT_BRACKET("["), RIGHT_BRACKET("]"),
    LEFT_BRACE("{"), RIGHT_BRACE("}"),
    
    IDENTIFIER, INTEGER, REAL, STRING, CHARACTER,
    ERROR, END_OF_FILE;

    private static final int FIRST_RESERVED_INDEX = IF.ordinal();
    private static final int LAST_RESERVED_INDEX  = RETURN.ordinal();

    private static final int FIRST_SPECIAL_INDEX = STAR.ordinal();
    private static final int LAST_SPECIAL_INDEX  = RIGHT_BRACE.ordinal();

    private String text;  // token text

    /**
     * Constructor.
     */
    SCTokenType()
    {
        this.text = this.toString().toLowerCase();
    }

    /**
     * Constructor.
     * @param text the token text.
     */
    SCTokenType(String text)
    {
        this.text = text;
    }

    /**
     * Getter.
     * @return the token text.
     */
    public String getText()
    {
        return text;
    }

    // Set of lower-cased Pascal reserved word text strings.
    public static HashSet<String> RESERVED_WORDS = new HashSet<String>();
    static {
    	SCTokenType values[] = SCTokenType.values();
        for (int i = FIRST_RESERVED_INDEX; i <= LAST_RESERVED_INDEX; ++i) {
            RESERVED_WORDS.add(values[i].getText().toLowerCase());
        }
    }

    // Hash table of Pascal special symbols.  Each special symbol's text
    // is the key to its Pascal token type.
    public static Hashtable<String, SCTokenType> SPECIAL_SYMBOLS =
        new Hashtable<String, SCTokenType>();
    static {
    	SCTokenType values[] = SCTokenType.values();
        for (int i = FIRST_SPECIAL_INDEX; i <= LAST_SPECIAL_INDEX; ++i) {
            SPECIAL_SYMBOLS.put(values[i].getText(), values[i]);
        }
    }
}
