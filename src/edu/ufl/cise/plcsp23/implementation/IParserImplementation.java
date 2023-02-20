package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.exceptions.SyntaxException;
import edu.ufl.cise.plcsp23.interfaces.IParser;
import edu.ufl.cise.plcsp23.exceptions.PLCException;
import edu.ufl.cise.plcsp23.ast.AST;

public class IParserImplementation implements IParser {

    @Override
    public AST parse() throws PLCException {
        return null;
    }

    public IParserImplementation(String input) throws SyntaxException {
        // TODO Auto-generated constructor stub
        throw new SyntaxException("Not yet implemented");
    }
}
