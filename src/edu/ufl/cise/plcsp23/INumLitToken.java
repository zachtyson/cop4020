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

public interface INumLitToken extends IToken {

	Integer getValue();
	//A Num_Lit token can either be a 0, or a non-zero digit followed by a sequence of digits (can't start with 0 and can't be empty)
	//In this project, the number is a java integer, so the bounds are -2^31 to 2^31-1
	//I'm not really experienced with interfaces but I think in this case it's identical to IToken except it has an associated value


}
