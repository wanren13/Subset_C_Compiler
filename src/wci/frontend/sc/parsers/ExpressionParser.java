package wci.frontend.sc.parsers;

import java.util.EnumSet;
import java.util.HashMap;

import wci.frontend.*;
import wci.frontend.sc.*;
import wci.intermediate.icodeimpl.*;
import wci.intermediate.symtabimpl.*;
import wci.intermediate.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;

public class ExpressionParser extends StatementParser {
    
    // Synchronization set for starting an expression.
    static final EnumSet<SCTokenType> EXPR_START_SET =
        EnumSet.of(PLUS, MINUS, IDENTIFIER, INTEGER, REAL, STRING,
        		CHARACTER, SCTokenType.NOT, LEFT_PAREN);

    public ExpressionParser(SCParserTD parent)
    {
        super(parent);
    }

    public ICodeNode parse(Token token)
        throws Exception
    {
        return parseExpression(token);
    }

    // Set of relational operators.
    private static final EnumSet<SCTokenType> REL_OPS =
        EnumSet.of(EQUALS_EQUALS, NOT_EQUALS, LESS_THAN, LESS_EQUALS,
                   GREATER_THAN, GREATER_EQUALS);


    // Map relational operator tokens to node types.
    private static final HashMap<SCTokenType, ICodeNodeType>
        REL_OPS_MAP = new HashMap<SCTokenType, ICodeNodeType>();
    static {
        REL_OPS_MAP.put(EQUALS_EQUALS, EQ);
        REL_OPS_MAP.put(NOT_EQUALS, NE);
        REL_OPS_MAP.put(LESS_THAN, LT);
        REL_OPS_MAP.put(LESS_EQUALS, LE);
        REL_OPS_MAP.put(GREATER_THAN, GT);
        REL_OPS_MAP.put(GREATER_EQUALS, GE);
    };
    
    private ICodeNode parseExpression(Token token)
            throws Exception
    {
    	// Parse a simple expression and make the root of its tree
        // the root node.
        ICodeNode rootNode = parseSimpleExpression(token);

        token = currentToken();
        TokenType tokenType = token.getType();

        // Look for a relational operator.
        if (REL_OPS.contains(tokenType)) {

            // Create a new operator node and adopt the current tree
            // as its first child.
            ICodeNodeType nodeType = REL_OPS_MAP.get(tokenType);
            ICodeNode opNode = ICodeFactory.createICodeNode(nodeType);
            opNode.addChild(rootNode);

            token = nextToken();  // consume the operator

            // Parse the second simple expression.  The operator node adopts
            // the simple expression's tree as its second child.
            opNode.addChild(parseSimpleExpression(token));

            // The operator node becomes the new root node.
            rootNode = opNode;
            rootNode.setTypeSpec(Predefined.booleanType);        
         }

        return rootNode;
    }
    
    // Set of additive operators.
    private static final EnumSet<SCTokenType> ADD_OPS =
        EnumSet.of(PLUS, MINUS, LOGICAL_OR);

    // Map additive operator tokens to node types.
    private static final HashMap<SCTokenType, ICodeNodeTypeImpl>
        ADD_OPS_OPS_MAP = new HashMap<SCTokenType, ICodeNodeTypeImpl>();
    static {
        ADD_OPS_OPS_MAP.put(PLUS, ADD);
        ADD_OPS_OPS_MAP.put(MINUS, SUBTRACT);
        ADD_OPS_OPS_MAP.put(LOGICAL_OR, ICodeNodeTypeImpl.OR);
    };

    private ICodeNode parseSimpleExpression(Token token)
            throws Exception
    {
    	TokenType signType = null;  // type of leading sign (if any)

        // Look for a leading + or - sign.
        TokenType tokenType = token.getType();
        if ((tokenType == PLUS) || (tokenType == MINUS)) {
            signType = tokenType;
            token = nextToken();  // consume the + or -
        }

        // Parse a term and make the root of its tree the root node.
        ICodeNode rootNode = parseTerm(token);
        TypeSpec resultType = rootNode != null ? rootNode.getTypeSpec()
                : Predefined.undefinedType;


        // Was there a leading - sign?
        if (signType == MINUS) {

            // Create a NEGATE node and adopt the current tree
            // as its child. The NEGATE node becomes the new root node.
            ICodeNode negateNode = ICodeFactory.createICodeNode(NEGATE);
            negateNode.addChild(rootNode);
            rootNode = negateNode;
        }

        token = currentToken();
        tokenType = token.getType();

        // Loop over additive operators.
        while (ADD_OPS.contains(tokenType)) {

            // Create a new operator node and adopt the current tree
            // as its first child.
            ICodeNodeType nodeType = ADD_OPS_OPS_MAP.get(tokenType);
            ICodeNode opNode = ICodeFactory.createICodeNode(nodeType);
            opNode.addChild(rootNode);

            token = nextToken();  // consume the operator

            // Parse another term.  The operator node adopts
            // the term's tree as its second child.
            opNode.addChild(parseTerm(token));

            // The operator node becomes the new root node.
            rootNode = opNode;
            if(tokenType == LOGICAL_OR)
            	resultType = Predefined.booleanType;
            
            rootNode.setTypeSpec(resultType);

            token = currentToken();
            tokenType = token.getType();
        }

        return rootNode;
    }

