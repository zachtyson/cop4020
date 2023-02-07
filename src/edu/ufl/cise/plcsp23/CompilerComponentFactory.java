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

import java.util.ArrayList;

public class CompilerComponentFactory {
	public static IScanner makeScanner(String input) throws LexicalException {
		//Add statement to return an instance of your scanner

		return new IScannerImplementation(input);
	}

}

class IScannerImplementation implements IScanner {
	private int position = 0;
	private ArrayList<IToken> tokens = new ArrayList<IToken>();
	//Scanner has an array of ITokens, and a position variable for the next() method

	@Override
	public IToken next() throws LexicalException {
		if(tokens.size() == 0) {
			throw new LexicalException("No tokens");
			//If there aren't any tokens, throw an exception
		} else {
			return tokens.get(position++);
			//Otherwise return the token at the current position and increment the position
		}
	}

	private String input;

	public IScannerImplementation(String in) throws LexicalException {
		//check if input is empty or null
		if(in == null || in.length() == 0) {
			tokens.add(new ITokenImplementation(input,"EOF",0,0));
		}
		input = in;
		//Iterating over the input string character by character
		//Increment i based on the next character and if it can be part of a token
		//Since of course we want to extend a token as far as possible
		//A while loop would work better here, just from intuition
		int stringLength = input.length();
		int stringSpot = 0;
		String currentToken = "";
		String currentTokenType = "";
		System.out.println(stringLength);
		System.out.println(in);
		while(stringSpot < stringLength) {
			//So I think here I'm going to check a character, see if it can be part of a token, and then increment the stringSpot
			//And once I can't extend the token anymore, I'll add it to the array of tokens [not including the current character]
			//e.g. for '0ada int' i would keep extending until I hit the space, catch the space, and then backtrack to the previous character
			char currentChar = input.charAt(stringSpot);
			//check if currentToken + currentChar is a valid token, otherwise just use currentToken as a token
			//if currentToken + currentChar is a valid token, then add currentChar to currentToken and increment stringSpot

			//print ascii value of currentChar
			System.out.println((int) currentChar);
			if(currentToken.isEmpty()) {
				//If empty token, then try and figure out what currentChar can belong to
				//e.g if currentChar is a number, then it's a NumLit, or if it's a letter then it can be an ident or a reserved word
				//or if it's a quote then it's the beginning of a string literal
				//Java supports switch statements with chars, so I'll use that
				int asciiValue = (int) currentChar;
				//I was initially going to use a switch statement, but unfortunately Java doesn't support ranges in switch statements
				//So for sake of readability (and not having to write dozens of cases) I'll use if statements


				// All valid characters are shown in LexicalStructure.pdf, so anything not there is a LexicalException
				// I will move these if statements into a separate method, in the future, once I get things working
				if(asciiValue == 48) { 	//0, but NOTE that since tokens that start with 0 are only valid if they are the only digit, I can essentially end this token here
					currentToken += currentChar;
					currentTokenType = "NumLit";
					{
						//todo: put stuff here for single digit 0
					}
				} else if(asciiValue > 48 && asciiValue < 58) { //1-9, I do wish Java had support for ranges in switch statements
					currentToken += currentChar;
					currentTokenType = "NumLit";
				} else {
					throw new LexicalException("Invalid character");
				}
			}
			if(analyzeLexicalStructure(currentToken, currentChar)) {
				currentToken += currentChar;
			}
			else {
				//add currentToken to the array of tokens
				//reset currentToken to ""
			}
			stringSpot++;
		}
	}

	//function that parses the input string and checks if input + char is a valid token
	//if it is, then return true and add char to input
	//if it isn't, then return false and don't add char to input
	//WELL I forgot that Java doesn't have passing by reference, so I'll find another way to do this

	private boolean analyzeLexicalStructure(String s, char c) {
		return true;
	}
}


//Multiple lines are inputted as a single string

//A NumLit is a sequence of one or more digits, currently only non-negative integers are supported
//A NumLit cannot start with 0 unless it is the only digit


//A StringLit is a sequence of characters surrounded by double quotes
//A StringLit can contain any character except a double quote or a backslash unless it is preceded by a backslash
//A String_Characters can be any character besides " or \ unless it is an escape sequence
//The following are valid escape commands: \b, \t, \n, \r, \", \\

class ITokenImplementation implements IToken {
	private String tokenString;
	private Kind kind;
	private SourceLocation sourceLocation;


	@Override
	public SourceLocation getSourceLocation() {
		return sourceLocation;
	}

	@Override
	public Kind getKind() {
		return kind;
	}

	@Override
	public String getTokenString() {
		return tokenString;
	}

	public ITokenImplementation(String t, String k, int x, int y) {
		tokenString = t;
		kind = Kind.valueOf(k);
		sourceLocation = new SourceLocation(x, y);
	}
}
