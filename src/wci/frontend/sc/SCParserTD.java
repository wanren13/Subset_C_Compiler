package wci.frontend.sc;

import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.sc.parsers.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.Predefined;
import wci.message.Message;
import static wci.frontend.sc.SCErrorCode.*;
import static wci.message.MessageType.PARSER_SUMMARY;


public class SCParserTD extends Parser
{
	protected static SCErrorHandler errorHandler = new SCErrorHandler();

	private SymTabEntry routineId;  // name of the routine being parsed
	static protected String programName;

	/**
	 * Constructor.
	 * @param scanner the scanner to be used with this parser.
	 */
	public SCParserTD(Scanner scanner)
	{
		super(scanner);
	}

	/**
	 * Constructor for subclasses.
	 * @param parent the parent parser.
	 */
	public SCParserTD(SCParserTD parent)
	{
		super(parent.getScanner());
	}

	/**
	 * Getter.
	 * @return the routine identifier's symbol table entry.
	 */
	public SymTabEntry getRoutineId()
	{
		return routineId;
	}

	/**
	 * Getter.
	 * @return the error handler.
	 */
	public SCErrorHandler getErrorHandler()
	{
		return errorHandler;
	}
	
	@SuppressWarnings("static-access")
	public void setProgramName(String programName)
	{
		this.programName = programName;
	}

	/**
	 * Parse a subset C source program and generate the symbol table
	 * and the intermediate code.
	 * @throws Exception if an error occurred.
	 */
	public void parse()
			throws Exception
			{
		long startTime = System.currentTimeMillis();
		Predefined.initialize(symTabStack);
		
		try {
			Token token = nextToken();
			
			// Parse a program.
			ProgramParser programParser = new ProgramParser(this);
			programParser.parse(token);
			
			token = currentToken();
			
			// Send the parser summary message.
			float elapsedTime = (System.currentTimeMillis() - startTime)/1000f;
			sendMessage(new Message(PARSER_SUMMARY,
					new Number[] {token.getLineNumber(),
					getErrorCount(),
					elapsedTime}));
		}
		catch (java.io.IOException ex) {
			errorHandler.abortTranslation(IO_ERROR, this);
		}
			}

	/**
	 * Return the number of syntax errors found by the parser.
	 * @return the error count.
	 */
	public int getErrorCount()
	{
		return errorHandler.getErrorCount();
	}

	/**
	 * Synchronize the parser.
	 * @param syncSet the set of token types for synchronizing the parser.
	 * @return the token where the parser has synchronized.
	 * @throws Exception if an error occurred.
	 */
	public Token synchronize(EnumSet<SCTokenType> syncSet)
			throws Exception
			{
		Token token = currentToken();

		// If the current token is not in the synchronization set,
		// then it is unexpected and the parser must recover.
		if (!syncSet.contains(token.getType())) {

			// Flag the unexpected token.
			errorHandler.flag(token, UNEXPECTED_TOKEN, this);

			// Recover by skipping tokens that are not
			// in the synchronization set.
			do {
				token = nextToken();
			} while (!(token instanceof EofToken) &&
					!syncSet.contains(token.getType()));
		}

		return token;
			}

}

