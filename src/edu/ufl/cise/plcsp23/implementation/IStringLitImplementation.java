package edu.ufl.cise.plcsp23.implementation;
//A StringLit is a sequence of characters surrounded by double quotes
//A StringLit can contain any character except a double quote or a backslash unless it is preceded by a backslash
//A String_Characters can be any character besides " or \ unless it is an escape sequence
//The following are valid escape commands: \b, \t, \n, \r, \", \\

import edu.ufl.cise.plcsp23.IStringLitToken;
import edu.ufl.cise.plcsp23.LexicalException;

public class IStringLitImplementation extends ITokenImplementation implements IStringLitToken {
    //Implementation of a StringLit token
    //It's basically identical to a regular token, but it has a getValue() method which doesn't exist in IToken
    private String tokenString;
    private String value;
    private Kind kind;
    private SourceLocation sourceLocation;

    public IStringLitImplementation(String t, String k, int x, int y) throws LexicalException {
        tokenString = t;
        kind = Kind.valueOf("STRING_LIT"); //It's always a STRING_LIT in this case
        sourceLocation = new SourceLocation(x, y);

        //remove first and last character
        value = tokenString.substring(1, tokenString.length() - 1);
        //Iterate over new string and check for illegal escape sequences
        //If there is an illegal escape sequence, throw an exception
        for (int i = 0; i < value.length(); i++) { //This checks for all escape sequences and makes sure that things are valid
            //Honestly not sure what I did
            if (value.charAt(i) == '\\') {
                try {
                    if (value.charAt(i + 1) != 'b' && value.charAt(i + 1) != 't' && value.charAt(i + 1) != 'n' && value.charAt(i + 1) != 'r' && value.charAt(i + 1) != '"' && value.charAt(i + 1) != '\\') {
                        kind = Kind.valueOf("ERROR");
                        //Checks for all possible escape values and if it's not one of them, it sets an error state
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    kind = Kind.valueOf("ERROR");
                }
            }
        }

        //Looking for illegal line terminators
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n') {
                kind = Kind.valueOf("ERROR");
            }
            //Checks for ACTUAL line terminators, not just escape sequences
            //if that makes sense (escape sequences just confuse me)
        }

        //replace escape sequences with their actual values
        value = value.replace("\\b", "\b");
        value = value.replace("\\t", "\t");
        value = value.replace("\\n", "\n");
        value = value.replace("\\r", "\r");
        value = value.replace("\\\"", "\"");
        value = value.replace("\\\\", "\\");
        //honestly not really sure how this works LMAO

        //For catching the illegal escape sequence error, I think the course of action would be to
        //check if the string contains a backslash, and if it does, check if the next character is a valid escape sequence
        // e.g \n isn't valid, but \\n is because the first backslash is an escape sequence and the second is the newline
        //Initial plan was to check while parsing, but I couldn't figure out how to do it


    }

    @Override
    public String getValue() {
        return value;
        //returns the actual literal value of the string
    }

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
}
