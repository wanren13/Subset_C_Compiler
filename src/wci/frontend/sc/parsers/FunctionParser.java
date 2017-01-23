package wci.frontend.sc.parsers;

import java.util.*;
import wci.frontend.Token;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.*;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.*;
import static wci.intermediate.symtabimpl.Predefined.*;
import static wci.intermediate.symtabimpl.RoutineCodeImpl.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;

public class FunctionParser extends SCParserTD {

	public FunctionParser(SCParserTD parent) {
		super(parent);
	}

	// the map includes int, float, char and void predefined TypeSpec
	protected static final HashMap<SCTokenType, TypeSpec>
	RETURN_TYPE_MAP = new HashMap<SCTokenType, TypeSpec>();
	static {
		RETURN_TYPE_MAP.putAll(SimpleVarDeclParser.TOKEN_TYPE_MAP);
		RETURN_TYPE_MAP.put(VOID, voidType);
	};

	// the set includes INT, FLOAT, CHAR and VOID token types
	protected static final EnumSet<SCTokenType> FUNCTION_START_SET =
			EnumSet.copyOf(SimpleVarDeclParser.DECLARATION_START_SET);
	static{
		FUNCTION_START_SET.add(VOID);
	};

	protected static final EnumSet<SCTokenType> DECLARATION_FOLLOW_SET =
			EnumSet.of(COMMA, RIGHT_PAREN);

	public boolean parse(Token firstToken, Token secondToken, SymTabEntry parentId)
			throws Exception
	{
		TypeSpec returnType = RETURN_TYPE_MAP.get((SCTokenType) firstToken.getType());

		Definition funcDefn = null;
		SymTabEntry funcId = null;
		Token token = currentToken();
		String funcName = null;
		
		boolean funcRedefine = false;

		if(secondToken.getType() == IDENTIFIER){

			funcName = secondToken.getText().toLowerCase();			
			funcId = symTabStack.lookupLocal(funcName);

			if (funcId == null) {
				funcId = symTabStack.enterLocal(funcName);
				funcDefn = (firstToken.getType() == VOID)?PROCEDURE:FUNCTION;
				funcId.setDefinition(funcDefn);
				funcId.setTypeSpec(returnType);
				funcId.appendLineNumber(secondToken.getLineNumber());
			}
            else if (funcId.getAttribute(ROUTINE_CODE) != FORWARD) {
                errorHandler.flag(token, FUNCTION_REDEFINED, this);
                return funcRedefine;
            }
		}
		else{
			errorHandler.flag(firstToken, MISSING_IDENTIFIER, this);
		}

		ICode iCode = ICodeFactory.createICode();
		funcId.setAttribute(ROUTINE_ICODE, iCode);
		
		// Push the routine's new symbol table onto the stack.
		// if previously forwarded, get the existing table
        if (funcId.getAttribute(ROUTINE_CODE) == FORWARD) {
            SymTab symTab = (SymTab) funcId.getAttribute(ROUTINE_SYMTAB);
            symTabStack.push(symTab);
        }
        // if not forwarded, create new table
        else {
        	SymTab symTab = symTabStack.push();
            funcId.setAttribute(ROUTINE_SYMTAB, symTab);
           	((SymTabImpl)symTab).setFunctionName(funcName);
            if (funcDefn == DefinitionImpl.FUNCTION)
            	((SymTabImpl)symTab).setIsFunction(true);
        }
      
        //add function to list of functions (only if this has not been added already)
        if (funcId.getAttribute(ROUTINE_CODE) != FORWARD) {
        	@SuppressWarnings("unchecked")
			ArrayList<SymTabEntry> subroutines = (ArrayList<SymTabEntry>)
        			parentId.getAttribute(ROUTINE_ROUTINES);
        	subroutines.add(funcId);
        }

        //if the function was previously forwarded, ignore parameters if given
        if (funcId.getAttribute(ROUTINE_CODE) == FORWARD) {
        	if (token.getType() == LEFT_PAREN) {
        		while (token.getType() != RIGHT_PAREN) {
        			token = nextToken();
        		}
        		token = nextToken(); //consume )
        	}
        }
        //function was not forwarded previously, so parse parameters
        else {
			token = nextToken(); // consume (
	
			SimpleVarDeclParser varParser = new SimpleVarDeclParser(this);
	
			ArrayList<SymTabEntry> parms = new ArrayList<SymTabEntry>();
	
			while(SimpleVarDeclParser.DECLARATION_START_SET.contains(token.getType())){
				parms.add(varParser.parse(token));
				token = synchronize(DECLARATION_FOLLOW_SET);			
				if(token.getType() == COMMA)
					token = nextToken();
			}
			
			for (SymTabEntry parmId: parms)
				parmId.setDefinition(VALUE_PARM);
	
			funcId.setAttribute(ROUTINE_PARMS, parms);

			token = currentToken();
	
			if(token.getType() != RIGHT_PAREN)
				errorHandler.flag(token, MISSING_RIGHT_PAREN, this);
	
			token = nextToken(); // consume )
        }
        
        //token should be at { or ;        
		if (token.getType() == SEMICOLON) {
			funcId.setAttribute(ROUTINE_CODE, FORWARD);
		}
		else if (token.getType() == LEFT_BRACE) {
			funcId.setAttribute(ROUTINE_CODE, DECLARED);
			
			CompoundStatementParser compoundstmtParser =
					new CompoundStatementParser(this);

			ICodeNode rootNode = compoundstmtParser.parse(token);
			//TODO: add LABEL node
			iCode.setRoot(rootNode);
		}
		else {
			errorHandler.flag(token, UNEXPECTED_TOKEN, this);
		}

		//pop function's symbol table off stack
		symTabStack.pop();
		
		return funcRedefine;
	}
}