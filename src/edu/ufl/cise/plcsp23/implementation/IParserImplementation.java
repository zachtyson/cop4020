package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;

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

    public IParserImplementation(String input) throws PLCException {
        System.out.println(input);
        if(input == null || input.isEmpty()) {
            throw new SyntaxException("Input is null or empty");
        }
        //Create a scanner
        IScannerImplementation scanner = new IScannerImplementation(input);
        //Pass the input to the scanner
        //Get each token from the scanner and parse it into an AST
        ArrayList<IToken> tokenList = new ArrayList<>();
        while(true) {
            IToken token = scanner.next();
            if(token.getKind() == IToken.Kind.EOF) {
                break;
            }
            tokenList.add(token);
        }

        if(tokenList.size() == 0) {
            throw new SyntaxException("No tokens to parse");
        }
        //Iterate over tokenList print out each token
        System.out.println(tokenList.size());
        for(IToken token : tokenList) {
            System.out.println(token.getTokenString());
        }
        while(tokenList.size() > 0) {
            AST ast = parseExpr(tokenList);
            if(ast == null) {
                break;
            }
            ASTList.add(ast);
        }



    }

    AST parseExpr(ArrayList<IToken> tokenList) throws PLCException {
        if(tokenList.size() == 0) {
           return null;
        }
        //Primary_Expr - STRING_LIT,NUM_LIT,IDENT, (Expr),Z,rand
        return null;
    }
}
