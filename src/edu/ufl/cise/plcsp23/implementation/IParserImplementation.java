package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.LexicalException;
import edu.ufl.cise.plcsp23.SyntaxException;
import edu.ufl.cise.plcsp23.IParser;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.ast.AST;

import java.util.ArrayList;

public class IParserImplementation implements IParser {
    private ArrayList<AST> ASTList = new ArrayList<AST>();
    @Override
    public AST parse() throws PLCException {
        if(ASTList.size() == 0) {
            throw new SyntaxException("No ASTs to parse");
        }
        return ASTList.get(0);
    }

    //expr = conditional_expr | or_expr
    //conditional_expr = if expr ? expr ? expr
    //or_expr = and_expr (|/|| and_expr)* -- At least one and_expr followed by zero or more |/|| and_expr
    //and_expr comparison_expr (&/&& comparison_expr)* -- At least one comparison_expr followed by zero or more &/&& comparison_expr
    //comparison_expr = power_expr (</>/==/<=/>= power_expr)* -- At least one power_expr followed by zero or more ==/!=/</>/<=/>= power_expr
    //power_expr = additive_expr ** power_expr | additive_expr -- At least one additive_expr followed by ** and either another power_expr or an additive_expr
    //additive_expr = multiplicative_expr (+/- multiplicative_expr)* -- At least one multiplicative_expr followed by zero or more +/- multiplicative_expr
    //multiplicative_expr = unary_expr (*//% unary_expr)* -- At least one unary_expr followed by zero or more */% unary_expr

    public IParserImplementation(String input) throws SyntaxException, LexicalException, PLCException {
        if(input == null || input.isEmpty()) {
            throw new SyntaxException("Input is null or empty");
        }
        //Create a scanner
        IScannerImplementation scanner = new IScannerImplementation(input);
        //Pass the input to the scanner
        while (true) {
            try {
                throw new SyntaxException("Not yet implemented");
            } catch (Exception e) {
                if(e.getMessage().equals("Number out of bounds")) {
                    throw new SyntaxException("Number out of bounds");
                    //I don't think this will ever happen given the nature of Assignment 2
                } else if(e.getMessage().equals("No tokens")) {
                    //This should only happen when there is an empty input, and it somehow gets past the null check
                    throw new SyntaxException("Input is null or empty");
                }
                break;
            }
        }

        throw new SyntaxException("Not yet implemented");
    }
}
