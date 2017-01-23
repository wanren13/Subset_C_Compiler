package wci.frontend.sc.parsers;

import java.util.ArrayList;
import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.sc.*;
import wci.intermediate.*;
import wci.intermediate.icodeimpl.ICodeNodeTypeImpl;
import wci.intermediate.symtabimpl.Predefined;
import wci.intermediate.symtabimpl.RoutineCodeImpl;

import static wci.frontend.sc.SCErrorCode.*;
import static wci.frontend.sc.SCTokenType.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.symtabimpl.RoutineCodeImpl.*;
import static wci.intermediate.typeimpl.TypeFormImpl.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;


public class CallParser extends StatementParser
{
	/**
	 * Constructor.
	 * @param parent the parent parser.
	 */
	public CallParser(SCParserTD parent)
	{
		super(parent);
	}

	/**
	 * Parse a call to a declared procedure or function.
	 * @param token the initial token.
	 * @return the root node of the generated parse tree.
	 * @throws Exception if an error occurred.
	 */
	public ICodeNode parse(Token token)
			throws Exception
			{
		SymTabEntry pfId = symTabStack.lookup(token.getText().toLowerCase());
		RoutineCode routineCode = (RoutineCode) pfId.getAttribute(ROUTINE_CODE);
		return (
				  (routineCode == DECLARED) ||
				  (routineCode == FORWARD)
				)? parseDeclared(token): parseStandard(token);

			}

	// Synchronization set for the , token.
	private static final EnumSet<SCTokenType> COMMA_SET =
			ExpressionParser.EXPR_START_SET.clone();
	static {
		COMMA_SET.add(COMMA);
		COMMA_SET.add(RIGHT_PAREN);
	};

