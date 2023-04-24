package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.runtime.ConsoleIO;

import java.net.URLDecoder;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;

public class CodeGenerator implements ASTVisitor {
    //So CodeGenerator is run after the type checker, so we can assume that the types are correct, and we can just generate the code
    //by converting the AST to Java code
    private String packageName = null;

    private final HashSet<String> imports = new HashSet<>();

    public CodeGenerator(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    Type returnType;

    @Override
    public Object visitProgram(Program program, Object arg){
        StringBuilder code = new StringBuilder();
        //Program ::= Type Ident NameDef* Block
        returnType = program.getType();
        String type = Type.convertToString(returnType);
        String ident = program.getIdent().getName();
        List<NameDef> paramList = program.getParamList();
        Block block = program.getBlock();
        ArrayList<String> paramNames = new ArrayList<>();
        for(NameDef param : paramList) {
            paramNames.add(param.getIdent().getNameScope());
        }
        ArrayList<String> paramTypes = new ArrayList<>();
        for(NameDef param : paramList) {
            paramTypes.add(Type.convertToString(param.getType()));
        }
        String publicClass = "public class " + ident + " {\n";
        StringBuilder publicStatic = new StringBuilder("public static " + type + " apply(");
        for(int i = 0; i < paramNames.size(); i++) {
            publicStatic.append(paramTypes.get(i)).append(" ").append(paramNames.get(i));
            if(i != paramNames.size() - 1) {
                publicStatic.append(", ");
            }
        }
        String blockCode = (String) visitBlock(block, arg);
        if(packageName != null && !packageName.isEmpty()) {
            code.append("package ").append(packageName).append(";\n");
        }
        for(int i = 0; i < imports.size(); i++) {
            code.append("import ").append(imports.toArray()[i]).append(";\n");
        }
        code.append(publicClass);
        code.append(publicStatic);
        code.append(") {\n");
        code.append(blockCode);
        code.append("}\n");
        code.append("}\n");
        return code.toString();

    }

    @Override
    public Object visitBlock(Block block, Object arg){
        StringBuilder code = new StringBuilder();
        //Block ::= DecList StatementList
        List<Declaration> decList = block.getDecList();
        List<Statement> statementList = block.getStatementList();
        //Each declaration is a variable declaration, so we can just generate the code for each declaration
        //e.g. int x = 0;
        for(Declaration dec : decList) {
            code.append((String) visitDeclaration(dec, arg));
        }
        //Each statement is a statement, so we can just generate the code for each statement
        //e.g. x = 1;
        for (Statement statement : statementList) {
            //Again, a statement can either be
            //LValue = Expr
            //write Expr
            //while Expr Block
            //: Expr (this is a return statement)
            if(statement instanceof AssignmentStatement) {
                code.append((String) visitAssignmentStatement((AssignmentStatement) statement, arg));
            }
            else if(statement instanceof WriteStatement) {
                code.append((String) visitWriteStatement((WriteStatement) statement, arg));
            }
            else if(statement instanceof WhileStatement) {
                code.append((String) visitWhileStatement((WhileStatement) statement, arg));
            }
            else if(statement instanceof ReturnStatement) {
                code.append((String) visitReturnStatement((ReturnStatement) statement, arg));
            }
        }
        return code.toString();
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg){
        StringBuilder code = new StringBuilder();
        //Declaration::= NameDef (Expr | ε )
        NameDef nameDef = declaration.getNameDef();
        Expr expr = declaration.getInitializer();
        String type = Type.convertToString(nameDef.getType());
        String ident = nameDef.getIdent().getNameScope();
        code.append(type).append(" ").append(ident).append(";\n");
        if(expr != null) {
            code.append(ident).append(" = ");
            boolean hasEndingParen = false;
            if(nameDef.getType() == Type.STRING) {
                code.append("String.valueOf(");
                hasEndingParen = true;
            }

            String exprCode = (String) visitExpr(expr, arg);
            if(expr instanceof BinaryExpr) {
                BinaryExpr binaryExpr = (BinaryExpr) expr;
                IToken.Kind op = binaryExpr.getOp();
                if(op == IToken.Kind.LE || op == IToken.Kind.LT || op == IToken.Kind.GE || op == IToken.Kind.GT || op == IToken.Kind.EQ || op == IToken.Kind.AND || op == IToken.Kind.OR) {
                    code.append("(").append(exprCode).append(") ? 1 : 0");
                }
                else {
                    code.append(exprCode);
                }
            } else {
                code.append(exprCode);
            }
            if(hasEndingParen) {
                code.append(")");
            }
        }
        code.append(";").append("\n");
        return code.toString();
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg){
        StringBuilder code = new StringBuilder();
        //AssignmentStatement ::= LValue = Expr
        LValue lValue = assignmentStatement.getLv();
        Expr expr = assignmentStatement.getE();
        code.append((String) visitLValue(lValue, arg));
        code.append(" = ");
        String exprCode = (String) visitExpr(expr, arg);
        boolean hasEndingParen = false;
        //Assignment Compatibility
        //image image
        //      pixel
        //      string
        //pixel pixel
        //      int
        //int   int
        //      pixel
        //string string
        //      image
        //      pixel
        //      int
        //we are not error checking, since this should be done in the type checker
        //this is just a reminder tbh

        if(lValue.getIdent().getDef().getType() == Type.STRING) {
            code.append("String.valueOf(");
            hasEndingParen = true;
        }


        if(expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;
            IToken.Kind op = binaryExpr.getOp();
            if(op == IToken.Kind.LE || op == IToken.Kind.LT || op == IToken.Kind.GE || op == IToken.Kind.GT || op == IToken.Kind.EQ) {
                code.append("(").append(exprCode).append(") ? 1 : 0");
            }
            else {
                code.append(exprCode);
            }
        } else {
            code.append(exprCode);
        }
        if(hasEndingParen) {
            code.append(")");
        }
        code.append(";").append("\n");
        return code.toString();
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg){
        StringBuilder code = new StringBuilder();
        //WriteStatement ::= write Expr
        imports.add("edu.ufl.cise.plcsp23.runtime.ConsoleIO");
        Expr expr = writeStatement.getE();
        code.append("ConsoleIO.write(");
        code.append((String) visitExpr(expr, arg));
        code.append(");").append("\n");
        return code.toString();
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg){
        StringBuilder code = new StringBuilder();
        //WhileStatement ::= while Expr Block
        Expr expr = whileStatement.getGuard();
        Block block = whileStatement.getBlock();
        code.append("while(");
        String exprCode = (String) visitExpr(expr, arg);
        code.append(exprCode);
        if(expr instanceof NumLitExpr || expr instanceof IdentExpr || expr instanceof ZExpr) {
            code.append(" != 0");
        } else if (expr instanceof BinaryExpr binaryExpr) {
            IToken.Kind op = binaryExpr.getOp();
            if(op != IToken.Kind.LE && op != IToken.Kind.LT && op != IToken.Kind.GE && op != IToken.Kind.GT && op != IToken.Kind.EQ) {
                code.append(" != 0");
            }
        }
        code.append(") {").append("\n");
        code.append((String) visitBlock(block, arg));
        code.append("}").append("\n");
        return code.toString();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg){
        StringBuilder code = new StringBuilder();
        //ReturnStatement ::= : Expr
        Expr expr = returnStatement.getE();
        code.append("return ");
        String exprCode = (String) visitExpr(expr, arg);
        boolean closeParen = false;
        if(expr.getType() == Type.INT && returnType == Type.STRING) {
            closeParen = true;
            code.append("String.valueOf(");
        }
        if(expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;
            IToken.Kind op = binaryExpr.getOp();
            if(op == IToken.Kind.LE || op == IToken.Kind.LT || op == IToken.Kind.GE || op == IToken.Kind.GT || op == IToken.Kind.EQ) {
                code.append("(").append(exprCode).append(") ? 1 : 0");
            }
            else {
                code.append(exprCode);
            }
        } else {
            code.append(exprCode);
        }
        if(closeParen) {
            code.append(")");
        }
        code.append(";").append("\n");
        return code.toString();
    }

    public Object visitIdent(Ident ident, Object arg){
        //Ident ::= String
        return ident.getNameScope();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg){
        //Dimension ::= Expr0 Expr1
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg){
        //LValue ::= Ident (PixelSelector | ε ) (ChannelSelector | ε )
        //For assignment 5, only handle the case where there is no PixelSelector and no ChannelSelector.
        //This means that the LValue is just an Ident.
        if(lValue.getPixelSelector() != null || lValue.getColor() != null) {
            throw new UnsupportedOperationException("PixelSelector and ChannelSelector not supported in Assignment 5");
        }
        Ident ident = lValue.getIdent();
        return visitIdent(ident, arg);

    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg){
        //PixelSelector ::= [ Expr0 Expr1 ]
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg){
        //NameDef ::= Type Ident (Dimension | ε )
        //(Do not implement dimensions in assignment 5)
        //So currently just assume that there is no dimension
        StringBuilder code = new StringBuilder();
        Type type = nameDef.getType();
        String name = nameDef.getIdent().getNameScope();
        String typeString = Type.convertToString(type);
        code.append(typeString).append(" ").append(name);
        return code.toString();
    }


    public Object visitExpr(Expr expr, Object arg){
        //Expr ::= ConditionalExpr | BinaryExpr |
        //UnaryExpr | StringLitExpr | IdentExpr |
        //NumLitExpr | ZExpr | RandExpr |
        //UnaryExprPostFix | PixelFuncExpr
        //|PredeclaredVarExp
        if(expr instanceof ConditionalExpr) {
            return visitConditionalExpr((ConditionalExpr) expr, arg);
        }
        else if(expr instanceof BinaryExpr) {
            return visitBinaryExpr((BinaryExpr) expr, arg);
        }
        else if(expr instanceof UnaryExpr) {
            return visitUnaryExpr((UnaryExpr) expr, arg);
        }
        else if(expr instanceof StringLitExpr) {
            return visitStringLitExpr((StringLitExpr) expr, arg);
        }
        else if(expr instanceof IdentExpr) {
            return visitIdentExpr((IdentExpr) expr, arg);
        }
        else if(expr instanceof NumLitExpr) {
            return visitNumLitExpr((NumLitExpr) expr, arg);
        }
        else if(expr instanceof ZExpr) {
            return visitZExpr((ZExpr) expr, arg);
        }
        else if(expr instanceof RandomExpr) {
            return visitRandomExpr((RandomExpr) expr, arg);
        }
        else if(expr instanceof UnaryExprPostfix) {
            return visitUnaryExprPostFix((UnaryExprPostfix) expr, arg);
        }
        else if(expr instanceof PixelFuncExpr) {
            return visitPixelFuncExpr((PixelFuncExpr) expr, arg);
        }
        else if(expr instanceof PredeclaredVarExpr) {
            return visitPredeclaredVarExpr((PredeclaredVarExpr) expr, arg);
        }
        else if(expr instanceof ExpandedPixelExpr) {
            return visitExpandedPixelExpr((ExpandedPixelExpr) expr, arg);
        }
        return null;
    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) {
        StringBuilder code = new StringBuilder();
        //ConditionalExpr ::= Expr0 Expr1 Expr2
        Expr expr0 = conditionalExpr.getGuard();
        Expr expr1 = conditionalExpr.getTrueCase();
        Expr expr2 = conditionalExpr.getFalseCase();
        code.append("(");
        String expr0Code = (String) visitExpr(expr0, arg);
        if(expr0 instanceof BinaryExpr) {
            code.append("(").append(expr0Code).append(")");
        } else {
            code.append("(").append(expr0Code).append(" != 0 )");

        }
        code.append(" ? ");
        code.append((String) visitExpr(expr1, arg));
        code.append(":\n");
        code.append((String) visitExpr(expr2, arg));
        code.append(")");

        return code.toString();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) {
        //BinaryExpr ::= Expr0 (+| - | * | / | % | < | > |
        //<= | >= |== | | | || & | && | **) Expr1
        StringBuilder code = new StringBuilder();
        Expr expr0 = binaryExpr.getLeft();
        IToken.Kind op = binaryExpr.getOp();
        Expr expr1 = binaryExpr.getRight();
        code.append("(");
        String expr0Code = (String) visitExpr(expr0, arg);
        String opString = convertOpToString(op);
        if (opString.equals("**")){
            imports.add("java.lang.Math");
            String expr1Code = (String) visitExpr(expr1, arg);
            code.append("(int) Math.pow(").append(expr0Code).append(", ").append(expr1Code).append("))");
            return code.toString();
        }
        // && and || are SO ANNOYING AKLDJASKDLJASDKLASJ
        // Cause both the clauses need to be converted from int to boolean but ONLY if they are not already boolean
        // So we need to check if they are boolean and if they are not, then we need to convert them to boolean
        // Cause this: :if (val > 0 && val2 > 0) ? val ? val2. we don't need to convert val and val2 to boolean
        // But this: if(val && val2) ? val ? val2. We need to convert val and val2 to boolean
        // So we need to check if the left and right expressions are boolean or not
        // So I guess we can just check to see if it's a binary expression or not???
        else if (opString.equals("&&") || opString.equals("||")) {
            if (expr0 instanceof BinaryExpr) {
                code.append("(").append(expr0Code).append(")");
            } else {
                code.append("(").append(expr0Code).append(" != 0 )");
            }
            code.append(" ").append(opString).append(" ");
            if (expr1 instanceof BinaryExpr) {
                code.append("(").append((String) visitExpr(expr1, arg)).append(")");
            } else {
                code.append("(").append((String) visitExpr(expr1, arg)).append(" != 0 )");
            }
            code.append(")");
            return code.toString();
        }
        code.append(expr0Code);
        code.append(" ").append(opString).append(" ");
        code.append((String) visitExpr(expr1, arg));
        code.append(")");
        return code.toString();

    }

    private String convertOpToString(IToken.Kind k) {
        switch(k) {
            case PLUS -> { return "+"; }
            case MINUS -> { return "-"; }
            case TIMES -> { return "*"; }
            case DIV -> { return "/"; }
            case MOD -> { return "%"; }
            case LT -> { return "<"; }
            case GT -> { return ">"; }
            case LE -> { return "<="; }
            case GE -> { return ">="; }
            case EQ -> { return "=="; }
            case OR -> { return "||"; }
            case AND -> { return "&&"; }
            case BITAND -> {return "&";}
            case BITOR -> {return "|";}
            case BANG -> {return "!";}
            case EXP -> {return "**";}
            default -> { return ""; }
        }
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg){
        //UnaryExpr ::= (! | - | sin | cos | atan) Expr
//        StringBuilder code = new StringBuilder();
//        IToken.Kind op = unaryExpr.getOp();
//        Expr expr = unaryExpr.getE();
//        code.append(convertOpToString(op)).append("( ");
//        code.append((String) visitExpr(expr, arg)).append(" )");
//        return code.toString();
        return null;
        //not implemented in assignment 5
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg){
        //Generate the Java string literal corresponding
        //to this one. (You may ignore escape
        //sequences)
        return "\"" + stringLitExpr.getValue() + "\"";
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) {
        //Generate name
        return identExpr.getNameScope();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg){
        //This isn't even mentioned in the assignment, but I'm assuming it's just a number
        return String.valueOf(numLitExpr.getValue());
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg){
        //Constant with value 255
        return String.valueOf(255);
    }

    @Override
    public Object visitRandomExpr(RandomExpr randExpr, Object arg){
        //Generate code for a random int in [0,256)
        //using Math.floor(Math.random() * 256)
        imports.add("java.lang.Math");
        return "(int) Math.floor(Math.random() * 256)";
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostFix, Object arg){
        //UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg){
        //PixelFunctionExpr ::= ( x_cart | y_cart | a_polar | r_polar ) PixelSelector
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExp, Object arg){
        //PredeclaredVarExpr ::= x | y | a | r
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg){
        //ExpandedPixelExpr ::= Expr0 Expr1 Expr2
        //Not implemented in Assignment 5
        return null;
    }

}
