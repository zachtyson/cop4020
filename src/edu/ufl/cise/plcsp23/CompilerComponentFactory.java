/*Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the spring semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */

package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.exceptions.LexicalException;
import edu.ufl.cise.plcsp23.exceptions.PLCException;
import edu.ufl.cise.plcsp23.implementation.IParserImplementation;
import edu.ufl.cise.plcsp23.implementation.IScannerImplementation;
import edu.ufl.cise.plcsp23.interfaces.IParser;
import edu.ufl.cise.plcsp23.interfaces.IScanner;

import static edu.ufl.cise.plcsp23.IToken.Kind.valueOf;

public class CompilerComponentFactory {
	public static IScanner makeScanner(String input) throws LexicalException {
		//Add statement to return an instance of your scanner

		return new IScannerImplementation(input);
	}

	public static IParser makeAssignment2Parser(String input) throws PLCException {
		//Add statement to return an instance of your parser
		return new IParserImplementation(input);
	}
}

