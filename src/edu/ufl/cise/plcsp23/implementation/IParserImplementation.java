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
            notCompressed = compressAST();
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
        for(AST ast : ASTList) {
            //System.out.println(ast.getFirstToken().getTokenString() + " " + ast.getFirstToken().getKind());
            //recursivelyPrintAST(ast);
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
        boolean same = true;
        ArrayList<AST> compressed = new ArrayList<AST>();
//        for(AST ast : ASTList) {
//            System.out.println(ast.firstToken.getKind() + " " + ast.firstToken.getTokenString());
//        }
        //parseMultiDivModulo(compressed);
        for(int i = 0;i < ASTList.size(); i++) {
            System.out.println(i);
            //System.out.println("TEMP" + ASTList.get(i).firstToken.getKind() + " " + ASTList.get(i).firstToken.getTokenString());
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
                        if(ast2 instanceof NumLitExpr) {
                            //Expression within a parenthesis, just have to look for matching )
                            if(i + 2 >= ASTList.size()) {
                                throw new SyntaxException("Expected ) after (3");
                            }
                            AST ast3 = ASTList.get(i + 2);
                            if(ast3.firstToken.getKind() == IToken.Kind.RPAREN) {
                                //This is a primary_expr within a parenthesis, so it can be compressed from three ASTs to one
                                String s = ast2.firstToken.getTokenString();
                                int x = ast.firstToken.getSourceLocation().line();
                                int y = ast.firstToken.getSourceLocation().column();
                                INumLitToken n = new INumLitImplementation(s, "NUM_LIT", x, y);
                                NumLitExpr numExpr = new NumLitExpr(n);
                                i = i + 2;
                                same = false;
                                compressed.add(numExpr);
                            } else {
                                //If it isn't formatted like (Expr), we just move on because there may be a valid expression later
                                //So we try to see if maybe another compressAST() the stuff within the parenthesis will be compressed into a single expression
                                i++;
                                compressed.add(ast);
                                continue;
                            }

                        } else if (ast2 instanceof StringLitExpr) {
                            if(i + 2 >= ASTList.size()) {
                                throw new SyntaxException("Expected ) after (3");
                            }
                            AST ast3 = ASTList.get(i + 2);
                            if(ast3.firstToken.getKind() == IToken.Kind.RPAREN) {
                                //This is a primary_expr within a parenthesis, so it can be compressed from three ASTs to one
                                String s = ast2.firstToken.getTokenString();
                                int x = ast.firstToken.getSourceLocation().line();
                                int y = ast.firstToken.getSourceLocation().column();
                                IStringLitToken n = new IStringLitImplementation(s, "STRING_LIT", x, y);
                                NumLitExpr numExpr = new NumLitExpr(n);
                                i = i + 2;
                                same = false;
                                compressed.add(numExpr);
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
                    case PLUS,MINUS -> { //additive_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after additive expression");
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after additive expression");
                        }
                    }
                    case TIMES,DIV,MOD -> { //multiplicative_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
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
                System.out.println("Here");
                switch(ast2.firstToken.getKind()) {

                    case PLUS,MINUS -> { //additive_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after additive expression");
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;

                        } else {
                            throw new SyntaxException("Expected a primary expression after additive expression");
                        }
                    }
                    case TIMES,DIV,MOD -> { //multiplicative_expr
                        if(i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
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

                    case PLUS, MINUS -> {
                        if (i + 2 == ASTList.size()) {
                            throw new SyntaxException("Expected a primary expression after additive expression");
                        }
                        AST ast3 = ASTList.get(i + 2);
                        if (ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast.firstToken, (Expr) ast, ast2.firstToken.getKind(), (Expr) ast3);
                            compressed.add(binaryExpr);
                            i = i + 2;
                            same = false;
                            continue;
                        } else {
                            throw new SyntaxException("Expected a primary expression after additive expression");
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
    void parseMultiDivModulo(ArrayList<AST> compressed) throws SyntaxException {
        //ArrayLists in Java are passed by reference so we don't need to return anything
        //purpose of this is to parse multiplication, division, and modulo prior to addition and subtraction because
        //multiplication, division, and modulo have higher precedence than addition and subtraction
        //e.g 1 + 2 * 3 = 7 since 2 * 3 is evaluated first
        for(int i = 0; i < ASTList.size(); i++) {
            //Iterate through the ASTList for ZExpr (Multiplication, Division, Modulo)
            //If they are found, go back one and forward one and compress them into a BinaryExpr if possible
            //If not possible, throw a SyntaxException
            AST ast = ASTList.get(i);
            if(ast instanceof ZExpr || ast instanceof RandomExpr || ast instanceof IdentExpr || ast instanceof NumLitExpr || ast instanceof StringLitExpr) {
                if(ast.firstToken.getKind() == IToken.Kind.MOD || ast.firstToken.getKind() == IToken.Kind.DIV || ast.firstToken.getKind() == IToken.Kind.TIMES) {
                    //We have determined that ast is either a multiplication, division, or modulo expression
                    //Go back one and forward one and compress them into a BinaryExpr if possible
                    if(i == 0) {
                        throw new SyntaxException("Expected a primary expression before multiplicative expression");
                    }
                    if(i + 1 == ASTList.size()) {
                        throw new SyntaxException("Expected a primary expression after multiplicative expression");
                    }
                    AST ast2 = ASTList.get(i - 1);
                    AST ast3 = ASTList.get(i + 1);
                    if(ast2 instanceof RandomExpr || ast2 instanceof IdentExpr || ast2 instanceof NumLitExpr || ast2 instanceof StringLitExpr || ast2 instanceof UnaryExpr) {
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            //Place binaryExpr in original
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        } else if (ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    else if (ast2 instanceof BinaryExpr) {
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        } else if (ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    else {
                        throw new SyntaxException("Expected a primary expression before multiplicative expression");
                    }
                    //Remove the three elements from the ASTList and replace them with the BinaryExpr

                }
            }
            else if(ast instanceof UnaryExpr) {
                if(ast.firstToken.getKind() == IToken.Kind.MOD || ast.firstToken.getKind() == IToken.Kind.DIV || ast.firstToken.getKind() == IToken.Kind.TIMES) {
                    //We have determined that ast is either a multiplication, division, or modulo expression
                    //Go back one and forward one and compress them into a BinaryExpr if possible
                    if(i == 0) {
                        throw new SyntaxException("Expected a primary expression before multiplicative expression");
                    }
                    if(i + 1 == ASTList.size()) {
                        throw new SyntaxException("Expected a primary expression after multiplicative expression");
                    }
                    AST ast2 = ASTList.get(i - 1);
                    AST ast3 = ASTList.get(i + 1);
                    if(ast2 instanceof RandomExpr || ast2 instanceof IdentExpr || ast2 instanceof NumLitExpr || ast2 instanceof StringLitExpr || ast2 instanceof UnaryExpr) {
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            //Place binaryExpr in original
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        } else if (ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    else if (ast2 instanceof BinaryExpr) {
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        } else if (ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    else {
                        throw new SyntaxException("Expected a primary expression before multiplicative expression");
                    }
                    //Remove the three elements from the ASTList and replace them with the BinaryExpr

                }

            }
            else if(ast instanceof BinaryExpr) {
                if(ast.firstToken.getKind() == IToken.Kind.MOD || ast.firstToken.getKind() == IToken.Kind.DIV || ast.firstToken.getKind() == IToken.Kind.TIMES) {
                    //We have determined that ast is either a multiplication, division, or modulo expression
                    //Go back one and forward one and compress them into a BinaryExpr if possible
                    if(i == 0) {
                        throw new SyntaxException("Expected a primary expression before multiplicative expression");
                    }
                    if(i + 1 == ASTList.size()) {
                        throw new SyntaxException("Expected a primary expression after multiplicative expression");
                    }
                    AST ast2 = ASTList.get(i - 1);
                    AST ast3 = ASTList.get(i + 1);
                    if(ast2 instanceof RandomExpr || ast2 instanceof IdentExpr || ast2 instanceof NumLitExpr || ast2 instanceof StringLitExpr || ast2 instanceof UnaryExpr) {
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            //Place binaryExpr in original
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        } else if (ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    else if (ast2 instanceof BinaryExpr) {
                        if(ast3 instanceof RandomExpr || ast3 instanceof IdentExpr || ast3 instanceof NumLitExpr || ast3 instanceof StringLitExpr || ast3 instanceof UnaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        } else if (ast3 instanceof BinaryExpr) {
                            BinaryExpr binaryExpr = new BinaryExpr(ast2.firstToken, (Expr) ast2, ast.firstToken.getKind(), (Expr) ast3);
                            ASTList.set(i - 1, binaryExpr);
                            ASTList.remove(i);
                            ASTList.remove(i);
                        }
                        else {
                            throw new SyntaxException("Expected a primary expression after multiplicative expression");
                        }
                    }
                    else {
                        throw new SyntaxException("Expected a primary expression before multiplicative expression");
                    }
                    //Remove the three elements from the ASTList and replace them with the BinaryExpr

                }

            }

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

}
