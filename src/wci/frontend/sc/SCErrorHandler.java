package wci.frontend.sc;

import static wci.frontend.sc.SCErrorCode.*;
import static wci.message.MessageType.*;
import wci.frontend.Parser;
import wci.frontend.Token;
import wci.frontend.sc.SCErrorCode;
import wci.message.Message;

public class SCErrorHandler
{
    private static final int MAX_ERRORS = 25;

    private static int errorCount = 0;   // count of syntax errors

    /**
     * Getter.
     * @return the syntax error count.
     */
    public int getErrorCount()
    {
        return errorCount;
    }

    /**
     * Flag an error in the source line.
     * @param token the bad token.
     * @param errorCode the error code.
     * @param parser the parser.
     * @return the flagger string.
     */
    public void flag(Token token, SCErrorCode errorCode, Parser parser)
    {
        // Notify the parser's listeners.
        parser.sendMessage(new Message(SYNTAX_ERROR,
                                       new Object[] {token.getLineNumber(),
                                                     token.getPosition(),
                                                     token.getText(),
                                                     errorCode.toString()}));

        if (++errorCount > MAX_ERRORS) {
            abortTranslation(TOO_MANY_ERRORS, parser);
        }
    }

    /**
     * Abort the translation.
     * @param errorCode the error code.
     * @param parser the parser.
     */
    public void abortTranslation(SCErrorCode errorCode, Parser parser)
    {
        // Notify the parser's listeners and then abort.
        String fatalText = "FATAL ERROR: " + errorCode.toString();
        parser.sendMessage(new Message(SYNTAX_ERROR,
                                       new Object[] {0,
                                                     0,
                                                     "",
                                                     fatalText}));
        System.exit(errorCode.getStatus());
    }
}
