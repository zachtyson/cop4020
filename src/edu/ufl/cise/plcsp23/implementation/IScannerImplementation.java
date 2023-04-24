package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.INumLitToken;
import edu.ufl.cise.plcsp23.IScanner;
import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.LexicalException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static edu.ufl.cise.plcsp23.IToken.Kind.*;
import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;

public class IScannerImplementation implements IScanner {
    private int position = 0;

    private int start = 0;

    private int line = 1;

    private int column = 1;

    private int next_value = 0;
    private ArrayList<IToken> tokens = new ArrayList<IToken>();
    //Scanner has an array of ITokens, and a position variable for the next() method



    @Override
    public IToken next() throws LexicalException {
        IToken t = tokens.get(next_value);
        if(t.getKind() == EOF) {
            return t;
        }
        if(t.getKind() == ERROR) {
            throw new LexicalException("Invalid token at line " + t.getSourceLocation().line() + " and column " + t.getSourceLocation().column() + ": " + t.getTokenString());
        }
        if(t.getKind() == NUM_LIT) {
            INumLitToken n = (INumLitImplementation) t;
            if(((INumLitImplementation) n).getValueTest() == null) {
                throw new LexicalException("Invalid token at line " + t.getSourceLocation().line() + " and column " + t.getSourceLocation().column() + ": " + t.getTokenString());
            }
            next_value++;
            return t;
        }
        next_value++;
        return t;

    }

    private String input;

    private int k;

    private HashMap<String,IToken.Kind> reserved;

    public IScannerImplementation(String input) throws LexicalException {
        add_reserved();
        this.input = input;
        while(!isAtEnd()) {
            start = position;
            scanToken();
        }
        tokens.add(new ITokenImplementation("", EOF, 0, 0));

    }

    private void scanToken() throws LexicalException {
        k = column;
        char c = advance();
        switch (c) {
            case '.' -> addToken(".", DOT, line, k);
            case ',' -> addToken(",", COMMA, line, k);
            case '?' -> addToken("?", QUESTION, line, k);
            case ':' -> addToken(":", COLON, line, k);
            case '(' -> addToken("(", LPAREN, line, k);
            case ')' -> addToken(")", RPAREN, line, k);
            case '[' -> addToken("[", LSQUARE, line, k);
            case ']' -> addToken("]", RSQUARE, line, k);
            case '{' -> addToken("{", LCURLY, line, k);
            case '}' -> addToken("}", RCURLY, line, k);
            case '=' -> {
                if (match('=')) {
                    addToken("==", EQ, line, k);
                } else {
                    addToken("=", ASSIGN, line, k);
                }
            }
            case '<' -> {
                if (match('-')) {
                    if (match('>')) {
                        addToken("<->", EXCHANGE, line, k);
                    } else {
                        addToken("<-", ERROR, line, k);
                    }
                } else if (match('=')) {
                    addToken("<=", LE, line, k);
                } else {
                    addToken("<", LT, line, k);
                }
            }
            case '>' -> {
                if (match('=')) {
                    addToken(">=", GE, line, k);
                } else {
                    addToken(">", GT, line, k);
                }
            }
            case '!' -> addToken("!", BANG, line, k);
            case '&' -> {
                if (match('&')) {
                    addToken("&&", AND, line, k);
                } else {
                    addToken("&", BITAND, line, k);
                }
            }
            case '|' -> {
                if (match('|')) {
                    addToken("||", OR, line, k);
                } else {
                    addToken("|", BITOR, line, k);
                }
            }
            case '+' -> addToken("+", PLUS, line, k);
            case '-' -> addToken("-", MINUS, line, k);
            case '*' -> {
                if (match('*')) {
                    addToken("**", EXP, line, k);
                } else {
                    addToken("*", TIMES, line, k);
                }
            }
            case '/' -> addToken("/", DIV, line, k);
            case '%' -> addToken("%", MOD, line, k);
            case ' ', '\t', '\r','\b','\f' -> {
            }
            case '\n' -> {
                line++;
                column = 1;
            }
            case '"' -> string_lit();
            case '\\' -> {
                if (match('n','t','r','b','f','\\' ,'"')) {
                    //do nothing
                }
                else {
                    addToken(c+ "", ERROR, line, k);
                }
            }
            case '~' -> {
                //this signifies a comment, so we continue until we reach the end of the line
                while(peek() != '\n' && !isAtEnd()) {
                    advance();
                }
            }
            default -> {
                if(isDigit(c)) {
                    number(c);
                }
                else if(isAlphabetic(c) || c == '_') {
                    identifier();
                }
                else {
                    addToken(c+ "", ERROR, line, k);
                }
            }
        }
    }

