package edu.ufl.cise.plcsp23.interfaces;

import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.ast.AST;

public interface IParser {

	AST parse() throws PLCException;

}