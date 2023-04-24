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

    public INumLitImplementation(String t, Kind k, int x, int y) {
        tokenString = t;
        kind = Kind.NUM_LIT;
        sourceLocation = new SourceLocation(x, y);
    }


    @Override
    public int getValue() {
        try {
            return (int)Integer.parseInt(tokenString);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid token at line " + sourceLocation.line() + " and column " + sourceLocation.column() + ": " + tokenString);
        }
    }

    public Integer getValueTest() {
        try {
            return (int)Integer.parseInt(tokenString);
        } catch (NumberFormatException e) {
            return null;
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
