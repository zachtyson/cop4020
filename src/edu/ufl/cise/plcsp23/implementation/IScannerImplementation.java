package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.IScanner;
import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.LexicalException;

import java.util.ArrayList;

import static edu.ufl.cise.plcsp23.IToken.Kind.NUM_LIT;
import static edu.ufl.cise.plcsp23.IToken.Kind.valueOf;

public class IScannerImplementation implements IScanner {
    private int position = 0;

    private int start = 0;

    private int line = 1;

    private int column = 1;
    private ArrayList<IToken> tokens = new ArrayList<IToken>();
    //Scanner has an array of ITokens, and a position variable for the next() method



    @Override
    public IToken next() throws LexicalException {
        return tokens.get(position++);
    }

    private String input;

    public IScannerImplementation(String input) throws LexicalException {
        this.input = input;
        while(isAtEnd()) {
            start = position;
            scanToken();
        }
        tokens.add(new ITokenImplementation("", "EOF", 0, 0));
    }

    private void scanToken() {

    }

    private boolean isAtEnd() {
        return position >= input.length();
    }

}
