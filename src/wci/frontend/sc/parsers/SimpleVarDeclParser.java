package wci.frontend.sc.parsers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import wci.frontend.*;
import wci.frontend.sc.*;
import wci.intermediate.*;
import static wci.intermediate.symtabimpl.Predefined.*;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.VARIABLE;

public class SimpleVarDeclParser extends SCParserTD
{
	@SuppressWarnings("unused")
	private Definition definition;

	protected static final HashMap<SCTokenType, TypeSpec>
	TOKEN_TYPE_MAP = new HashMap<SCTokenType, TypeSpec>();
	static {
		TOKEN_TYPE_MAP.put(INT, integerType);
		TOKEN_TYPE_MAP.put(FLOAT, realType);
		TOKEN_TYPE_MAP.put(CHAR, charType);
	};


	/**
	 * Constructor.
	 * @param parent the parent parser.
	 */
	public SimpleVarDeclParser(SCParserTD parent)
	{
		super(parent);
	}

	protected void setDefinition(Definition definition)
	{
		this.definition = definition;
	}

	protected static final EnumSet<SCTokenType> DECLARATION_START_SET =
			EnumSet.of(INT, FLOAT, CHAR);

	/**
	 * Parse declarations.
	 * To be overridden by the specialized declarations parser subclasses.
	 * @param token the initial token.
	 * @throws Exception if an error occurred.
	 */
	public SymTabEntry parse(Token token)
			throws Exception
	{
		SymTabEntry id = null;
		if (token.getType() == INT || token.getType() == FLOAT ||
				token.getType() == CHAR) {

			//look up type in symbol table
			Token typeToken = token;

			token = nextToken();  // consume INT, FLOAT or CHAR

			//get variable name
			id = parseIdentifier(token, false);

			//assign type spec to variable
			if (id != null) {
				id.setTypeSpec(TOKEN_TYPE_MAP.get((SCTokenType)typeToken.getType()));
			}
		}
		return id;
	}


	public ArrayList<ICodeNode> parse(Token typetoken, Token idtoken)
			throws Exception
	{		
		AssignmentStatementParser assignParser = new AssignmentStatementParser(this);
		ArrayList<ICodeNode> nodeList = new ArrayList<ICodeNode>();
		ICodeNode node = null;
		SymTabEntry id = null;
		Token lasttoken = currentToken();

		if (DECLARATION_START_SET.contains(typetoken.getType())) {

			//get variable name
			id = parseIdentifier(idtoken, true);			

			//assign type spec to variable
			if (id != null) {
				id.setTypeSpec(TOKEN_TYPE_MAP.get((SCTokenType)typetoken.getType()));
			}			
		}

		if(lasttoken.getType() == EQUALS) {
			lasttoken = nextToken();
			node = assignParser.parse(idtoken, lasttoken);
			nodeList.add(node);
			lasttoken = currentToken();
		}

		if(lasttoken.getType() == COMMA)
			lasttoken = nextToken();  // consume ,

		while (lasttoken.getType() != SEMICOLON) {
			idtoken = currentToken(); // get identifier
			id = parseIdentifier(idtoken, false);
			if (id != null)
				id.setTypeSpec(TOKEN_TYPE_MAP.get((SCTokenType)typetoken.getType()));

			lasttoken = currentToken(); // = or ,

			if (lasttoken.getType() == EQUALS) {
				lasttoken = nextToken(); // consume =

				node = assignParser.parse(idtoken, lasttoken);
				nodeList.add(node);
			}

			lasttoken = synchronize(EnumSet.of(COMMA, SEMICOLON));

			if(lasttoken.getType() == COMMA)
				lasttoken = nextToken();

			else if (lasttoken.getType() != SEMICOLON)
				errorHandler.flag(lasttoken, UNEXPECTED_TOKEN, this);		

		}

		return nodeList;
	}
	

	private SymTabEntry parseIdentifier(Token token, boolean skip)
			throws Exception
	{
		SymTabEntry id = null;

		if (token.getType() == IDENTIFIER) {
			String name = token.getText().toLowerCase();
			id = symTabStack.lookupLocal(name);

			// Enter a new identifier into the symbol table.
			if (id == null) {
				id = symTabStack.enterLocal(name);
				id.setDefinition(VARIABLE);
				id.appendLineNumber(token.getLineNumber());
				
				// Set its slot number in the local variables array.
                int slot = id.getSymTab().nextSlotNumber();
                id.setAttribute(SLOT, slot);
			}
			else {
				errorHandler.flag(token, IDENTIFIER_REDEFINED, this);
			}

			if(!skip)
				token = nextToken();   // consume the identifier token
		}
		else {
			errorHandler.flag(token, MISSING_IDENTIFIER, this);
		}

		return id;
	}
}
