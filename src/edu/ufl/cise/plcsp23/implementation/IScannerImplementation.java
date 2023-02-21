package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.IScanner;
import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.LexicalException;

import java.util.ArrayList;

import static edu.ufl.cise.plcsp23.IToken.Kind.NUM_LIT;
import static edu.ufl.cise.plcsp23.IToken.Kind.valueOf;

public class IScannerImplementation implements IScanner {
    private int position = 0;
    private ArrayList<IToken> tokens = new ArrayList<IToken>();
    //Scanner has an array of ITokens, and a position variable for the next() method

    @Override
    public IToken next() throws LexicalException {
        for (IToken token : tokens) {
            if (token.getKind().equals(NUM_LIT)) {
                try {
                    int val = Integer.parseInt(token.getTokenString());
                } catch (NumberFormatException e) {
                    throw new LexicalException("Number out of bounds");
                }
            }
        }
        if (tokens.size() == 0) {
            throw new LexicalException("No tokens");
            //If there aren't any tokens, throw an exception
        } else {
            IToken tokenToReturn;
            position++;
            if (position >= tokens.size()) {
                //if trying to get a token that doesn't exist return last token
                tokenToReturn = tokens.get(tokens.size() - 1);
            } else {
                tokenToReturn = tokens.get(position - 1);
            }

            if (tokenToReturn.getKind().equals(valueOf("ERROR"))) {
                throw new LexicalException("Invalid operator");
            }
            return tokenToReturn;


            //Otherwise return the token at the current position and increment the position
        }
    }

    private String input;

