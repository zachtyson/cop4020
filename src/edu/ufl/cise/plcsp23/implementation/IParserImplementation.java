package edu.ufl.cise.plcsp23.implementation;

import com.sun.tools.jconsole.JConsoleContext;
import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;

import java.io.Console;
import java.util.ArrayList;
import java.util.Objects;

public class IParserImplementation implements IParser {
    private ArrayList<AST> ASTList = new ArrayList<AST>();
    private ArrayList<AST> tempASTList = new ArrayList<AST>();


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
        //Convert ArrayList of tokens to ArrayList of ASTs
        getTokens(input);
        for(IToken token : tokenList) {
            if(token.getKind() == IToken.Kind.NUM_LIT) {
                int x = token.getSourceLocation().line();
                int y = token.getSourceLocation().column();
                String n = token.getTokenString();
                INumLitToken numLitToken = new INumLitImplementation(n, "NUM_LIT", x, y);
                NumLitExpr numLitExpr = new NumLitExpr(numLitToken);
                ASTList.add(numLitExpr);
            }
            else if(token.getKind() == IToken.Kind.IDENT) {
                IdentExpr identExpr = new IdentExpr(token);
                ASTList.add(identExpr);
            }
            else if (token.getKind() == IToken.Kind.RES_rand) {
                RandomExpr randomExpr = new RandomExpr(token);
                ASTList.add(randomExpr);
            }
            else if (token.getKind() == IToken.Kind.STRING_LIT) {
                int x = token.getSourceLocation().line();
                int y = token.getSourceLocation().column();
                String n = token.getTokenString();
                IStringLitToken stringLitToken = new IStringLitImplementation(n, "STRING_LIT", x, y);
                StringLitExpr stringLitExpr = new StringLitExpr(stringLitToken);
                ASTList.add(stringLitExpr);
            }
            else {
                ZExpr zExpr = new ZExpr(token);
                ASTList.add(zExpr);
            }
        }
        int iteration = 0;
        do {
            iteration++;
            AST ast = expr();
            if(ast == null) {
                break;
            }
            tempASTList.add(ast);
        } while (compareASTLists());
        System.out.println("Iterations: " + iteration);
        System.out.println(ASTList.size());
        for(AST ast : ASTList) {
            System.out.println(ast.getFirstToken().getTokenString());
        }
        //Iterate over ASTList and see if any ASTs can be reduced
        //If they can, reduce them
        //If they can't, move on to the next one

    }

    private boolean compareASTLists() {
        if(ASTList.size() == tempASTList.size()) {
            return false;
        }
        ASTList = tempASTList;
        index = 0;
        return true;
    }

    private Expr expr() throws PLCException {
        if(index > ASTList.size() - 1) {
            return null;
        }
        Expr expr = conditional_expr();
        if(expr == null) {
            expr = or_expr();
        }
        return expr;
    }

    private Expr conditional_expr() throws PLCException {
        if(index > ASTList.size() - 1) {
            return null;
        }
        AST ifexpr = ASTList.get(index);
        if(ifexpr.getFirstToken().getKind() == IToken.Kind.RES_if) {
            if(ifexpr instanceof ConditionalExpr) {
                return null;
            }
            index++;
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("Expected expression1 after " + ifexpr.getFirstToken().getTokenString());
            }
            if(index > ASTList.size() - 1) {
                throw new SyntaxException("Expected ? after " + ifexpr.getFirstToken().getTokenString());
            }
            Expr question1 = (Expr)ASTList.get(index);
            if(question1.getFirstToken().getKind() != IToken.Kind.QUESTION) {
                //Add to list later since they may be able to be reduced but not now
                tempASTList.add(ifexpr);
                return question1;
            }
            index++;
            Expr expr2 = expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression2 after " + question1.getFirstToken().getTokenString());
            }
            if(index > ASTList.size() - 1) {
                throw new SyntaxException("Expected ? after " + question1.getFirstToken().getTokenString());
            }
            Expr question2 = (Expr)ASTList.get(index);
            if(question2.getFirstToken().getKind() != IToken.Kind.QUESTION) {
                tempASTList.add(ifexpr);
                tempASTList.add(question1);
                tempASTList.add(expr2);
                return question2;
            }
            index++;
            Expr expr3 = expr();
            if(expr3 == null) {
                throw new SyntaxException("Expected expression after " + question2.getFirstToken().getTokenString());
            }
            return new ConditionalExpr(ifexpr.getFirstToken(),expr,expr2,expr3);
        }
        return null;
    }

    private Expr or_expr() throws PLCException {
        Expr expr = and_expr();
        if(expr == null) {
            return null;
        }
        if(index > ASTList.size() - 1) {
            return expr;
        }

        Expr op = (Expr)ASTList.get(index);
        if(op.getFirstToken().getKind() == IToken.Kind.OR || op.getFirstToken().getKind() == IToken.Kind.BITOR) {
            index++;
            Expr expr2 = and_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + op.getFirstToken().getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,op.getFirstToken().getKind(),expr2);
        }
        return expr;
    }

    private Expr and_expr() throws PLCException {
        Expr expr = comparison_expr();
        if(expr == null) {
            return null;
        }
        if(index > ASTList.size() - 1) {
            return expr;
        }

        Expr op = (Expr)ASTList.get(index);
        if(op.getFirstToken().getKind() == IToken.Kind.AND) {
            index++;
            Expr expr2 = comparison_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + op.getFirstToken().getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,op.getFirstToken().getKind(),expr2);
        }
        if(op.getFirstToken().getKind() == IToken.Kind.BITAND) {
            index++;
            Expr expr2 = comparison_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + op.getFirstToken().getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,op.getFirstToken().getKind(),expr2);
        }
        return expr;
    }

    private Expr comparison_expr() throws PLCException {
        Expr expr = power_expr();
        if(expr == null) {
            return null;
        }
        if(index > ASTList.size() - 1) {
            return expr;
        }
        AST ast = ASTList.get(index);
        if(ast.firstToken.getKind() == IToken.Kind.EQ || ast.firstToken.getKind() == IToken.Kind.LT || ast.firstToken.getKind() == IToken.Kind.GT || ast.firstToken.getKind() == IToken.Kind.LE || ast.firstToken.getKind() == IToken.Kind.GE) {
            index++;
            Expr expr2 = power_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + ast.firstToken.getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,ast.firstToken.getKind(),expr2);
        }
        return expr;
    }

    private Expr power_expr() throws PLCException {
        Expr expr = additive_expr();
        if(expr == null) {
            return null;
        }
        if(index > ASTList.size() - 1) {
            return expr;
        }
        AST ast = ASTList.get(index);
        if(ast.firstToken.getKind() == IToken.Kind.EXP) {
            index++;
            Expr expr2 = additive_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + ast.firstToken.getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,ast.firstToken.getKind(),expr2);
        }
        return expr;
    }

    private Expr additive_expr() throws PLCException {
        Expr expr = multiplicative_expr();
        if(expr == null) {
            return null;
        }
        if(index > ASTList.size() - 1) {
            return expr;
        }
        AST ast = ASTList.get(index);
        if(ast.firstToken.getKind() == IToken.Kind.PLUS || ast.firstToken.getKind() == IToken.Kind.MINUS) {
            index++;
            Expr expr2 = multiplicative_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + ast.firstToken.getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,ast.firstToken.getKind(),expr2);
        }
        return expr;
    }

    private Expr multiplicative_expr() throws PLCException {
        Expr expr = unary_expr();
        if(expr == null) {
            return null;
        }
        if(index > ASTList.size() - 1) {
            return expr;
        }
        AST ast = ASTList.get(index);
        if(ast.firstToken.getKind() == IToken.Kind.TIMES || ast.firstToken.getKind() == IToken.Kind.DIV || ast.firstToken.getKind() == IToken.Kind.MOD) {
            index++;
            Expr expr2 = unary_expr();
            if(expr2 == null) {
                throw new SyntaxException("Expected expression after " + ast.firstToken.getTokenString());
            }
            return new BinaryExpr(expr.getFirstToken(),expr,ast.firstToken.getKind(),expr2);
        }
        return expr;
    }

    private Expr unary_expr() throws PLCException {
        if(index > ASTList.size() - 1) {
            return null;
        }
        AST ast = ASTList.get(index);
        IToken.Kind kind = ast.firstToken.getKind();
        if(kind == IToken.Kind.BANG || kind == IToken.Kind.MINUS || kind == IToken.Kind.RES_sin || kind == IToken.Kind.RES_cos || kind == IToken.Kind.RES_atan) {
            index++;
            Expr expr = unary_expr();
            if(expr == null) {
                return primary_expr();
            }
            return new UnaryExpr(ast.firstToken,kind,expr);
        }
        return primary_expr();
    }

    private Expr primary_expr() throws PLCException {
        if(index > ASTList.size() - 1) {
            return null;
        }
        System.out.println("SUP" + ASTList.get(index).firstToken.getTokenString());
        AST ast = ASTList.get(index);
        index++;
        if(ast.getFirstToken().getKind() == IToken.Kind.LPAREN) {
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("Expected expression after " + ast.firstToken.getTokenString());
            }
            if(index > ASTList.size() - 1) {
                throw new SyntaxException("Expected ) after expression");
            }
            index++;
            return expr;
        }
        if(ast.getFirstToken().getKind() == IToken.Kind.NUM_LIT) {
            IToken token = ast.getFirstToken();
            int x = token.getSourceLocation().line();
            int y = token.getSourceLocation().column();
            String n = token.getTokenString();
            INumLitToken numLitToken = new INumLitImplementation(n, "NUM_LIT", x, y);
            return new NumLitExpr(numLitToken);
        }
        if(ast.getFirstToken().getKind() == IToken.Kind.STRING_LIT) {
            IToken token = ast.getFirstToken();
            int x = token.getSourceLocation().line();
            int y = token.getSourceLocation().column();
            String n = token.getTokenString();
            IStringLitToken stringLitToken = new IStringLitImplementation(n, "STRING_LIT", x, y);
            return new StringLitExpr(stringLitToken);
        }
        if(ast.getFirstToken().getKind() == IToken.Kind.IDENT) {
            IToken token = ast.getFirstToken();
            return new IdentExpr(token);
        }
        if(ast.getFirstToken().getKind() == IToken.Kind.RES_rand) {
            return new RandomExpr(ast.getFirstToken());
        }
        if(ast.getFirstToken().getKind() == IToken.Kind.EOF) {
            return null;
        }
        if(ast.getFirstToken().getKind() == IToken.Kind.RPAREN) {
            throw new SyntaxException("Expected expression before " + ast.firstToken.getTokenString());
        }
        return new ZExpr(ast.getFirstToken());
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
