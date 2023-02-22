package edu.ufl.cise.plcsp23.implementation;

import com.sun.tools.jconsole.JConsoleContext;
import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;

import java.io.Console;
import java.util.ArrayList;
import java.util.Objects;

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
        //System.out.println(input);
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
        boolean notCompressed = true;
        int count = 0;
        while(notCompressed) {
            //System.out.println("Compression Pass: "+count);
            count++;
            //System.out.println("ASTs:" + ASTList.size());
            for (AST ast : ASTList) {
                //System.out.println(ast.firstToken.getKind()+" "+ast.firstToken.getTokenString());
            }
            notCompressed = compressAST(0);
        }
        while(notCompressed) {
            //System.out.println("Compression Pass: "+count);
            count++;
            //System.out.println("ASTs:" + ASTList.size());
            for (AST ast : ASTList) {
                //System.out.println(ast.firstToken.getKind()+" "+ast.firstToken.getTokenString());
            }
            notCompressed = compressAST(0);
        }
        for(AST ast : ASTList) {

        }
        System.out.println("ASTs:");
        for(AST ast : ASTList) {
            System.out.println(ast.getFirstToken().getTokenString() + " " + ast.getFirstToken().getKind());
            //recursivelyPrintAST(ast);
        }

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



        //Primary_Expr - STRING_LIT,NUM_LIT,IDENT, (Expr),Z,rand
        //Go left to right repeatedly until either there is no more tokens, there are no more ASTs, or there is a syntax error



        return true;
    }

    void ifStatement(ArrayList<IToken> tokenList) {

    }

    boolean compressAST(int startSpot) throws PLCException{
        //Iterate over ASTList and if possible convert Expr into more specific Expr
        //If no changes are made to the ASTList, then return false
        boolean same = true;
        ArrayList<AST> compressed = new ArrayList<AST>();
//        for(AST ast : ASTList) {
//            System.out.println(ast.firstToken.getKind() + " " + ast.firstToken.getTokenString());
//        }
        //parseMultiDivModulo(compressed);

        for(int i = startSpot ;i < ASTList.size(); i++) {
            //System.out.println(i);
            //System.out.println("TEMP" + ASTList.get(i).firstToken.getKind() + " " + ASTList.get(i).firstToken.getTokenString());
            AST ast = ASTList.get(i);
            if(ast instanceof ZExpr) {
                //unary expr !, -,sin,cos,atan followed by either a primary_expr or another unary_expr
                switch(ast.firstToken.getKind()) {
                    case BITOR,OR -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        AST ast2 = ASTList.get(i + 1);
                        if(ast2 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast2 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast2.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast2 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case AND,BITAND -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast2 = ASTList.get(i + 1);
                        if(ast2 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast2 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast2.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast2 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case GE,LE,EQ,LT,GT -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast2 = ASTList.get(i + 1);
                        if(ast2 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast2 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast2.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast2 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case BANG,MINUS,RES_sin,RES_cos,RES_atan -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast2 = ASTList.get(i + 1);
                        if(ast2 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast2 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast2.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast2 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast2);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case LPAREN -> {
                        //Check second AST
                        if(i + 1 >= ASTList.size()) {
                            throw new SyntaxException("Expected ) after (1");
                        }
                        //Iterate over ASTList until we find a matching )
                        int balance = 0;
                        for(int j = i; j < ASTList.size(); j++) {
                            AST ast2 = ASTList.get(j);
                            if(ast2.firstToken.getKind() == IToken.Kind.LPAREN) {
                                balance--;
                            } else if(ast2.firstToken.getKind() == IToken.Kind.RPAREN) {
                                balance++;
                            }
                        }
                        if(balance != 0) {
                            throw new SyntaxException("Expected ) after (2");
                        }

                        AST ast2 = ASTList.get(i + 1);
                        if(ast2 instanceof NumLitExpr || ast2 instanceof StringLitExpr || ast2 instanceof IdentExpr || ast2 instanceof ZExpr  || ast2 instanceof UnaryExpr) {
                            //Expression within a parenthesis, just have to look for matching )
                            if(ast2 instanceof ZExpr) {
                                if(ast2.getFirstToken().getKind() == IToken.Kind.RPAREN) {
                                    throw new SyntaxException("Expected expression after (");
                                }
                                if(ast2.getFirstToken().getKind() == IToken.Kind.LPAREN) {
                                    if(i + 2 >= ASTList.size()) {
                                        throw new SyntaxException("Expected ) after (3");
                                    }
                                    AST ast3 = ASTList.get(i + 2);
                                    if(ast3.firstToken.getKind() == IToken.Kind.RPAREN) {
                                        throw new SyntaxException("Expected expression after (");
                                    }
                                }
                            }
                            if(i + 2 >= ASTList.size()) {
                                throw new SyntaxException("Expected ) after (3");
                            }
                            AST ast3 = ASTList.get(i + 2);
                            if(ast3.firstToken.getKind() == IToken.Kind.RPAREN) {
                                //This is a primary_expr within a parenthesis, so it can be compressed from three ASTs to one
                                String s = ast2.firstToken.getTokenString();
                                int x = ast.firstToken.getSourceLocation().line();
                                int y = ast.firstToken.getSourceLocation().column();
                                if(ast2 instanceof NumLitExpr) {
                                    INumLitToken n = new INumLitImplementation(s, "NUM_LIT", x, y);
                                    NumLitExpr numExpr = new NumLitExpr(n);
                                    compressed.add(numExpr);
                                } else if (ast2 instanceof StringLitExpr) {
                                    IStringLitToken n = new IStringLitImplementation(s, "STRING_LIT", x, y);
                                    StringLitExpr numExpr = new StringLitExpr(n);
                                    compressed.add(numExpr);
                                } else if (ast2 instanceof IdentExpr) {
                                    IToken n = new ITokenImplementation(s, "IDENT", x, y);
                                    IdentExpr numExpr = new IdentExpr(n);
                                    compressed.add(numExpr);
                                } else {
                                    IToken n = new ITokenImplementation(s, ast2.firstToken.getKind().toString(), x, y);
                                    IdentExpr numExpr = new IdentExpr(n);
                                    compressed.add(numExpr);
                                }
                                i = i + 2;
                                same = false;
                            } else {
                                //If it isn't formatted like (Expr), we just move on because there may be a valid expression later
                                //So we try to see if maybe another compressAST() the stuff within the parenthesis will be compressed into a single expression
                                i++;
                                compressed.add(ast);
                                continue;
                            }

                        }
                        else if (ast2 instanceof BinaryExpr) {
                            if(i + 2 >= ASTList.size()) {
                                throw new SyntaxException("Expected ) after (3");
                            }
                            AST ast3 = ASTList.get(i + 2);
                            if(ast3.firstToken.getKind() == IToken.Kind.RPAREN) {
                                //This is a primary_expr within a parenthesis, so it can be compressed from three ASTs to one
                                String s = ast2.firstToken.getTokenString();
                                int x = ast.firstToken.getSourceLocation().line();
                                int y = ast.firstToken.getSourceLocation().column();
                                BinaryExpr numExpr = new BinaryExpr(ast2.firstToken, ((BinaryExpr) ast2).getLeft(), ((BinaryExpr) ast2).getOp(), ((BinaryExpr) ast2).getRight());
                                i = i + 2;
                                compressed.add(numExpr);
                                same = false;
                            } else {
                                //If it isn't formatted like (Expr), we just move on because there may be a valid expression later
                                //So we try to see if maybe another compressAST() the stuff within the parenthesis will be compressed into a single expression
                                i++;
                                same = false;
                                compressed.add(ast);
                                continue;

                            }
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression or unary expression after unary expression");
                        }
                    }
                    case PLUS,DIV-> {
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        //Iterate over ASTList to see if there's any * / or % since they have higher precedence
                        i++;
                        same = false;
                        compressed.add(ast);
                        continue;
                    }
                    case RES_if -> {
                        //Condtionals are formatted as so:
                        //if <expr> ? <expr> ? <expr>
                        //first expression is the guard, second is the true expression, third is the false expression
                        AST ast1 = ASTList.get(i);
                        //So basically this is intended to be a recursive function that will parse the if statement
                        //And I did this because there can technically be infinitely nested if statements

                        if(i + 1 >= ASTList.size()) {
                            //There isn't enough space for an if statement
                            //I could just check for +4 instead of +1 but I did this for sake of future detailed error messages
                            throw new SyntaxException("Expected an if statement");
                        }
                        AST ast2 = ASTList.get(i + 1);
                        if (ast2.firstToken.getKind() == IToken.Kind.RES_if) {
                            compressed.add(ast);
                            boolean[] b = new boolean[6];
                            //Make all the booleans false
                            b[0] = true;
                            b[1] = true;
                            if(i + 2 >= ASTList.size()) {
                                throw new SyntaxException("Expected a ? after expr statement");
                            }
                            AST ast3 = ASTList.get(i + 2);
                            if(ast3.firstToken.getKind() != IToken.Kind.QUESTION) {
                                continue;
                            } else {
                                b[2] = true;
                            }
                            if(i + 3 >= ASTList.size()) {
                                continue;
                            }
                            AST ast4 = ASTList.get(i + 3);
                            if(ast4.firstToken.getKind() != IToken.Kind.QUESTION) {
                                b[3] = true;
                            }
                            if(i + 4 >= ASTList.size()) {
                                continue;
                            }
                            AST ast5 = ASTList.get(i + 4);
                            if(ast5.firstToken.getKind() == IToken.Kind.QUESTION) {
                                b[4] = true;
                            }
                            if(i + 5 >= ASTList.size()) {
                                continue;
                            }
                            AST ast6 = ASTList.get(i + 5);

                            if(!(ast6 instanceof ZExpr)) {
                                b[5] = true;
                            } else b[5] = ast6.firstToken.getKind() != IToken.Kind.QUESTION;
                            compressed.remove(compressed.size() - 1);

                            AST smh = new ConditionalExpr(ast.firstToken, (Expr) ast2, (Expr) ast4, (Expr) ast6);
                            compressed.add(smh);
                            i = i + 5;
                            same = false;
                            break;
                        } else {//This is not an if statement
                            //I think that it would work fine if I just parsed until I found a ?
                            //But I'm not sure if that would be the best way to do it
                            if (i + 2 >= ASTList.size()) {
                                throw new SyntaxException("Expected a ? after expr statement");
                            }
                            AST ast3 = ASTList.get(i + 2);
                            //So if ast3 isn't a ? then I guess I can just return, parse it into a single expression, and then continue
                            if (ast3.firstToken.getKind() != IToken.Kind.QUESTION) {
                                continue;
                            } else {
                                //ast3 is a ? so it goes
                                //ast1 is if
                                //ast2 is guard
                                //ast3 is ?
                                //now we just need true expression, ?, and false expression
                                if (i + 3 >= ASTList.size()) {
                                    throw new SyntaxException("Expected a true expression after ?");
                                }
                                AST ast4 = ASTList.get(i + 3);
                                if (i + 4 >= ASTList.size()) {
                                    throw new SyntaxException("Expected a ? after true expression");
                                    //Should be true expression, or partial true expression
                                }
                                AST ast5 = ASTList.get(i + 4);
                                if (i + 5 >= ASTList.size()) {
                                    throw new SyntaxException("Expected a ? after true expression");
                                }
                                if(ast5.firstToken.getKind() != IToken.Kind.QUESTION) {
                                    //
                                    continue;
                                } else {
                                    AST ast6 = ASTList.get(i + 5);
                                    AST ConditionalExpr = new ConditionalExpr(ast1.firstToken, (Expr)ast2, (Expr)ast4, (Expr)ast6);
                                    compressed.add(ConditionalExpr);
                                    i = i + 5;
                                    same = false;
                                    continue;

                                }
                            }
                        }

                    }
                    default -> {
                        //Else it's just a regular primary_expr, so we just move on since it can't be compressed (it would've been compressed already)
                        compressed.add(ast);

                        continue;
                    }
                }
            }
            else if (ast instanceof RandomExpr || ast instanceof IdentExpr || ast instanceof NumLitExpr || ast instanceof StringLitExpr) {
                //RandomExpr can be compressed into a multiplicative_expr (which can lead to an additive_expr)
                //No need to check the kind of RandomExpr because it can only be one kind
                if(i + 1 == ASTList.size()) {
                    //I don't think there's any issues if this happens but I'll leave it here just in case
                    continue;
                }
                AST ast2 = ASTList.get(i + 1);
                switch(ast2.firstToken.getKind()) {
                    case BITOR,OR -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        AST ast3 = ASTList.get(i + 1);
                        if(ast3 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast3 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast3.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast3 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case AND,BITAND -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 1);
                        if(ast3 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast3 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast3.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast3 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case GE,LE,EQ,LT,GT -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 1);
                        if(ast3 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast3 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast3.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast3 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case BANG,RES_sin,RES_cos,RES_atan -> {
                        //Check second AST
                        if(i + 1 == ASTList.size()) {
                            throw new SyntaxException("Expected an expression after unary expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 1);
                        if(ast3 instanceof UnaryExpr) {
                            //This unary expr is op + unary_expr, so it can be compressed from two ASTs to one
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;

                        } else if (ast3 instanceof ZExpr) {
                            //This unary expr is op + primary_expr, so it can be compressed from two ASTs to one
                            IToken.Kind k = ast3.firstToken.getKind();
                            if(k == IToken.Kind.BANG || k == IToken.Kind.MINUS || k == IToken.Kind.RES_sin || k == IToken.Kind.RES_cos || k == IToken.Kind.RES_atan) {
                                compressed.add(ast);
                                continue;
                            }
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        } else if (ast3 instanceof StringLitExpr) {
                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                        else {
                            //Else I think this can still be a unary expr

                            UnaryExpr unaryExpr = new UnaryExpr(ast.firstToken, ast.firstToken.getKind(), (Expr) ast3);
                            compressed.add(unaryExpr);
                            same = false;
                        }
                    }
                    case PLUS,MINUS -> { //additive_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after additive expression 1");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after additive expression 2");
                        }
                    }
                    case TIMES,DIV,MOD -> { //multiplicative_expr
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            same = false;
                            i = i + 2;
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    case EXP -> {
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after power expression");
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            same = false;
                            i = i + 2;
                            System.out.println("I'm here");
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after power expression");
                        }
                    }
                    default -> {
                        compressed.add(ast);

                    }

                }
                i++;
                continue;
            }
            else if (ast instanceof UnaryExpr) {
                //UnaryExpr can be treated similar to a primary_expr in the sense
                //that ie can be compressed into multiplicative,additive, or power expressions
                if(i + 1 == ASTList.size()) {
                    //I don't think there's any issues if this happens but I'll leave it here just in case
                    i++;
                    continue;
                }
                AST ast2 = ASTList.get(i + 1);
                switch(ast2.firstToken.getKind()) {

                    case PLUS,MINUS -> { //additive_expr

                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after additive expression 3");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;

                        } else {
                            throw new SyntaxException("Expected a primary expression after additive expression 4");
                        }
                    }
                    case TIMES,DIV,MOD -> { //multiplicative_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;

                        } else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    case EXP -> { //power_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after power expression");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after power expression");
                        }
                    }
                }
            }
            else if (ast instanceof BinaryExpr) {
                if(i + 1 == ASTList.size()) {
                    //I don't think there's any issues if this happens but I'll leave it here just in case
                    i++;
                    continue;
                }
                AST ast2 = ASTList.get(i + 1);
                switch (ast2.firstToken.getKind()) {

                    case PLUS, MINUS,OR,BITOR,AND,BITAND,GE,LE,EQ,GT,LT,EXP,TIMES,MOD,DIV,BANG,RES_sin,RES_cos,RES_atan -> {
                        if (i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after additive expression 5 ");
                        }
                        if(checkHigherPrecedence(i + 1,compressed)) {
                            compressed.add(ast);
                            continue;
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if (ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;
                        }  else if(ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;
                        }
                        else {
                            i++;
                            continue;
                        }
                    }
                }
            }
            else {
                i++;
            }
        }
        if(same) {
            return false;
        } else {

            ASTList = compressed;
            return true;
        }
    }
    void recursivelyPrintAST(AST a) {
        if(a instanceof BinaryExpr) {
            recursivelyPrintAST(((BinaryExpr) a).getLeft());
            System.out.print(((BinaryExpr) a).getOp());
            recursivelyPrintAST(((BinaryExpr) a).getRight());
        } else if (a instanceof UnaryExpr) {
            System.out.println(((UnaryExpr) a).getOp());
            recursivelyPrintAST(((UnaryExpr) a).getE());
        } else {
            System.out.print(a.firstToken.getTokenString());
        }
    }

    boolean checkHigherPrecedence(int spot,ArrayList<AST> ASTList) {
        //Iterates through the ASTList and checks if there is a higher precedence operator
        //if there is, consume those first before consuming the current operator
        //The precedence goes:
        //| ||
        //& &&
        // < > == <= >=
        // **
        // + -
        //* / %
        //! -(negative) sin cos atan
        if(spot >= ASTList.size()) {
            return false;
        }
        AST ast = ASTList.get(spot);
        if(!(ast instanceof ZExpr)) {
            return false;
        }
        int s = getPrecedenceNum(spot, ASTList, ast);
        ArrayList<Integer> precedenceList = new ArrayList<>();
        for(int i = spot; i < ASTList.size(); i++) {
            AST ast2 = ASTList.get(i);
            if(ast2 instanceof ZExpr) {
                int num = getPrecedenceNum(i, ASTList, ast2);
                if(num <= s) {
                    return true;
                }
            }
        }
        return false;


    }

    int getPrecedenceNum(int spot, ArrayList<AST> ASTList, AST ast) {
        return switch (ast.firstToken.getKind()) {
            case OR,BITOR-> 1;
            case AND,BITAND -> 2;
            case LT,GT,LE,GE,EQ -> 3;
            case EXP -> 4;
            case PLUS -> 5;
            case TIMES,DIV,MOD -> 6;
            case BANG,RES_atan,RES_cos,RES_sin-> 7;
            case MINUS -> getMinusPrecedence(spot, ASTList);
            default -> 100;
        };
    }

    int getMinusPrecedence(int spot, ArrayList<AST> ASTList) {
        //Checks if the minus is a negative sign or a subtraction sign
        //If it's a negative sign, it returns 7
        //If it's a subtraction sign, it returns 5
        if(spot == 0) {
            return 5;
        }
        //Absolutely no idea how I would code this so I'm just gonna leave it like this until a problem arises
        else {
            return 7;
        }
    }
}
