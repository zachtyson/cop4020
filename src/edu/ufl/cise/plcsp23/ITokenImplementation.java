package edu.ufl.cise.plcsp23;

class ITokenImplementation implements IToken {
    private String tokenString;
    private Kind kind;
    private SourceLocation sourceLocation;


    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getTokenString() {
        return tokenString;
    }

    public ITokenImplementation(String t, String k, int x, int y) {
        tokenString = t;
        if (k == "OPERATOR") {
            //call function for operator
            k = getTokenOperatorEnum(t);
        }
        kind = Kind.valueOf(k);
        sourceLocation = new SourceLocation(x, y);
    }

    public static String getTokenOperatorEnum(String input) {
        return switch (input) {
            case "." -> "DOT";
            case "," -> "COMMA";
            case "?" -> "QUESTION";
            case ":" -> "COLON";
            case "(" -> "LPAREN";
            case ")" -> "RPAREN";
            case "<" -> "LT";
            case ">" -> "GT";
            case "[" -> "LSQUARE";
            case "]" -> "RSQUARE";
            case "{" -> "LCURLY";
            case "}" -> "RCURLY";
            case "=" -> "ASSIGN";
            case "==" -> "EQ";
            case "<->" -> "EXCHANGE";
            case "<=" -> "LE";
            case ">=" -> "GE";
            case "!" -> "BANG";
            case "&" -> "BITAND";
            case "&&" -> "AND";
            case "|" -> "BITOR";
            case "||" -> "OR";
            case "+" -> "PLUS";
            case "-" -> "MINUS";
            case "*" -> "TIMES";
            case "**" -> "EXP";
            case "/" -> "DIV";
            case "%" -> "MOD";
            default -> "ERROR";
        };
    }

}
