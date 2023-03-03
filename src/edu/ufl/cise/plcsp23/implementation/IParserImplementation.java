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

    }

    private Expr expr() throws PLCException {
        Expr expr = conditional_expr();
        if(expr == null) {
            expr = or_expr();
        }
        return expr;
    }

    private Expr conditional_expr() throws PLCException {
        //conditional expr = if expr ? expr ? expr
        if(tokenList.get(index).getKind() == IToken.Kind.RES_if) {
            IToken firstToken = tokenList.get(index);
            index++;
            Expr expr1 = expr();
            if(expr1 == null) {
                throw new SyntaxException("Expected expression after if keyword");
            }
            if(tokenList.get(index).getKind() != IToken.Kind.QUESTION) {
                throw new SyntaxException("Expected ? after expr");
            }
            index++;
            Expr expr2 = expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expr after ?");
            }
            if(tokenList.get(index).getKind() != IToken.Kind.QUESTION) {
                throw new SyntaxException("Expected ? after expr");
            }
            index++;
            Expr expr3 = expr();
            if(expr3 == null) {
                throw new SyntaxException("Expected expr after :");
            }
            return new ConditionalExpr(firstToken, expr1, expr2, expr3);
        }
        return null;
    }

    private void getTokens(String input) throws PLCException {
        while(true) {
            IScannerImplementation scanner = new IScannerImplementation(input);
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