    // Set of multiplicative operators.
    private static final EnumSet<SCTokenType> MULT_OPS =
        EnumSet.of(STAR, SLASH, PERCENT, LOGICAL_AND);

    // Map multiplicative operator tokens to node types.
    private static final HashMap<SCTokenType, ICodeNodeType>
        MULT_OPS_OPS_MAP = new HashMap<SCTokenType, ICodeNodeType>();
    static {
        MULT_OPS_OPS_MAP.put(STAR, MULTIPLY);
        MULT_OPS_OPS_MAP.put(SLASH, FLOAT_DIVIDE);
        MULT_OPS_OPS_MAP.put(PERCENT, ICodeNodeTypeImpl.MOD);
        MULT_OPS_OPS_MAP.put(LOGICAL_AND, ICodeNodeTypeImpl.AND);
    };

    private ICodeNode parseTerm(Token token)
            throws Exception
    {
    	// Parse a factor and make its node the root node.
        ICodeNode rootNode = parseFactor(token);
        TypeSpec resultType = rootNode != null ? rootNode.getTypeSpec()
                : Predefined.undefinedType;

        token = currentToken();
        TokenType tokenType = token.getType();

        // Loop over multiplicative operators.
        while (MULT_OPS.contains(tokenType)) {

            // Create a new operator node and adopt the current tree
            // as its first child.
            ICodeNodeType nodeType = MULT_OPS_OPS_MAP.get(tokenType);
            ICodeNode opNode = ICodeFactory.createICodeNode(nodeType);
            opNode.addChild(rootNode);

            token = nextToken();  // consume the operator

            // Parse another factor.  The operator node adopts
            // the term's tree as its second child.
            opNode.addChild(parseFactor(token));

            // The operator node becomes the new root node.
            rootNode = opNode;
            if(tokenType == LOGICAL_AND)
            	resultType = Predefined.booleanType;
            rootNode.setTypeSpec(resultType);

            token = currentToken();
            tokenType = token.getType();
        }

        return rootNode;
    }

    private ICodeNode parseFactor(Token token)
            throws Exception
    {
    	TokenType tokenType = token.getType();
        ICodeNode rootNode = null;

        switch ((SCTokenType) tokenType) {

            case IDENTIFIER: {
                // Look up the identifier in the symbol table stack.
                // Flag the identifier as undefined if it's not found.
                String name = token.getText().toLowerCase();
                SymTabEntry id = symTabStack.lookup(name);
                if (id == null) {
                    errorHandler.flag(token, IDENTIFIER_UNDEFINED, this);
                    id = symTabStack.enterLocal(name);
                }

                Definition defnCode = id.getDefinition();

                if (defnCode != null)
                switch ((DefinitionImpl) defnCode) {
                    case FUNCTION: {
                        CallParser callParser = new CallParser(this);
                        rootNode = callParser.parse(token);
                        break;
                    }

                    default: {
                        rootNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.VARIABLE);
                        rootNode.setAttribute(ID, id);
                        rootNode.setTypeSpec(id.getTypeSpec());
                        id.appendLineNumber(token.getLineNumber());
                        break;
                    }
                }

                token = nextToken();  // consume the identifier
                break;
            }

            case INTEGER: {
                // Create an INTEGER_CONSTANT node as the root node.
                rootNode = ICodeFactory.createICodeNode(INTEGER_CONSTANT);
                rootNode.setAttribute(VALUE, token.getValue());
                rootNode.setTypeSpec(Predefined.integerType);

                token = nextToken();  // consume the number
                break;
            }

            case REAL: {
                // Create an REAL_CONSTANT node as the root node.
                rootNode = ICodeFactory.createICodeNode(REAL_CONSTANT);
                rootNode.setAttribute(VALUE, token.getValue());
                rootNode.setTypeSpec(Predefined.realType);

                token = nextToken();  // consume the number
                break;
            }

            case STRING: {
                String value = (String) token.getValue();

                // Create a STRING_CONSTANT node as the root node.
                rootNode = ICodeFactory.createICodeNode(STRING_CONSTANT);
                rootNode.setAttribute(VALUE, value);

                token = nextToken();  // consume the string
                break;
            }
            
            case CHARACTER: {
            	char value = (char) token.getValue();
            	
            	rootNode = ICodeFactory.createICodeNode(INTEGER_CONSTANT);
            	rootNode.setAttribute(VALUE, (int)value);
            	rootNode.setTypeSpec(Predefined.charType);
            	
            	token = nextToken();
            	break;
            }

            case NOT: {
                token = nextToken();  // consume the NOT

                // Create a NOT node as the root node.
                rootNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.NOT);

                // Parse the factor.  The NOT node adopts the
                // factor node as its child.
                rootNode.addChild(parseFactor(token));
                rootNode.setTypeSpec(Predefined.booleanType);

                break;
            }

            case LEFT_PAREN: {
                token = nextToken();      // consume the (

                // Parse an expression and make its node the root node.
                rootNode = parseExpression(token);

                // Look for the matching ) token.
                token = currentToken();
                if (token.getType() == RIGHT_PAREN) {
                    token = nextToken();  // consume the )
                }
                else {
                    errorHandler.flag(token, MISSING_RIGHT_PAREN, this);
                }

                break;
            }

            default: {
                errorHandler.flag(token, UNEXPECTED_TOKEN, this);
                break;
            }
        }

        return rootNode;
    }
}
