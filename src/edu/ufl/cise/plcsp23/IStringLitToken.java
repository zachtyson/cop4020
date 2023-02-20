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

import edu.ufl.cise.plcsp23.IToken;

public interface IStringLitToken extends IToken {

	String getValue();
	//A String_Lit is string_characters surrounded by double quotes
	//And a string character contains either A) An input_character, which is any ascii character except LF or CR (Not including " or \)
	//or B) An escape_sequence, which can be: \b, \t, \n, \r, \", \\
	//Similar to what I said in INumLitToken, I think I'm understanding interfaces correctly in this context but I'm not sure
	//I'm willing to sacrifice a good grade on the first assignment to find out

}
