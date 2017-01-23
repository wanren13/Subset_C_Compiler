package wci.frontend.sc.parsers;

import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;


public class AssignmentStatementParser extends StatementParser
{
	private boolean isReturn = false;
	
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public AssignmentStatementParser(SCParserTD parent)
    {
        super(parent);
    }

    // Synchronization set for the := token.
    private static final EnumSet<SCTokenType> EQUALS_SET =
        ExpressionParser.EXPR_START_SET.clone();
    static {
        EQUALS_SET.add(EQUALS);
        EQUALS_SET.addAll(StatementParser.STMT_FOLLOW_SET);
    }

    /**
     * Parse an assignment statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token)
        throws Exception
    {
        // Create the ASSIGN node.
        ICodeNode assignNode = ICodeFactory.createICodeNode(ASSIGN);
        
        SymTabEntry targetId = null;

        // Look up the target identifier in the symbol table stack.
        // Enter the identifier into the table if it's not found.
        String targetName = token.getText().toLowerCase();

        //if this is a return statement, lookup function name and then
        //look it up in symbol table, otherwise use token text
        if (targetName.equals("return") || isReturn) {
        	SymTab symTab = symTabStack.getLocalSymTab();
        	targetName = ((SymTabImpl)symTab).getFunctionName();
        	if (targetName == null)
        		targetName = "error_unknown_function";
        }

        targetId = symTabStack.lookup(targetName);

        if (targetId == null) {
        	errorHandler.flag(token, IDENTIFIER_UNDEFINED, this);
        	//		    targetId = symTabStack.enterLocal(targetName);
        }
        else {
        	targetId.appendLineNumber(token.getLineNumber());

        	token = nextToken();  // consume the identifier token

        	// Create the variable node and set its name attribute.
        	ICodeNode variableNode = ICodeFactory.createICodeNode(VARIABLE);
        	variableNode.setAttribute(ID, targetId);
        	variableNode.setTypeSpec(targetId.getTypeSpec()); 

        	// The ASSIGN node adopts the variable node as its first child.
        	assignNode.addChild(variableNode);
        	
        	// Set ASSIGN node type spec
        	assignNode.setTypeSpec(targetId.getTypeSpec()!=null?
        		targetId.getTypeSpec():Predefined.undefinedType);
        }

        // Synchronize on the = token.
        token = synchronize(EQUALS_SET);
        if (token.getType() == EQUALS) {
        	token = nextToken();  // consume the =
        }
        else {
        	//don't error if this is a "return" statement
        	if (isReturn == false)
        		errorHandler.flag(token, MISSING_EQUALS, this);
        }

        // Parse the expression.  The ASSIGN node adopts the expression's
        // node as its second child.
        ExpressionParser expressionParser = new ExpressionParser(this);
        assignNode.addChild(expressionParser.parse(token));
        
        return assignNode;
    }    
    
    
    /**
     * Parse an assignment statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token identifier, Token value)
        throws Exception
    {
        // Create the ASSIGN node.
        ICodeNode assignNode = ICodeFactory.createICodeNode(ASSIGN);
        setLineNumber(assignNode, identifier);
        
        SymTabEntry targetId = null;

        // Look up the target identifier in the symbol table stack.
        // Enter the identifier into the table if it's not found.
        String targetName = identifier.getText().toLowerCase();

        //if this is a return statement, lookup function name and then
        //look it up in symbol table, otherwise use token text
        if (targetName.equals("return") || isReturn) {
        	SymTab symTab = symTabStack.getLocalSymTab();
        	targetName = ((SymTabImpl)symTab).getFunctionName();
        	if (targetName == null)
        		targetName = "error_unknown_function";
        }

        targetId = symTabStack.lookup(targetName);

        if (targetId == null) {
		    targetId = symTabStack.enterLocal(targetName);
		}
		targetId.appendLineNumber(identifier.getLineNumber());

        // Create the variable node and set its name attribute.
        ICodeNode variableNode = ICodeFactory.createICodeNode(VARIABLE);
        variableNode.setAttribute(ID, targetId);
        variableNode.setTypeSpec(targetId.getTypeSpec());

        // The ASSIGN node adopts the variable node as its first child.
        assignNode.addChild(variableNode);
        
        // Set ASSIGN node type spec
        assignNode.setTypeSpec(targetId.getTypeSpec()!=null?
        		targetId.getTypeSpec():Predefined.undefinedType);

        // Parse the expression.  The ASSIGN node adopts the expression's
        // node as its second child.
        ExpressionParser expressionParser = new ExpressionParser(this);
        assignNode.addChild(expressionParser.parse(value));
        
        return assignNode;
    }  
    

    /**
     * Parse an assignment to a function name.
     * @param token Token
     * @return ICodeNode
     * @throws Exception
     */
    public ICodeNode parseReturn(Token token)
        throws Exception
    {
        isReturn = true;
        return parse(token);
    }
}