    public IScannerImplementation(String in) throws LexicalException {
        //check if input is empty or null
        if (in == null || in.length() == 0) {
            tokens.add(new ITokenImplementation("", "EOF", 0, 0));
            return;
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
        int line = 1;
        int column = 1;
        int currLine = 1;
        int currColumn = 0;
        while (stringSpot < stringLength) {
            currColumn++;

            //So I think here I'm going to check a character, see if it can be part of a token, and then increment the stringSpot
            //And once I can't extend the token anymore, I'll add it to the array of tokens [not including the current character]
            //e.g. for '0ada int' i would keep extending until I hit the space, catch the space, and then backtrack to the previous character
            char currentChar = input.charAt(stringSpot);
            //check if currentToken + currentChar is a valid token, otherwise just use currentToken as a token
            //if currentToken + currentChar is a valid token, then add currentChar to currentToken and increment stringSpot

            //characters that can be operators:
            // . | , | ? | : | ( | ) | < | > | [ | ] | { | } | = | == | <-> | <= | >= | ! | & | && | | | || |
            // + | - | * | ** | / | %
            //directly from LexicalStructure.pdf
            int[] operatorChars = {46, 44, 63, 58, 40, 41, 60, 62, 91, 93, 123, 125, 61, 45, 61, 33, 38, 43, 45, 42, 47, 37, 124};
            if (currentToken.isEmpty()) {
                line = currLine;
                column = currColumn;
                //If empty token, then try and figure out what currentChar can belong to
                //e.g if currentChar is a number, then it's a NumLit, or if it's a letter then it can be an ident or a reserved word
                //or if it's a quote then it's the beginning of a string literal
                //Java supports switch statements with chars, so I'll use that
                int asciiValue = (int) currentChar;
                //I was initially going to use a switch statement, but unfortunately Java doesn't support ranges in switch statements
                //So for sake of readability (and not having to write dozens of cases) I'll use if statements


                // All valid characters are shown in LexicalStructure.pdf, so anything not there is a LexicalException
                // I will move these if statements into a separate method, in the future, once I get things working
                if (asciiValue == 48) {    //0, but NOTE that since tokens that start with 0 are only valid if they are the only digit, I can essentially end this token here
                    currentToken += currentChar;
                    currentTokenType = "NUM_LIT";
                    tokens.add(new INumLitImplementation(currentToken, currentTokenType, line, column));
                    currentToken = "";
                    currentTokenType = "";
                    stringSpot++;
                    continue;
                } else if (asciiValue > 48 && asciiValue < 58) { //1-9, I do wish Java had support for ranges in switch statements
                    currentTokenType = "NUM_LIT";
                } else if (asciiValue == 32 || asciiValue == 10) { //spaces serve no purpose other than separating tokens, unless in a string literal
                    //New token unless in a string literal
                    if (currentTokenType.equals("STRING_LIT")) {
                        currentToken += currentChar;
                    } else {
                        if ((int) currentChar == 10) {
                            currLine++;
                            currColumn = 0; //New line, so reset column and increment line
                        }
                        //add currentToken to the array of tokens
                        if (currentTokenType.equals("")) {
                            //If currentTokenType is empty, then it's a space, so just ignore it
                            stringSpot++;
                            continue;
                        }
                        if (currentTokenType.equals("NUM_LIT")) {
                            tokens.add(new INumLitImplementation(currentToken, currentTokenType, line, column));
                        } else {
                            tokens.add(new ITokenImplementation(currentToken, currentTokenType, line, column));
                        }
                        currentToken = "";
                        currentTokenType = "";
                        continue;
                        //reset currentToken to ""
                    }
                } else if ((asciiValue < 91 && asciiValue > 64) || (asciiValue > 96 && asciiValue < 123) || asciiValue == 95) {
                    //A-Z, a-z, or _
                    //Beginning of an ident or reserved word
                    //Assume it's an ident until it matches the list of reserved words
                    currentTokenType = "IDENT";
                    currentToken += currentChar;
                    stringSpot++;
                    continue;

                } else if (asciiValue == 126) {
                    currentTokenType = "COMMENT";
                    //Comments aren't added to the array of tokens,
                    while (asciiValue != 10) {
                        stringSpot++;
                        currentChar = input.charAt(stringSpot);
                        asciiValue = (int) currentChar;
                    }
                    //So everything after the comment is ignored until a new line is reached
                    currentToken = "";
                    currentTokenType = "";
                    continue;
                }
                //else if asciiValue is in operators[]
                else if (doesArrayContain(operatorChars, asciiValue)) {
                    //First character signifies this can be an operator
                    currentTokenType = "OPERATOR";
                    currentToken += currentChar;
                    stringSpot++;
                    continue;
                } else if (asciiValue == 92) {
                    //92 in ASCII is a backslash, which is used to escape characters
                    //Backslashes will only be used for whitespace or in string literals
                    currentTokenType = "ESCAPE";
                    currentToken += currentChar;
                } else if (asciiValue == 9 || asciiValue == 11 || asciiValue == 12 || asciiValue == 13) {
                    currentTokenType = "ESCAPE";
                    currentToken += currentChar;
                } else if (asciiValue == 34) { //This is a double quote and signifies the beginning of a string literal
                    currentTokenType = "STRING_LIT";
                    currentToken += currentChar;
                    stringSpot++;
                    continue;
                } else {
                    //If it's not a valid character, create an error token
                    currentTokenType = "ERROR";
                    currentToken += currentChar;
                    tokens.add(new ITokenImplementation(currentToken, currentTokenType, line, column));
                    return;
                }

            }
            if (analyzeLexicalStructure(currentToken, currentChar, currentTokenType)) {
                currentToken += currentChar;
            } else {
                if ((int) currentChar == 10) {
                    currLine++;
                    currColumn = 0;
                }
                if (currentTokenType.equals("NUM_LIT")) {
                    tokens.add(new INumLitImplementation(currentToken, currentTokenType, line, column));
                    currColumn = currColumn -2;
                    //check if stringSpot++ is out of bounds
                    if((int)currentChar == 10) {

                        currLine--;
                    }
                    stringSpot--;
                } else if (currentTokenType.equals("IDENT")) {
                    //Check if currentToken is a reserved word
                    //Remove whitespace
                    currentToken = currentToken.trim();
                    String[] reservedWords = {"image", "pixel", "int", "string", "void", "nil", "load", "display", "write",
                            "x", "y", "a", "r", "X", "Y", "Z", "x_cart", "y_cart", "a_polar", "r_polar", "rand", "sin", "cos", "atan", "if", "while"};
                    //check if currentToken is a reserved word
                    for (String reservedWord : reservedWords) {
                        if (currentToken.equals(reservedWord)) {
                            currentTokenType = "RES_" + reservedWord;
                            break;
                        }
                    }
                    currColumn = currColumn -2;
                    //check if stringSpot++ is out of bounds
                    if((int)currentChar == 10) {

                        currLine--;
                    }
                    tokens.add(new ITokenImplementation(currentToken, currentTokenType, line, column));
                    stringSpot--;

                } else if (currentTokenType.equals("OPERATOR")) {
                    tokens.add(new ITokenImplementation(currentToken, currentTokenType, line, column));
                    currentToken = "" + currentChar;
                    stringSpot--;

                } else if (currentTokenType.equals("ESCAPE")) {
                    //If it's a backslash + whitespace, then it's a whitespace escape
                    //If it's not a whitespace escape, then we seem to have an invalid escape sequence
                    //Not really sure how to handle invalid escapes
                    //I can either ignore them or make an error token but I'm not sure
                    //For now it's just ignored
                    currentToken = "";
                    currentTokenType = "";
                } else if (currentTokenType.equals("STRING_LIT")) {
                    currentToken += "\"";
                    tokens.add(new IStringLitImplementation(currentToken, currentTokenType, line, column));
                    currentToken = "";
                    currentTokenType = "";

                } else {
                    tokens.add(new ITokenImplementation(currentToken, currentTokenType, line, column));
                }
                currentToken = "";
                currentTokenType = "";

                //add currentToken to the array of tokens
                //reset currentToken to ""
            }
            stringSpot++;
        }
        //if token wasn't empty then add it to the array of tokens, otherwise just add EOF
        if (!currentToken.isEmpty()) {
            String[] reservedWords = {"image", "pixel", "int", "string", "void", "nil", "load", "display", "write",
                    "x", "y", "a", "r", "X", "Y", "Z", "x_cart", "y_cart", "a_polar", "r_polar", "rand", "sin", "cos", "atan", "if", "while"};
            //check if currentToken is a reserved word
            for (String reservedWord : reservedWords) {
                if (currentToken.equals(reservedWord)) {
                    currentTokenType = "RES_" + reservedWord;
                    break;
                }
            }
            tokens.add(new ITokenImplementation(currentToken, currentTokenType, line, column));
        }
        tokens.add(new ITokenImplementation("", "EOF", 0, 0));


    }

    //function that parses the input string and checks if input + char is a valid token
    //if it is, then return true and add char to input
    //if it isn't, then return false and don't add char to input
    //WELL I forgot that Java doesn't have passing by reference, so I'll find another way to do this

    private boolean analyzeLexicalStructure(String currentToken, char currentChar, String currentTokenType) throws LexicalException {

        switch (currentTokenType) {
            case "NUM_LIT":
                //check if currentChar is a digit
                return Character.isDigit(currentChar);
            case "STRING_LIT":
                //Allow any character except for a double quote (unless it's an escape sequence)
                //If it's an escape sequence, then it's a valid character
                //If it's a double quote, then it's the end of the string literal
                if (currentChar == '"') {
                    //check the last of the currentToken to see if it's a backslash
                    //if it is, then it's a valid string literal
                    //if it isn't, then it's the end of the string literal
                    if (currentToken.charAt(currentToken.length() - 1) == '\\') {
                        return true;
                    } else {
                        return false;
                    }

                } else {
                    return true;
                }
            case "IDENT":
                //check if currentChar is a digit, letter, or _
                if (Character.isDigit(currentChar) || Character.isLetter(currentChar) || currentChar == '_') {
                    // valid character, so it's the same token
                    // There's the issue with reserved words but this is checked in the main method
                    return true;
                } else {
                    //Non valid character, so it's a different token
                    return false;
                }
            case "OPERATOR":
                //check if currentToken + currentChar is a valid operator
                //if it is, then return true
                //if it isn't, then return false
                //List of operators: . , ? : ( ) < > [ ] { } = == <-> <= >= ! & && | || + 0 * ** / %
                String[] operators = {".", ",", "?", ":", "(", ")", "<", ">", "[", "]", "{", "}", "=", "==", "<->", "<=", ">=", "!", "&", "&&", "|", "||", "+", "-", "*", "**", "/", "%"};
                if (doesArrayContain(operators, currentToken + currentChar)) {
                    return true;
                } else {
                    return false;
                }
            case "ESCAPE":
                //check if currentChar is a whitespace character
                //if it is, then return true
                //if it isn't, then return false
                String[] whitespaceChars = {"b", "t", "n", "r", "\"", "\\"};
                if (doesArrayContain(whitespaceChars, currentToken + currentChar)) {
                    return true;
                } else {
                    return false;
                }
            default:
                throw new LexicalException("Invalid token");
        }
    }

    public boolean doesArrayContain(String[] array, String string) {
        for (String element : array) {
            //if string is a substring of element, then return true
            if (element.contains(string)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesArrayContain(int[] array, int number) {
        for (int element : array) {
            if (element == number) {
                return true;
            }
        }
        return false;
    }
}
