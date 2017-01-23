package wci.frontend.sc.parsers;

import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.icodeimpl.*;

import static wci.frontend.sc.SCTokenType.*;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;


public class WhileStatementParser extends StatementParser
{
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public WhileStatementParser(SCParserTD parent)
    {
        super(parent);
    }

    // Synchronization set for DO.
    private static final EnumSet<SCTokenType> WHILE_SET =
        StatementParser.STMT_START_SET.clone();
    static {
        WHILE_SET.add(RIGHT_PAREN);
        WHILE_SET.addAll(StatementParser.STMT_FOLLOW_SET);
    }

    /**
     * Parse a WHILE statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token)
        throws Exception
    {
        token = nextToken();  // consume the WHILE
        token = nextToken();  // consume the '('

        // Create LOOP, TEST, and NOT nodes.
        ICodeNode loopNode = ICodeFactory.createICodeNode(LOOP);
        ICodeNode breakNode = ICodeFactory.createICodeNode(TEST);
        ICodeNode notNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.NOT);

        // The LOOP node adopts the TEST node as its first child.
        // The TEST node adopts the NOT node as its only child.
        loopNode.addChild(breakNode);
        breakNode.addChild(notNode);

        // Parse the expression.
        // The NOT node adopts the expression subtree as its only child.
        ExpressionParser expressionParser = new ExpressionParser(this);
        notNode.addChild(expressionParser.parse(token));

        // Synchronize at the DO.
        token = synchronize(WHILE_SET);
        if (token.getType() == RIGHT_PAREN) {
            token = nextToken();  // consume the ')'
        }
        else {
            errorHandler.flag(token, MISSING_RIGHT_PAREN, this);
        }

        // Parse the statement.
        // The LOOP node adopts the statement subtree as its second child.
        StatementParser statementParser = new StatementParser(this);
        loopNode.addChild(statementParser.parse(token));

        return loopNode;
    }
}
