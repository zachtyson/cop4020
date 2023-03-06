package edu.ufl.cise.plcsp23.implementation;

import com.sun.tools.jconsole.JConsoleContext;
import edu.ufl.cise.plcsp23.*;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.Dimension;

import javax.swing.plaf.nimbus.State;
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
        try {
            while(notFinished) {
                ASTList.add(program());
                if(index > tokenList.size() - 1) {
                    notFinished = false;
                }
            }
        } catch (PLCException e) {
            throw new SyntaxException(e.getMessage());
        }
    }

    private Program program() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        Type type = type();
        if(type == null) {
            throw new SyntaxException("Program must have a type");
            //If there's no type then it's not a program
        }
        Ident ident = ident();
        //If there's no ident then it's not a program
        if(ident == null) {
            throw new SyntaxException("Program must have an identifier");
            //Honestly not sure if I should be throwing exceptions yet 'cause I haven't read the whole assignment
        }
        consume(IToken.Kind.LPAREN);
        ArrayList<NameDef> paramList = param_list();
        consume(IToken.Kind.RPAREN);

        //After this it's a block, so we'll call block()
        Block block = block();

        return null;
    }

    private Type type() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        //A type can either be : int, pixel, image, string, or void
        //If it is not one of these, then it is not a type
        if(match_kind(IToken.Kind.RES_int, IToken.Kind.RES_pixel, IToken.Kind.RES_image, IToken.Kind.RES_string, IToken.Kind.RES_void)) {
            return Type.getType(previous());
        }
        return null;
    }

    private Ident ident() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        if(match_kind(IToken.Kind.IDENT)) {
            return new Ident(previous());
        }
        return null;
    }

    private ArrayList<NameDef> param_list() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        ArrayList<NameDef> paramList = new ArrayList<NameDef>();
        //A name def is either A: a type followed by an ident or B: a type followed by a dimension followed by an ident
        do {
            NameDef nameDef = name_def();
            if(nameDef == null) {
                break;
            }
            paramList.add(nameDef);
        } while(match_kind(IToken.Kind.COMMA)); //do while loop since we need to check for at least one name def which wouldn't have a comma before it
        return paramList;
    }

    private Block block() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        //a block is a list of declarations followed by a list of statements
        IToken firstToken = current();
        consume(IToken.Kind.LCURLY); //We know it's a block if it starts with a left curly brace
        //declarations and statements are separated by periods (.), personally a weird choice but whatever
        ArrayList<Declaration> decList = new ArrayList<Declaration>();
        decList = declaration_list();
        ArrayList<Statement> stmtList = new ArrayList<Statement>();
        stmtList = statement_list();
        return new Block(firstToken, decList, stmtList);

    }

    private ArrayList<Statement> statement_list() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        ArrayList<Statement> stmtList = new ArrayList<Statement>();
        while(true) {
            //try to get a new statement, if it's null then we're done, this could technically just be an empty list
            Statement stmt = statement();
            if(stmt == null) {
                break;
            }
            //There is a period after each statement, similar to how there is a semicolon after each statement in many languages
            consume(IToken.Kind.DOT);
            stmtList.add(stmt);
        }
        return stmtList;
    }

    private Statement statement() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        //A statement can be one of the three following:
        //A: LValue = Expr
        //B: write Expr
        //C: while Expr block
        //so step one should be to check for each of these
        //ugh that means I have to implement LValue
        //It's pretty nice that like 90% of the code just sorta happens naturally, esp compared to the Scanner so I shouldn't complain
        LValue lvalue = lvalue();
        if(lvalue != null) {
            //If it's an LValue it's an assignment statement
            consume(IToken.Kind.ASSIGN);
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("Assignment statement must have an expression");
                //Honestly not even sure if expr can be null here, it might be previously caught somewhere in expr()
            }
            return new AssignmentStatement(lvalue.getFirstToken(),lvalue, expr);
        }
        else if(match_kind(IToken.Kind.RES_write)) {
            //If it's write then it's a write statement
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("Write statement must have an expression");
            }
            return new WriteStatement(expr.getFirstToken(), expr);
        }
        else if(match_kind(IToken.Kind.RES_while)) {
            //If it's while then it's a while statement
            Expr expr = expr();
            if(expr == null) {
                throw new SyntaxException("While statement must have an expression");
            }
            Block block = block();
            if(block == null) {
                throw new SyntaxException("While statement must have a block");
            }
            return new WhileStatement(expr.getFirstToken(), expr, block);
        }
        return null;
    }

    private LValue lvalue() throws PLCException {
        //LValue::= IDENT (PixelSelector|Epsilon) (ColorChannel|Epsilon)
        //The grammar says ChannelSelector, but it's not in the given code so I'm assuming it's supposed to be ColorChannel
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        if(match_kind(IToken.Kind.IDENT)) {
            IToken firstToken = previous();
            Ident ident = new Ident(firstToken);
            PixelSelector pixelSelector = pixel_selector();
            ColorChannel channelSelector = channel_selector();
            return new LValue(firstToken,ident, pixelSelector, channelSelector);
        }
        return null;
    }

    private PixelSelector pixel_selector() throws PLCException {
        //PixelSelector::= [Expr,Expr] //basically the same as a pixel selector,but with one less expression
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        //It's a name def B
        IToken firstToken = current();
        consume(IToken.Kind.LSQUARE);
        Expr expr = expr();
        if(expr == null) {
            throw new SyntaxException("Expected an expression in dimension");
        }
        //Should be like [expr,expr]
        consume(IToken.Kind.COMMA);
        Expr expr2 = expr();
        if(expr2 == null) {
            throw new SyntaxException("Expected an expression in dimension");
        }
        consume(IToken.Kind.RSQUARE);
        return new PixelSelector(firstToken,expr,expr2);
    }

    private ColorChannel channel_selector() throws PLCException {
        //ChannelSelector::= : (r|g|b)
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        if(match_kind(IToken.Kind.COLON)) {
            if(match_kind(IToken.Kind.RES_red, IToken.Kind.RES_grn, IToken.Kind.RES_blu)) {
                return ColorChannel.getColor((previous()));
            }
        }
        return null;
    }



    private ArrayList<Declaration> declaration_list() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        ArrayList<Declaration> decList = new ArrayList<Declaration>();
        while(true) {
            //try to get a new declaration, if it's null then we're done
            Declaration dec = declaration();
            if(dec == null) {
                break;
            }
            //There is a period after each declaration, similar to how there is a semicolon after each statement in many languages
            consume(IToken.Kind.DOT);
            decList.add(dec);
        }
        return decList;
    }

    private Declaration declaration() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        //A declaration is either just a name def or a name def followed by an assignment (=Expr)
        NameDef nameDef = name_def();
        if(nameDef == null) {
            return null;
        }
        if(match_kind(IToken.Kind.ASSIGN)) {
            Expr expr = expr();
            if (expr == null) {
                throw new SyntaxException("Expected an expression after =");
            }
            return new Declaration(nameDef.firstToken,nameDef, expr);
        }
        return new Declaration(nameDef.firstToken,nameDef, null);
    }

    private NameDef name_def() throws PLCException {
        if(!match_kind(IToken.Kind.RES_int, IToken.Kind.RES_pixel, IToken.Kind.RES_image, IToken.Kind.RES_string, IToken.Kind.RES_void)) {
            return null;
        }
        //That gets the type
        IToken token = previous();
        Type type = Type.getType(previous());
        //Now we need to check if it's a name def A or B
        if(match_kind(IToken.Kind.IDENT)) {
            //It's a name def A
            Ident ident = new Ident(previous());
            return new NameDef(token,type,null,ident);
        }
        else if(current().getKind().equals(IToken.Kind.LSQUARE)) {
            Dimension dimension = dimension();
            return new NameDef(token,type,dimension,new Ident(previous()));
        }
        else {
            throw new SyntaxException("Expected an identifier");
        }
    }

    private Dimension dimension() throws PLCException {
        if(index > tokenList.size() - 1) {
            notFinished = false;
            return null;
        }
        //It's a name def B
        IToken firstToken = current();
        consume(IToken.Kind.LSQUARE);
        Expr expr = expr();
        if(expr == null) {
            throw new SyntaxException("Expected an expression in dimension");
        }
        //Should be like [expr,expr]
        consume(IToken.Kind.COMMA);
        Expr expr2 = expr();
        if(expr2 == null) {
            throw new SyntaxException("Expected an expression in dimension");
        }
        consume(IToken.Kind.RSQUARE);
        return new Dimension(firstToken,expr,expr2);
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

    private IToken consume(IToken.Kind... kind) throws PLCException {
        if(index > tokenList.size() - 1) {
            throw new SyntaxException("Unexpected end of input");
        }
        for(IToken.Kind k : kind) {
            if(check(k)) {
                return advance();
            }
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