	/**
	 * Parse the actual parameters of a procedure or function call.
	 * @param token the current token.
	 * @param pfId the symbol table entry of the procedure or function name.
	 * @param isDeclared true if parsing actual parms of a declared routine.
	 * @return the PARAMETERS node, or null if there are no actual parameters.
	 * @throws Exception if an error occurred.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	protected ICodeNode parseActualParameters(Token token, SymTabEntry pfId,
			boolean isDeclared,
			boolean isReadReadln,
			boolean isWriteWriteln)
					throws Exception
	{
		ExpressionParser expressionParser = new ExpressionParser(this);
		ICodeNode parmsNode = ICodeFactory.createICodeNode(PARAMETERS);
		ArrayList<SymTabEntry> formalParms = null;
		int parmCount = 0;
		int parmIndex = -1;

		if (isDeclared) {
			formalParms =
					(ArrayList<SymTabEntry>) pfId.getAttribute(ROUTINE_PARMS);
			parmCount = formalParms != null ? formalParms.size() : 0;
		}

		if (token.getType() != LEFT_PAREN) {         
			errorHandler.flag(token, MISSING_LEFT_PAREN, this);
			return null;
		}

		token = nextToken();  // consume opening (

		// Loop to parse each actual parameter.
		while (token.getType() != RIGHT_PAREN) {
			ICodeNode actualNode = expressionParser.parse(token);

			// Declared procedure or function: Check the number of actual
			// parameters, and check each actual parameter against the
			// corresponding formal parameter.
			if (isDeclared) {
				if (++parmIndex < parmCount) {
					SymTabEntry formalId = formalParms.get(parmIndex);
				}
				else if (parmIndex == parmCount) {
					errorHandler.flag(token, WRONG_NUMBER_OF_PARMS, this);
				}

			}
			
			// read or readln: Each actual parameter must be a variable that is
            //                 a scalar, boolean, or subrange of integer.
            else if (isReadReadln) {
                TypeSpec type = actualNode.getTypeSpec();
                TypeForm form = type.getForm();

                if (! (   (actualNode.getType() == ICodeNodeTypeImpl.VARIABLE)
                       && ( (form == SCALAR) ||
                            (type == Predefined.booleanType) ||
                            ( (form == SUBRANGE) &&
                              (type.baseType() == Predefined.integerType) ) )
                      )
                   )
                {
                    errorHandler.flag(token, INVALID_PARM, this);
                }
            }

            // write or writeln: The type of each actual parameter must be a
            // scalar, boolean, or a Pascal string. Parse any field width and
            // precision.
            else if (isWriteWriteln) {

            	// Create a WRITE_PARM node which adopts the expression node.
            	ICodeNode exprNode = actualNode;
            	actualNode = ICodeFactory.createICodeNode(WRITE_PARM);
            	actualNode.addChild(exprNode);

            }

			parmsNode.addChild(actualNode);
			token = synchronize(COMMA_SET);
			TokenType tokenType = token.getType();

			// Look for the comma.
			if (tokenType == COMMA) {
				token = nextToken();  // consume ,
			}
			else if (ExpressionParser.EXPR_START_SET.contains(tokenType)) {
				errorHandler.flag(token, MISSING_COMMA, this);
			}
			else if (tokenType != RIGHT_PAREN) {
				token = synchronize(ExpressionParser.EXPR_START_SET);
			}
		}

		token = nextToken();  // consume closing )

		if (isDeclared && (parmIndex != parmCount-1))
		{
			errorHandler.flag(token, WRONG_NUMBER_OF_PARMS, this);
		}

		return parmsNode;
	}

	public ICodeNode parseDeclared(Token token)
			throws Exception
	{
		// Create the CALL node.
		ICodeNode callNode = ICodeFactory.createICodeNode(CALL);
		SymTabEntry pfId = symTabStack.lookup(token.getText().toLowerCase());
		callNode.setAttribute(ID, pfId);
		callNode.setTypeSpec(pfId.getTypeSpec());

		token = nextToken();  // consume procedure or function identifier

		ICodeNode parmsNode = parseActualParameters(token, pfId,
				true, false, false);

		callNode.addChild(parmsNode);
		return callNode;
	}

	public ICodeNode parseStandard(Token token)
			throws Exception
	{
		ICodeNode callNode = ICodeFactory.createICodeNode(CALL);
		SymTabEntry pfId = symTabStack.lookup(token.getText().toLowerCase());
		RoutineCode routineCode = (RoutineCode) pfId.getAttribute(ROUTINE_CODE);
		callNode.setAttribute(ID, pfId);

		token = nextToken(); // consume procedure or function identifier

		switch ((RoutineCodeImpl) routineCode) {
		case READ:
		case READLN:  return parseReadReadln(token, callNode, pfId);

		case WRITE:
		case WRITELN: 
		case PRINTF:  return parseWriteWriteln(token, callNode, pfId);

		case EOF:
		case EOLN:    return parseEofEoln(token, callNode, pfId);

		case ABS:
		case SQR:     return parseAbsSqr(token, callNode, pfId);

		case ARCTAN:
		case COS:
		case EXP:
		case LN:
		case SIN:
		case SQRT:    return parseArctanCosExpLnSinSqrt(token, callNode,
				pfId);

		case PRED:
		case SUCC:    return parsePredSucc(token, callNode, pfId);

		case CHR:     return parseChr(token, callNode, pfId);
		case ODD:     return parseOdd(token, callNode, pfId);
		case ORD:     return parseOrd(token, callNode, pfId);

		case ROUND:
		case TRUNC:   return parseRoundTrunc(token, callNode, pfId);

		default:      return null;  // should never get here
		}
	}

	/**
	 * Parse a call to read or readln.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseReadReadln(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, true, false);
		callNode.addChild(parmsNode);

		// Read must have parameters.
		if ((pfId == Predefined.readId) &&
				(callNode.getChildren().size() == 0))
		{
			errorHandler.flag(token, WRONG_NUMBER_OF_PARMS, this);
		}

		return callNode;
					}

	/**
	 * Parse a call to write or writeln.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseWriteWriteln(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, true);
		callNode.addChild(parmsNode);

		// Write must have parameters.
		if ((pfId == Predefined.writeId) &&
				(callNode.getChildren().size() == 0))
		{
			errorHandler.flag(token, WRONG_NUMBER_OF_PARMS, this);
		}

		return callNode;
					}

	/**
	 * Parse a call to eof or eoln.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseEofEoln(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be no actual parameters.
		if (checkParmCount(token, parmsNode, 0)) {
			callNode.setTypeSpec(Predefined.booleanType);
		}

		return callNode;
					}

	/**
	 * Parse a call to abs or sqr.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseAbsSqr(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one integer or real parameter.
		// The function return type is the parameter type.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if ((argType == Predefined.integerType) ||
					(argType == Predefined.realType)) {
				callNode.setTypeSpec(argType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Parse a call to arctan, cos, exp, ln, sin, or sqrt.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseArctanCosExpLnSinSqrt(Token token,
			ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one integer or real parameter.
		// The function return type is real.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if ((argType == Predefined.integerType) ||
					(argType == Predefined.realType)) {
				callNode.setTypeSpec(Predefined.realType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Parse a call to pred or succ.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parsePredSucc(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one integer or enumeration parameter.
		// The function return type is the parameter type.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if ((argType == Predefined.integerType) ||
					(argType.getForm() == ENUMERATION))
			{
				callNode.setTypeSpec(argType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Parse a call to chr.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseChr(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one integer parameter.
		// The function return type is character.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if (argType == Predefined.integerType) {
				callNode.setTypeSpec(Predefined.charType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Parse a call to odd.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseOdd(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one integer parameter.
		// The function return type is boolean.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if (argType == Predefined.integerType) {
				callNode.setTypeSpec(Predefined.booleanType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Parse a call to ord.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseOrd(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one character or enumeration parameter.
		// The function return type is integer.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if ((argType == Predefined.charType) ||
					(argType.getForm() == ENUMERATION)) {
				callNode.setTypeSpec(Predefined.integerType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Parse a call to round or trunc.
	 * @param token the current token.
	 * @param callNode the CALL node.
	 * @param pfId the symbol table entry of the standard routine name.
	 * @return ICodeNode the CALL node.
	 * @throws Exception if an error occurred.
	 */
	private ICodeNode parseRoundTrunc(Token token, ICodeNode callNode,
			SymTabEntry pfId)
					throws Exception
					{
		// Parse any actual parameters.
		ICodeNode parmsNode = parseActualParameters(token, pfId,
				false, false, false);
		callNode.addChild(parmsNode);

		// There should be one real parameter.
		// The function return type is integer.
		if (checkParmCount(token, parmsNode, 1)) {
			TypeSpec argType =
					parmsNode.getChildren().get(0).getTypeSpec().baseType();

			if (argType == Predefined.realType) {
				callNode.setTypeSpec(Predefined.integerType);
			}
			else {
				errorHandler.flag(token, INVALID_TYPE, this);
			}
		}

		return callNode;
					}

	/**
	 * Check the number of actual parameters.
	 * @param token the current token.
	 * @param parmsNode the PARAMETERS node.
	 * @param count the correct number of parameters.
	 * @return true if the count is correct.
	 */
	private boolean checkParmCount(Token token, ICodeNode parmsNode, int count)
	{
		if ( ((parmsNode == null) && (count == 0)) ||
				(parmsNode.getChildren().size() == count) ) {
			return true;
		}
		else {
			errorHandler.flag(token, WRONG_NUMBER_OF_PARMS, this);
			return false;
		}
	}
	
	
	
}
