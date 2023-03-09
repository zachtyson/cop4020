package edu.ufl.cise.plcsp23.implementation;

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
        if(t.getKind() == ERROR) {
            throw new LexicalException("E");
        }
        next_value++;
        return t;

    }

    private String input;

    private HashMap<String,IToken.Kind> reserved;

    public IScannerImplementation(String input) throws LexicalException {
        add_reserved();
        this.input = input;
        while(!isAtEnd()) {
            start = position;
            scanToken();
        }
        tokens.add(new ITokenImplementation("", "EOF", 0, 0));
    }

    private void scanToken() throws LexicalException {
        char c = advance();
        switch (c) {
            case '.' -> addToken(".", "DOT", line, column);
            case ',' -> addToken(",", "COMMA", line, column);
            case '?' -> addToken("?", "QUESTION", line, column);
            case ':' -> addToken(":", "COLON", line, column);
            case '(' -> addToken("(", "LPAREN", line, column);
            case ')' -> addToken(")", "RPAREN", line, column);
            case '[' -> addToken("[", "LSQUARE", line, column);
            case ']' -> addToken("]", "RSQUARE", line, column);
            case '{' -> addToken("{", "LCURLY", line, column);
            case '}' -> addToken("}", "RCURLY", line, column);
            case '=' -> {
                if (match('=')) {
                    addToken("==", "EQ", line, column);
                } else {
                    addToken("=", "ASSIGN", line, column);
                }
            }
            case '<' -> {
                if (match('-')) {
                    if (match('>')) {
                        addToken("<->", "EXCHANGE", line, column);
                    } else {
                        throw new LexicalException("Invalid token at line " + line + " and column " + column);
                    }
                } else if (match('=')) {
                    addToken("<=", "LE", line, column);
                } else {
                    addToken("<", "LT", line, column);
                }
            }
            case '>' -> {
                if (match('=')) {
                    addToken(">=", "GE", line, column);
                } else {
                    addToken(">", "GT", line, column);
                }
            }
            case '!' -> addToken("!", "BANG", line, column);
            case '&' -> {
                if (match('&')) {
                    addToken("&&", "AND", line, column);
                } else {
                    addToken("&", "BITAND", line, column);
                }
            }
            case '|' -> {
                if (match('|')) {
                    addToken("||", "OR", line, column);
                } else {
                    addToken("|", "BITOR", line, column);
                }
            }
            case '+' -> addToken("+", "PLUS", line, column);
            case '-' -> addToken("-", "MINUS", line, column);
            case '*' -> {
                if (match('*')) {
                    addToken("**", "EXP", line, column);
                } else {
                    addToken("*", "TIMES", line, column);
                }
            }
            case '/' -> addToken("/", "DIV", line, column);
            case '%' -> addToken("%", "MOD", line, column);
            case ' ', '\t', '\r' -> {

            }
            case '\n' -> {
                line++;
                column = 1;
            }
            case '"' -> string_lit();
            default -> {
                if(isDigit(c)) {
                    number(c);
                }
                if(isAlphabetic(c)) {
                    identifier();
                }
                else {
                    addToken(c+ "", "ERROR", line, column);
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
            advance();
        }
        if(isAtEnd()) {
            throw new LexicalException("Unterminated string at line " + line + " and column " + column);
        }
        advance(); //last "
        addToken(input.substring(start, position), "STRING_LIT", line, column);
    }

    private void identifier() throws LexicalException {
        start = position - 1;
        while(isAlphabetic(peek()) || isDigit(peek())) {
            advance();
        }
        String text = input.substring(start, position);
        //check for reserved words
        if(reserved.containsKey(text)) {
            addToken(text, reserved.get(text).toString(), line, column);
        }
        else {
            addToken(text, "IDENT", line, column);
        }

    }

    private void number(char c) throws LexicalException {
        start = position - 1;
        if(c == '0') { //a num_lit can't start with 0 unless it's 0
            addToken("0", "NUM_LIT", line, column);
            return;
        }
        while(isDigit(peek())) {
            advance();
        }
        addToken(input.substring(start, position), "NUM_LIT", line, column);
    }

    private char peek() {
        if(isAtEnd()) {
            return '\0'; //can't return null because char can't be null
        }
        return input.charAt(position);
    }



    private char peekNext() {
        if(position + 1 >= input.length()) {
            return '\0';
        }
        return input.charAt(position + 1);
    }

    private boolean isAtEnd() {
        return position >= input.length();
    }

    private char advance() {
        position++;
        column++;
        return input.charAt(position - 1);
    }

    private void addToken(String tokenString, String kind, int line, int column) throws LexicalException {
        if(kind.equals("NUM_LIT")) {
            System.out.println(tokenString);
            tokens.add(new INumLitImplementation(tokenString, kind, line, column));
        }
        if(kind.equals("STRING_LIT")) {
            tokens.add(new IStringLitImplementation(tokenString, kind, line, column));
        }
        tokens.add(new ITokenImplementation(tokenString, kind, line, column));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (input.charAt(position) != expected) return false;
        column++;
        position++;
        return true;
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
