package edu.ufl.cise.plcsp23.implementation;

import com.sun.tools.jconsole.JConsoleContext;
import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;

import java.io.Console;
import java.util.ArrayList;
import java.util.Objects;

public class IParserImplementation implements IParser {
    private ArrayList<AST> ASTList = new ArrayList<AST>();

    private ArrayList<IToken> tokenList = new ArrayList<IToken>();
    private int index = 0;

    private int parseReturnIndex = 0;
    @Override
    public AST parse() throws PLCException {
        if(ASTList.size() == 0) {
            throw new SyntaxException("No ASTs to parse");
        }
        return ASTList.get(parseReturnIndex++);
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
        getTokens(input);
        while(true) {
            AST ast = expr();
            if(ast == null) {
                break;
            }
            ASTList.add(ast);
        }
    }

    private Expr expr() throws PLCException {
        if(index > tokenList.size() - 1) {
            return null;
        }
        Expr expr = conditional_expr();
        if(expr == null) {
            expr = or_expr();
        }
        return expr;
    }

    private Expr conditional_expr() throws PLCException {
        if(index > tokenList.size() - 1) {
            return null;
        }
        IToken firstToken = tokenList.get(index);
        if(firstToken.getKind() == IToken.Kind.RES_if) {
            index++;
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("Expected an expression after if");
            }
            if(index > tokenList.size() - 1) {
                throw new SyntaxException("Expected a ? after the expression");
            }
            IToken question = tokenList.get(index);
            if(question.getKind() != IToken.Kind.QUESTION) {
                throw new SyntaxException("Expected a ? after the expression");
            }
            index++;
            Expr expr2 = expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected an expression after the ?");
            }
            if(index > tokenList.size() - 1) {
                throw new SyntaxException("Expected a ? after the expression");
            }
            IToken question2 = tokenList.get(index);
            if(question2.getKind() != IToken.Kind.QUESTION) {
                throw new SyntaxException("Expected a ? after the expression");
            }
            index++;
            Expr expr3 = expr();
            if(expr3 == null) {
                throw new SyntaxException("Expected an expression after the :");
            }
            return new ConditionalExpr(firstToken, expr, expr2, expr3);
        }
        return null;
    }

    private Expr or_expr() throws PLCException {
        Expr expr = and_expr();
        if(expr == null) {
            return null;
        }
        //check if there is a | or || after the first and_expr
        if(index > tokenList.size() - 1) {
            return expr;
        }

        //index works since the and_expr() method increments the index so that the next token is the one after the and_expr
        IToken op = tokenList.get(index);
        if(op.getKind() == IToken.Kind.OR || op.getKind() == IToken.Kind.BITOR) {
            index++;
            Expr expr2 = and_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected an expression after " + op.getKind());
            }
            return new BinaryExpr(expr.getFirstToken(),expr, op.getKind(), expr2);
        }
        return expr;
    }

    private Expr and_expr() throws PLCException {
        Expr expr = comparison_expr();
        if(expr == null) {
            return null;
        }
        //check if there is a & or && after the first comparison_expr
        if(index > tokenList.size() - 1) {
            return expr;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.AND || tokenList.get(index).getKind() == IToken.Kind.BITAND) {
            int i = index;
            index++;
            Expr expr2 = comparison_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
            }
            return new BinaryExpr(expr.getFirstToken(),expr, tokenList.get(i).getKind(), expr2);
        }
        return expr;
    }

    private Expr comparison_expr() throws PLCException {
        Expr expr = power_expr();
        if(expr == null) {
            return null;
        }
        //check if there is <,>,==,<=,>= after the first power_expr
        if(index > tokenList.size() - 1) {
            return expr;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.LT || tokenList.get(index).getKind() == IToken.Kind.GT || tokenList.get(index).getKind() == IToken.Kind.EQ || tokenList.get(index).getKind() == IToken.Kind.LE || tokenList.get(index).getKind() == IToken.Kind.GE) {
            index++;
            int i = index;
            Expr expr2 = power_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
            }
            return new BinaryExpr(expr.getFirstToken(),expr, tokenList.get(i).getKind(), expr2);
        }
        return expr;
    }

    private Expr power_expr() throws PLCException {
        Expr expr = additive_expr();
        if(expr == null) {
            return null;
        }
        //check if there is ** after the first additive_expr
        if(index > tokenList.size() - 1) {
            return expr;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.EXP) {
            IToken op = tokenList.get(index);
            index++;
            Expr expr2 = power_expr();
            if(expr2 == null) {
                expr2 = additive_expr();
                if(expr2 == null) {
                    throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
                }
            }
            return new BinaryExpr(expr.getFirstToken(),expr, op.getKind(), expr2);
        }
        return expr;
    }

    private Expr additive_expr() throws PLCException {
        Expr expr = multiplicative_expr();
        if(expr == null) {
            return null;
        }
        //check if there is + or - after the first multiplicative_expr
        if(index > tokenList.size() - 1) {
            return expr;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.PLUS || tokenList.get(index).getKind() == IToken.Kind.MINUS) {
            IToken op = tokenList.get(index);
            index++;

            Expr expr2 = multiplicative_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
            }
            return new BinaryExpr(expr.getFirstToken(),expr, op.getKind(), expr2);
        }
        return expr;

    }

    private Expr multiplicative_expr() throws PLCException {
        Expr expr = unary_expr();
        if(expr == null) {
            return null;
        }
        //check if there is *,/,% after the first unary_expr
        if(index > tokenList.size() - 1) {
            return expr;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.TIMES || tokenList.get(index).getKind() == IToken.Kind.DIV || tokenList.get(index).getKind() == IToken.Kind.MOD) {
            IToken op = tokenList.get(index);
            index++;
            Expr expr2 = unary_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
            }
            return new BinaryExpr(expr.getFirstToken(),expr, op.getKind(), expr2);
        }
        return expr;
    }

    private Expr unary_expr() throws PLCException {
        //check if there is a !,-,sin,cos,atan
        if(index > tokenList.size() - 1) {
            return null;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.BANG || tokenList.get(index).getKind() == IToken.Kind.MINUS || tokenList.get(index).getKind() == IToken.Kind.RES_sin || tokenList.get(index).getKind() == IToken.Kind.RES_cos || tokenList.get(index).getKind() == IToken.Kind.RES_atan) {
            IToken op = tokenList.get(index);
            int i = index;
            index++;
            Expr expr = unary_expr();
            if(expr == null) {
                throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
            }
            return new UnaryExpr(tokenList.get(i), op.getKind(), expr);
        }

        return primary_expr();
    }

    private Expr primary_expr() throws PLCException {
        //check if there is a (expr)
        if(index > tokenList.size() - 1) {
            return null;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.LPAREN) {
            index++;
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("Expected an expression after " + tokenList.get(index).getKind());
            }
            if(tokenList.get(index).getKind() != IToken.Kind.RPAREN) {
                throw new SyntaxException("Expected a ) after " + tokenList.get(index).getKind());
            }
            index++;
            return expr;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.IDENT) {
            index++;
            return new IdentExpr(tokenList.get(index - 1));
        }
        if(tokenList.get(index).getKind() == IToken.Kind.NUM_LIT) {
            index++;
            //Create
            int x = tokenList.get(index - 1).getSourceLocation().line();
            int y = tokenList.get(index - 1).getSourceLocation().column();
            String n = tokenList.get(index - 1).getTokenString();
            INumLitToken numLitToken = new INumLitImplementation(n, "NUM_LIT", x, y);
            return new NumLitExpr(numLitToken);
        }
        if(tokenList.get(index).getKind() == IToken.Kind.STRING_LIT) {
            index++;
            int x = tokenList.get(index - 1).getSourceLocation().line();
            int y = tokenList.get(index - 1).getSourceLocation().column();
            String n = tokenList.get(index - 1).getTokenString();
            IStringLitToken stringLitToken = new IStringLitImplementation(n, "STRING_LIT", x, y);
            return new StringLitExpr(stringLitToken);
        }
        if(tokenList.get(index).getKind() == IToken.Kind.RES_rand) {
            index++;
            return new RandomExpr(tokenList.get(index - 1));
        }
        //check for invalid token
        if(tokenList.get(index).getKind() == IToken.Kind.EOF) {
            return null;
        }
        if(tokenList.get(index).getKind() == IToken.Kind.RPAREN) {
            throw new SyntaxException("Expected an expression before " + tokenList.get(index).getKind());
        }
        checkInvalidFallThrough();
        index++;
        return new ZExpr(tokenList.get(index-1));
    }

    boolean checkInvalidFallThrough() {
        //this is for a base ZExpr to check to see if there's any invalid tokens
        return tokenList.get(index).getKind() == IToken.Kind.DOT || tokenList.get(index).getKind() == IToken.Kind.COMMA || tokenList.get(index).getKind() == IToken.Kind.QUESTION || tokenList.get(index).getKind() == IToken.Kind.COLON || tokenList.get(index).getKind() == IToken.Kind.EQ || tokenList.get(index).getKind() == IToken.Kind.ASSIGN || tokenList.get(index).getKind() == IToken.Kind.EXCHANGE || tokenList.get(index).getKind() == IToken.Kind.LE || tokenList.get(index).getKind() == IToken.Kind.GE || tokenList.get(index).getKind() == IToken.Kind.BANG || tokenList.get(index).getKind() == IToken.Kind.BITAND || tokenList.get(index).getKind() == IToken.Kind.AND || tokenList.get(index).getKind() == IToken.Kind.BITOR || tokenList.get(index).getKind() == IToken.Kind.OR || tokenList.get(index).getKind() == IToken.Kind.PLUS || tokenList.get(index).getKind() == IToken.Kind.MINUS || tokenList.get(index).getKind() == IToken.Kind.TIMES || tokenList.get(index).getKind() == IToken.Kind.EXP || tokenList.get(index).getKind() == IToken.Kind.DIV || tokenList.get(index).getKind() == IToken.Kind.MOD || tokenList.get(index).getKind() == IToken.Kind.ERROR;
    }


    private void getTokens(String input) throws PLCException {
        IScannerImplementation scanner = new IScannerImplementation(input);

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
    }
}
