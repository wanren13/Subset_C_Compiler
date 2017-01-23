package wci.frontend.sc.parsers;

import java.util.EnumSet;

import wci.frontend.Token;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.icodeimpl.ICodeNodeTypeImpl;
import wci.intermediate.symtabimpl.Predefined;
import wci.intermediate.typeimpl.TypeChecker;
import static wci.frontend.sc.SCTokenType.*;
import static wci.frontend.sc.SCErrorCode.*;

public class IfStatementParser extends StatementParser {

    public IfStatementParser(SCParserTD parent)
    {
        super(parent);
    }

    // Synchronization set for {.
    private static final EnumSet<SCTokenType> AFTER_IF_SET =
        StatementParser.STMT_START_SET.clone();
    static {
    	AFTER_IF_SET.addAll(StatementParser.STMT_FOLLOW_SET);
    }

    /**
     * Parse an IF statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token)
        throws Exception
    {
        token = nextToken();  // consume the IF

        // Create an IF node.
        ICodeNode ifNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.IF);

        // Parse the expression.
        // The IF node adopts the expression subtree as its first child.
        ExpressionParser expressionParser = new ExpressionParser(this);
        ICodeNode exprNode = expressionParser.parse(token);
        ifNode.addChild(exprNode);

        
        // Type check: The expression type must be boolean.
        TypeSpec exprType = exprNode != null ? exprNode.getTypeSpec()
                                             : Predefined.undefinedType;
        if (!TypeChecker.isBoolean(exprType)) {
            errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
        }

        // Synchronize at the first statement token.
        token = synchronize(AFTER_IF_SET);

        // Parse the statement.
        // The IF node adopts the statement subtree as its second child.
        StatementParser statementParser = new StatementParser(this);
        ifNode.addChild(statementParser.parse(token));
        token = currentToken();
        
        if(token.getType() == SEMICOLON)
        	token= nextToken();

        // Look for an ELSE.
        if (token.getType() == ELSE) {
            token = nextToken();  // consume the ELSE

            // Parse the ELSE statement.
            // The IF node adopts the statement subtree as its third child.
            ifNode.addChild(statementParser.parse(token));
        }

        return ifNode;
    }
}
