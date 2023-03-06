package edu.ufl.cise.plcsp23.implementation;

import com.sun.tools.jconsole.JConsoleContext;
import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;

import java.awt.*;
import java.io.Console;
import java.util.ArrayList;
import java.util.Objects;

public class IParserImplementation implements IParser {
    private ArrayList<AST> ASTList = new ArrayList<AST>();

    private ArrayList<IToken> tokenList = new ArrayList<IToken>();

    private int index = 0;

    boolean notFinished = true;

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
        if(input == null || input.isEmpty()) {
            throw new SyntaxException("Input is null or empty");
        }
        //Convert ArrayList of tokens to ArrayList of ASTs
        getTokens(input);
        while(notFinished) {
            ASTList.add(expr());
            if(index > tokenList.size() - 1) {
                notFinished = false;
            }
        }
    }

    private Expr expr() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        Expr expr = conditional_expr();
        if(expr == null) {
            expr = or_expr();
        }
        return expr;
    }

    private Expr conditional_expr() throws PLCException {
        if(match_kind(IToken.Kind.RES_if)) {
            Expr expr1 = expr();
            if(match_kind(IToken.Kind.QUESTION)) {
                Expr expr2 = expr();
                if(match_kind(IToken.Kind.QUESTION)) {
                    Expr expr3 = expr();
                    return new ConditionalExpr(previous(), expr1, expr2, expr3);
                }
                else {
                    throw new SyntaxException("Expected '?'");
                }
            }
            else {
                throw new SyntaxException("Expected '?'");
            }
        }
        return null;
    }

    private Expr or_expr() throws PLCException {
        Expr expr = and_expr();
        while(match_kind(IToken.Kind.OR, IToken.Kind.BITOR)) {
            IToken.Kind kind = previous().getKind();
            expr = new BinaryExpr(previous(), expr, kind, and_expr());
        }
        return expr;
    }

    private Expr and_expr() throws PLCException {
        Expr expr = comparison_expr();
        while(match_kind(IToken.Kind.AND, IToken.Kind.BITAND)) {
            IToken.Kind kind = previous().getKind();
            expr =  new BinaryExpr(previous(), expr, kind, comparison_expr());
        }
        return expr;
    }

    private Expr comparison_expr() throws PLCException {
        Expr expr = power_expr();
        while(match_kind(IToken.Kind.EQ, IToken.Kind.LT, IToken.Kind.GT, IToken.Kind.LE, IToken.Kind.GE)) {
            IToken.Kind kind = previous().getKind();
            expr  = new BinaryExpr(previous(), expr, kind, power_expr());
        }
        return expr;
    }

    private Expr power_expr() throws PLCException {
        Expr expr = additive_expr();
        while(match_kind(IToken.Kind.EXP)) {
            IToken.Kind kind = previous().getKind();
            expr = new BinaryExpr(previous(), expr, kind, power_expr());
        }
        return expr;
    }

    private Expr additive_expr() throws PLCException {
        Expr expr = multiplicative_expr();
        while(match_kind(IToken.Kind.PLUS, IToken.Kind.MINUS)) {
            IToken.Kind kind = previous().getKind();
            expr = new BinaryExpr(previous(), expr, kind, multiplicative_expr());
        }
        return expr;
    }

    private Expr multiplicative_expr() throws PLCException {
        Expr expr = unary_expr();
        while(match_kind(IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD)) {
            IToken.Kind kind = previous().getKind();
            expr = new BinaryExpr(previous(), expr, kind, unary_expr());
        }
        return expr;
    }

    private Expr unary_expr() throws PLCException {
        while(match_kind(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_sin, IToken.Kind.RES_cos, IToken.Kind.RES_atan)) {
            IToken.Kind kind = previous().getKind();
            return new UnaryExpr(previous(), kind, unary_expr());
        }
        return primary_expr();
    }

    private Expr primary_expr() throws PLCException {
        IToken token = current();
        IToken.Kind k = token.getKind();
        if(k == IToken.Kind.NUM_LIT) {
            int x = token.getSourceLocation().line();
            int y = token.getSourceLocation().column();
            String n = token.getTokenString();
            INumLitToken numLitToken = new INumLitImplementation(n, "NUM_LIT", x, y);
            consume(IToken.Kind.NUM_LIT);
            return new NumLitExpr(numLitToken);
        }
        else if(k == IToken.Kind.IDENT) {
            consume(IToken.Kind.IDENT);
            return new IdentExpr(token);
        }
        else if (k == IToken.Kind.RES_rand) {
            consume(IToken.Kind.RES_rand);
            return new RandomExpr(token);
        }
        else if (k == IToken.Kind.STRING_LIT) {
            int x = token.getSourceLocation().line();
            int y = token.getSourceLocation().column();
            String n = token.getTokenString();
            IStringLitToken stringLitToken = new IStringLitImplementation(n, "STRING_LIT", x, y);
            consume(IToken.Kind.STRING_LIT);
            return new StringLitExpr(stringLitToken);
        }
        else if (k == IToken.Kind.LPAREN) {
            consume(IToken.Kind.LPAREN);
            Expr expr = expr();
            consume(IToken.Kind.RPAREN);
            return expr;
        }
        else if (match_kind(IToken.Kind.OR, IToken.Kind.BITOR, IToken.Kind.AND, IToken.Kind.BITAND, IToken.Kind.LE,
                IToken.Kind.GE, IToken.Kind.GT, IToken.Kind.LT, IToken.Kind.EQ, IToken.Kind.EXP, IToken.Kind.PLUS,
                IToken.Kind.MINUS, IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD, IToken.Kind.BANG, IToken.Kind.QUESTION) ) {
            throw new SyntaxException("Unexpected token: " + current().getTokenString());
        }
        else if (k == IToken.Kind.RPAREN|| k == IToken.Kind.RSQUARE || k == IToken.Kind.RCURLY) {
            throw new SyntaxException("Unexpected token: " + current().getTokenString());
        }
        else {
            consume(token.getKind());
            return new ZExpr(token);
        }
    }

    private boolean match_kind(IToken.Kind... kinds) {
        for(IToken.Kind kind : kinds) {
            if(check(kind)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private IToken consume(IToken.Kind kind) throws PLCException {
        if(index > tokenList.size() - 1) {
            throw new SyntaxException("Unexpected end of input");
        }
        if(check(kind)) {
            return advance();
        }
        throw new SyntaxException("Unexpected token: " + current().getTokenString());
    }

    private IToken advance() {
        if(index < tokenList.size()) {
            index++;
        }
        return previous();
    }

    private boolean check(IToken.Kind kind) {
        if(index > tokenList.size() - 1) {
            return false;
        }
        return tokenList.get(index).getKind() == kind;
    }

    private IToken previous() {
        return tokenList.get(index - 1);
    }

    private IToken current() throws SyntaxException {
        if(index > tokenList.size() - 1) {
            throw new SyntaxException("Unexpected end of input, expected token");
        }
        return tokenList.get(index);
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