    private void string_lit() throws LexicalException {
        while(peek() != '"' && !isAtEnd()) {
            if(peek() == '\n') {
                line++;
                column = 1;
            }
            if(peek() == '\\') {
                advance();
            }
            advance();
        }
        if(isAtEnd()) {
            addToken(input.substring(start, position), ERROR, line, k);
            return;
        }
        advance(); //last "
        addToken(input.substring(start, position), STRING_LIT, line, k);
    }

    private void identifier() throws LexicalException {
        start = position - 1;
        while(isAlphabetic(peek()) || isDigit(peek()) || peek() == '_') {
            advance();
        }
        String text = input.substring(start, position);
        //check for reserved words
        addToken(text, reserved.getOrDefault(text, IDENT), line, k);

    }

    private void number(char c) throws LexicalException {
        start = position - 1;
        if(c == '0') { //a num_lit can't start with 0 unless it's 0
            addToken("0", NUM_LIT, line, k);
            return;
        }
        while(isDigit(peek())) {
            advance();
        }
        addToken(input.substring(start, position), NUM_LIT, line, k);
    }

    private char peek() {
        if(isAtEnd()) {
            return '\0'; //can't return null because char can't be null
        }
        return input.charAt(position);
    }

    private boolean isAtEnd() {
        return position >= input.length();
    }

    private char advance() {
        position++;
        column++;
        return input.charAt(position - 1);
    }

    private void addToken(String tokenString, IToken.Kind kind, int line, int column) throws LexicalException {
        if(kind == NUM_LIT) {
            tokens.add(new INumLitImplementation(tokenString, kind, line, column));
            return;
        }
        if (kind == STRING_LIT ) {
            tokens.add(new IStringLitImplementation(tokenString, kind, line, column));
            return;
        }
        tokens.add(new ITokenImplementation(tokenString, kind, line, column));
    }

    private boolean match(char... expected) {
        if (isAtEnd()) return false;
        for (char c : expected) {
            if (input.charAt(position) == c) {
                position++;
                column++;
                return true;
            }
        }
        return false;
    }

    void add_reserved() {
        reserved = new HashMap<String, IToken.Kind>();
        reserved.put("image", IToken.Kind.RES_image);
        reserved.put("pixel", IToken.Kind.RES_pixel);
        reserved.put("int", IToken.Kind.RES_int);
        reserved.put("string", IToken.Kind.RES_string);
        reserved.put("void", IToken.Kind.RES_void);
        reserved.put("nil", IToken.Kind.RES_nil);
        reserved.put("load", IToken.Kind.RES_load);
        reserved.put("display", IToken.Kind.RES_display);
        reserved.put("write", IToken.Kind.RES_write);
        reserved.put("x", IToken.Kind.RES_x);
        reserved.put("y", IToken.Kind.RES_y);
        reserved.put("a", IToken.Kind.RES_a);
        reserved.put("r", IToken.Kind.RES_r);
        reserved.put("X", IToken.Kind.RES_X);
        reserved.put("Y", IToken.Kind.RES_Y);
        reserved.put("Z", IToken.Kind.RES_Z);
        reserved.put("x_cart", IToken.Kind.RES_x_cart);
        reserved.put("y_cart", IToken.Kind.RES_y_cart);
        reserved.put("a_polar", IToken.Kind.RES_a_polar);
        reserved.put("r_polar", IToken.Kind.RES_r_polar);
        reserved.put("rand", IToken.Kind.RES_rand);
        reserved.put("sin", IToken.Kind.RES_sin);
        reserved.put("cos", IToken.Kind.RES_cos);
        reserved.put("atan", IToken.Kind.RES_atan);
        reserved.put("if", IToken.Kind.RES_if);
        reserved.put("while", IToken.Kind.RES_while);
        reserved.put("red", IToken.Kind.RES_red);
        reserved.put("grn", IToken.Kind.RES_grn);
        reserved.put("blu", IToken.Kind.RES_blu);
    }
}
