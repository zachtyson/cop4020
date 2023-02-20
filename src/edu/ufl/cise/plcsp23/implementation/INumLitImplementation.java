package edu.ufl.cise.plcsp23.implementation;
//Multiple lines are inputted as a single string

//A NumLit is a sequence of one or more digits, currently only non-negative integers are supported
//A NumLit cannot start with 0 unless it is the only digit

import edu.ufl.cise.plcsp23.INumLitToken;

public class INumLitImplementation extends ITokenImplementation implements INumLitToken {
    //Implementation of a NumLit token
    //It's basically identical to a regular token, but it has a getValue() method which doesn't exist in IToken
    private String tokenString;
    private Kind kind;
    private SourceLocation sourceLocation;

    public INumLitImplementation(String t, String k, int x, int y) {
        tokenString = t;
        kind = Kind.valueOf("NUM_LIT"); //It's always a NUM_LIT in this case
        sourceLocation = new SourceLocation(x, y);
        //try to parse the string as an integer

    }


    @Override
    public int getValue() {
        try {
            return Integer.parseInt(tokenString);
        } catch (NumberFormatException e) {
            return -1;
            //This is just for debugging, since the exceptions should be thrown in the next() method
        }
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
