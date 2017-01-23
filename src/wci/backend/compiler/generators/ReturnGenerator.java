package wci.backend.compiler.generators;

import wci.intermediate.*;
import wci.backend.compiler.*;

import static wci.backend.compiler.Instruction.*;

/**
 * <h1>IfGenerator</h1>
 *
 * <p>Generate code for an IF statement.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class ReturnGenerator extends StatementGenerator
{
    /**
     * Constructor.
     * @param the parent executor.
     */
	
	static Label newLabel;
		
    public ReturnGenerator(CodeGenerator parent)
    {
        super(parent);
    }
    /**
     * Generate code for a RETURN statement.
     * @param node the root node of the statement.
     */
    public void generate(ICodeNode node)
        throws PascalCompilerException
    {       
       newLabel = Label.newLabel();
       emit(GOTO, newLabel);
    }
    
    static public Label getReturnLabel()
    {
    	return newLabel;
    }    
}
