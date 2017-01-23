package wci.frontend.sc;

public enum SCErrorCode
{
    FUNCTION_REDEFINED("Redefined function"),
    INCOMPATIBLE_ASSIGNMENT("Incompatible assignment"),
    INCOMPATIBLE_TYPES("Incompatible types"),
    IDENTIFIER_REDEFINED("Redefined identifier"),
    IDENTIFIER_UNDEFINED("Undefined identifier"),
    INVALID_CHARACTER("Invalid character"),
    INVALID_CHARACTER_LITERAL("Invalid character literal"),
    INVALID_NUMBER("Invalid number"),
    INVALID_RETURN("Invalid return. void function cannot return a value"),
    INVALID_STATEMENT("Invalid statement"),
    INVALID_TARGET("Invalid assignment target"),
    INVALID_TYPE("Invalid type"),
    INVALID_PARM("Invalid parameter"),
    MISSING_COMMA("Missing ,"),
    MISSING_RIGHT_BRACE("Missing }"),
    MISSING_EQUALS("Missing ="),
    MISSING_EQUALS_EQUALS("Missing =="),
    MISSING_IDENTIFIER("Missing identifier"),
    MISSING_LEFT_PAREN("Missing ("),
    MISSING_RIGHT_PAREN("Missing )"),
    MISSING_SEMICOLON("Missing ;"),
    MISSING_VARIABLE("Missing variable"),
    NO_MAIN_FUNCTION("\"main\" function not defined in program"),
    MAIN_FUNCTION_WRONG("\"main\" function does not have correct definition of \"void main()\""),
    RANGE_INTEGER("Integer literal out of range"),
    RANGE_REAL("Real literal out of range"),
    UNDECLARED_IDENTIFIER("Undeclared identifier"),
    UNEXPECTED_EOF("Unexpected end of file"),
    UNEXPECTED_TOKEN("Unexpected token"),
    WRONG_NUMBER_OF_PARMS("Wrong number of actual parameters"),

    // Fatal errors.
    IO_ERROR(-101, "Object I/O error"),
    TOO_MANY_ERRORS(-102, "Too many syntax errors");

    private int status;      // exit status
    private String message;  // error message

    /**
     * Constructor.
     * @param message the error message.
     */
    SCErrorCode(String message)
    {
        this.status = 0;
        this.message = message;
    }

    /**
     * Constructor.
     * @param status the exit status.
     * @param message the error message.
     */
    SCErrorCode(int status, String message)
    {
        this.status = status;
        this.message = message;
    }

    /**
     * Getter.
     * @return the exit status.
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * @return the message.
     */
    public String toString()
    {
        return message;
    }
}
