package wci.frontend.sc.parsers;

import static wci.frontend.sc.SCErrorCode.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.COMPOUND;

import java.util.ArrayList;
import java.util.EnumSet;

import wci.frontend.Token;
import wci.frontend.sc.SCParserTD;
import wci.frontend.sc.SCTokenType;
import wci.frontend.sc.parsers.StatementParser;
import wci.intermediate.ICodeFactory;
import wci.intermediate.ICodeNode;

public class CompoundStatementParser extends StatementParser {

    public CompoundStatementParser(SCParserTD parent)
    {
        super(parent);
    }

    // Synchronization set for following a variable declaration
    protected static final EnumSet<SCTokenType> DECL_FOLLOW_SET =
        EnumSet.of(SEMICOLON, RIGHT_BRACE);
    
    private static final EnumSet<SCTokenType> DECLARATION_START_SET =
			EnumSet.of(INT, FLOAT, CHAR);
    
   
    /**
     * Parse a compound statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token)
        throws Exception
    {
        // consume {
        	token = nextToken();
        	Token secondtoken;

        // Create the COMPOUND node.
        ICodeNode compoundNode = ICodeFactory.createICodeNode(COMPOUND);
        
        ArrayList<ICodeNode> nodeList = new ArrayList<ICodeNode>();
        
        //check for simple variable declarations
        while (DECLARATION_START_SET.contains((SCTokenType)token.getType())) {
        	secondtoken = nextToken();
        	
        	SimpleVarDeclParser varParser = new SimpleVarDeclParser(this);
        	nextToken();
        	nodeList.addAll(varParser.parse(token, secondtoken));
         	
        	//sync to ;
        	token = synchronize(DECL_FOLLOW_SET);
        	
        	if(token.getType() != SEMICOLON)
        		errorHandler.flag(token, MISSING_SEMICOLON, this);
        	
        	//consume ;
        	token = nextToken();        	
        }
        
        for(ICodeNode node : nodeList)
        	compoundNode.addChild(node);
        
        // Parse the statement list terminated by the } token.
        StatementParser statementParser = new StatementParser(this);
        statementParser.parseList(token, compoundNode, RIGHT_BRACE, MISSING_RIGHT_BRACE);

        return compoundNode;
    }
}

