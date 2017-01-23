package wci.frontend.sc.parsers;

import java.util.ArrayList;
import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.*;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;

public class ProgramParser extends SCParserTD {

	public ProgramParser(SCParserTD parent) {
		super(parent);
	}

	// the set includes INT, FLOAT, CHAR and VOID token types
	protected static final EnumSet<SCTokenType> TYPE_START_SET =
			EnumSet.copyOf(SimpleVarDeclParser.DECLARATION_START_SET);
	static{
		TYPE_START_SET.add(VOID);
	};

	// Synchronization set for following a variable declaration
	protected static final EnumSet<SCTokenType> DECL_FOLLOW_SET =
			EnumSet.of(SEMICOLON);

	public void parse(Token token)
			throws Exception
			{
		SimpleVarDeclParser varParser = new SimpleVarDeclParser(this);
		FunctionParser funcParser = new FunctionParser(this);

		Token firstToken, secondToken = null, thirdToken = null;				
		//generate dummy program name
		SymTabEntry programId = symTabStack.enterLocal(programName);
		programId.setDefinition(DefinitionImpl.PROGRAM);
		//create initial stub code (which will eventually call "main")
		ICode rootNode = ICodeFactory.createICode();
        programId.setAttribute(ROUTINE_ICODE, rootNode);
        //create empty list of functions
        programId.setAttribute(ROUTINE_ROUTINES, new ArrayList<SymTabEntry>());
        //create new "global" symbol table level (and set it's stored function name)
        SymTab symTab = symTabStack.push();
        ((SymTabImpl)symTab).setFunctionName(programName);
        programId.setAttribute(ROUTINE_SYMTAB, symTab);
        //assign this stub as the program
        symTabStack.setProgramId(programId);
        
        ArrayList<ICodeNode> nodeList = new ArrayList<ICodeNode>();
        
		firstToken = token;
		
		boolean funcRedefine = false;

		//check for type identifiers
		while (TYPE_START_SET.contains(firstToken.getType()) && !(firstToken instanceof EofToken) 
				&& !funcRedefine) {			
			
			// get identifier token
			secondToken = nextToken();
			// get the token after identifier
			thirdToken = nextToken();
			
			// check type token
			// function declaration
			if(firstToken.getType() == VOID)
				funcRedefine = funcParser.parse(firstToken, secondToken, programId);

			// function declaration or simple variable declaration
			else{
				// variable declaration
				if(thirdToken.getType() == SEMICOLON || thirdToken.getType() == COMMA
						|| thirdToken.getType() == EQUALS) {
					//returns list of "ASSIGN" nodes for global variable assignments
					ArrayList<ICodeNode> globalVars = varParser.parse(firstToken, secondToken);

					//if "main" is not defined yet, add "ASSIGN" nodes to nodeList
					SymTabEntry mainId = symTabStack.lookupLocal("main");
					if (mainId == null) {
						nodeList.addAll(globalVars);
					}
				}

				// function declaration
				else if(thirdToken.getType() == LEFT_PAREN){
					funcRedefine = funcParser.parse(firstToken, secondToken, programId);
				}
				
			}
			
			firstToken = currentToken();

			if (firstToken.getType() == SEMICOLON)
				firstToken = nextToken();	// consume ;
		}
		
		//lookup "main" function
		SymTabEntry mainFunc = symTabStack.lookupLocal("main");
		//if main does not exist
		if (mainFunc == null) {
			errorHandler.flag(firstToken, NO_MAIN_FUNCTION, this);
		}
		//if main is not a procedure (void), or it does not have 0 parameters
		else if (mainFunc.getDefinition() != DefinitionImpl.PROCEDURE ||
				!((ArrayList<SymTabEntry>)mainFunc.getAttribute(ROUTINE_PARMS)).isEmpty()) {
			errorHandler.flag(firstToken, MAIN_FUNCTION_WRONG, this);
		}
		else {
			//make CALL node to call "main"
			ICodeNode callNode = ICodeFactory.createICodeNode(CALL);
			callNode.setAttribute(ID, mainFunc);
			ArrayList<Integer> lines = mainFunc.getLineNumbers();
			callNode.setAttribute(LINE, lines.get(0).intValue());
			callNode.setTypeSpec(mainFunc.getTypeSpec());

			//make COMPOUND node to surround CALL
			ICodeNode compoundNode = ICodeFactory.createICodeNode(COMPOUND);

			//add stored ASSIGN nodes as children to COMPOUND node
			for (ICodeNode node : nodeList)
				compoundNode.addChild(node);
			
			//add CALL "main" node as child to COMPOUND node 
			compoundNode.addChild(callNode);
			
			//set COMPOUND node to be the root of the "program" code
			rootNode.setRoot(compoundNode);
		}
		
		//pop "global" symbol table off stack
		symTabStack.pop();
	}
}
