package wci.frontend.sc.parsers;

import java.util.EnumSet;

import static wci.frontend.sc.SCTokenType.*;
import static wci.frontend.sc.SCErrorCode.*;
import wci.frontend.EofToken;
import wci.frontend.Token;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.icodeimpl.ICodeNodeTypeImpl;
import wci.intermediate.symtabimpl.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;

public class StatementParser extends SCParserTD {

	public StatementParser(SCParserTD parent) {
		super(parent);
	}

	// Synchronization set for starting a statement.
	protected static final EnumSet<SCTokenType> STMT_START_SET =
			EnumSet.of(LEFT_BRACE, FOR, SCTokenType.IF, DO, WHILE,
					IDENTIFIER, SEMICOLON, SCTokenType.RETURN);

	// Synchronization set for following a statement.
	protected static final EnumSet<SCTokenType> STMT_FOLLOW_SET =
			EnumSet.of(SEMICOLON, RIGHT_BRACE);

	public ICodeNode parse(Token token)
			throws Exception
	{
		ICodeNode statementNode = null;

		switch ((SCTokenType) token.getType()) {

			case LEFT_BRACE: {
				CompoundStatementParser compoundParser =
						new CompoundStatementParser(this);
				statementNode = compoundParser.parse(token);
				break;
			}
	
			case IDENTIFIER: {
				String name = token.getText().toLowerCase();
				SymTabEntry id = symTabStack.lookup(name);
				Definition idDefn = (id == null) ? UNDEFINED : id.getDefinition();

				//handle crashes because of syntax errors leading to undefined variables
				if (idDefn == null) idDefn = UNDEFINED;
				/*			
				AssignmentStatementParser assignmentParser =
						                          new AssignmentStatementParser(this);
				                      statementNode = assignmentParser.parse(token);
				 */
				switch ((DefinitionImpl)idDefn) {
				    case VARIABLE:
                    case VALUE_PARM:
                    case VAR_PARM:
                    case UNDEFINED: {
						AssignmentStatementParser assignmentParser =
								new AssignmentStatementParser(this);
						statementNode = assignmentParser.parse(token);
						break;
					}
		
					case PROCEDURE: {
						CallParser callParser = new CallParser(this);
						statementNode = callParser.parse(token);
						break;
					}
					
					default: {
						errorHandler.flag(token, UNEXPECTED_TOKEN, this);
						token = nextToken();  // consume identifier
					}
	
				}
				break;
	
			}
			//treat "return X" like "functionname = X"
			case RETURN: {
	        	SymTab symTab = symTabStack.getLocalSymTab();
	        	boolean isfunc = ((SymTabImpl)symTab).isFunction();
	        	if (isfunc) {
	        		AssignmentStatementParser assignmentParser =
	        				new AssignmentStatementParser(this);
	        		ICodeNode assignNode =
	        				assignmentParser.parseReturn(token);
	        		setLineNumber(assignNode, token);

	        		statementNode = ICodeFactory.createICodeNode(COMPOUND);
	        		statementNode.addChild(assignNode);
	        		
	        		ICodeNode returnNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.RETURN);
	        		setLineNumber(returnNode, token);
	        		statementNode.addChild(returnNode);	  
	        	}
	        	else {     		
					statementNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.RETURN);
					token = nextToken();
					EnumSet<SCTokenType> SEMI_SET = EnumSet.of(SEMICOLON);
					token = synchronize(SEMI_SET);
	        	}
	        	
        		break;
			}
	
			case WHILE: {
				WhileStatementParser whileParser =
						new WhileStatementParser(this);
				statementNode = whileParser.parse(token);
				break;
			}
	
			case IF: {
				IfStatementParser ifParser = new IfStatementParser(this);
				statementNode = ifParser.parse(token);
				break;
			}
	
			default: {
				statementNode = ICodeFactory.createICodeNode(NO_OP);
				break;
			}
		}

		// Set the current line number as an attribute.
		setLineNumber(statementNode, token);

		return statementNode;
	}

	protected void setLineNumber(ICodeNode node, Token token)
	{
		if (node != null) {
			node.setAttribute(LINE, token.getLineNumber());
		}
	}

	protected void parseList(Token token, ICodeNode parentNode,
			SCTokenType terminator,
			SCErrorCode errorCode)
					throws Exception
					{
		// Synchronization set for the terminator.
		EnumSet<SCTokenType> terminatorSet = STMT_START_SET.clone();
		terminatorSet.add(terminator);

		// Loop to parse each statement until the } token
		// or the end of the source file.
		while (!(token instanceof EofToken) &&
				(token.getType() != terminator)) {

			// Parse a statement.  The parent node adopts the statement node.
			ICodeNode statementNode = parse(token);
			parentNode.addChild(statementNode);

			// Synchronize at the start of the next statement
			// or at the terminator.
			token = synchronize(terminatorSet);

			if (token.getType() == SEMICOLON) {
				token = nextToken();  // consume ;
			}
		}

		// Look for the terminator token.
		if (token.getType() == terminator) {
			token = nextToken();  // consume the terminator token
		}
		else {
			errorHandler.flag(token, errorCode, this);
		}
					}

}