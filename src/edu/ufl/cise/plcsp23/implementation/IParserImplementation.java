package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;

public class IParserImplementation implements IParser {
    private ArrayList<AST> ASTList = new ArrayList<AST>();
    private int index = 0;
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
        parseExpr(tokenList);



    }

    boolean parseExpr(ArrayList<IToken> tokenList) throws PLCException {
        if(tokenList.size() == 0) {
           throw new SyntaxException("No tokens to parse");
        }
        for (IToken iToken : tokenList) {
            IToken.Kind kind = iToken.getKind();
            //So my logic here is to kinda go left to right and parse each token as it comes in
            //And then after tokens are parsed I will go over the ASTs and keep parsing until the AST is unchanged through a pass
            switch (kind) {
                case RES_rand -> {
                    //While there always needs to be a left parenthesis, this will be checked in a later pass
                    RandomExpr randomExpr = new RandomExpr(iToken);
                    ASTList.add(randomExpr);
                }
                case IDENT -> {
                    IdentExpr identExpr = new IdentExpr(iToken);
                    ASTList.add(identExpr);
                }
                case NUM_LIT -> {
                    String n = iToken.getTokenString();
                    int x = iToken.getSourceLocation().line();
                    int y = iToken.getSourceLocation().column();
                    INumLitToken numLitToken = new INumLitImplementation(n, "NUM_LIT", x, y);
                    NumLitExpr numExpr = new NumLitExpr(numLitToken);
                    ASTList.add(numExpr);
                }
                case STRING_LIT -> {
                    String n = iToken.getTokenString();
                    int x = iToken.getSourceLocation().line();
                    int y = iToken.getSourceLocation().column();
                    IStringLitToken stringLitToken = new IStringLitImplementation(n, "STRING_LIT", x, y);
                    StringLitExpr stringExpr = new StringLitExpr(stringLitToken);
                    ASTList.add(stringExpr);
                }
                default -> {
                    ZExpr zExpr = new ZExpr(iToken);
                    ASTList.add(zExpr);
                }
            }


        }
        for(AST ast : ASTList) {
            System.out.println(ast.getFirstToken().getTokenString() + " " + ast.getFirstToken().getKind());
        }


        //Primary_Expr - STRING_LIT,NUM_LIT,IDENT, (Expr),Z,rand
        //Go left to right repeatedly until either there is no more tokens, there are no more ASTs, or there is a syntax error



        return true;
    }

    void ifStatement(ArrayList<IToken> tokenList) {

    }

    boolean compressAST() throws PLCException{
        //Iterate over ASTList and if possible convert Expr into more specific Expr
        //If no changes are made to the ASTList, then return false
        for(int i = 0;i < ASTList.size(); i++) {
            AST ast = ASTList.get(i);
            if(ast instanceof ZExpr) {
                //unary expr !, -,sin,cos,atan followed by either a primary_expr or another unary_expr
                switch(ast.firstToken.getKind()) {
                    case BANG,MINUS,RES_sin,RES_cos,RES_atan -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        AST ast2 = ASTList.get(i + 1);
                        if(ast2 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                        } else if (ast2 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                        } else {
                            throw new SyntaxException("Expected a primary expression or unary expression after unary expression");
                        }
                    }

                }
            }
        }
        return false;
    }

}
