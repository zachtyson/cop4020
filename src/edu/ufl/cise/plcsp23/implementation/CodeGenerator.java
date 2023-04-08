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
    private final String packageName;

    private final HashSet<String> imports = new HashSet<>();

    public CodeGenerator(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //Program ::= Type Ident NameDef* Block
        Type t = program.getType();
        String type = Type.convertToString(t);
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
    public Object visitBlock(Block block, Object arg) throws PLCException {
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
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //Declaration::= NameDef (Expr | ε )
        NameDef nameDef = declaration.getNameDef();
        Expr expr = declaration.getInitializer();
        String type = Type.convertToString(nameDef.getType());
        String ident = nameDef.getIdent().getNameScope();
        code.append(type).append(" ").append(ident).append(";\n");
        if(expr != null) {
            code.append(ident).append(" = ").append((String) visitExpr(expr, arg));
        }
        code.append(";").append("\n");
        return code.toString();
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //AssignmentStatement ::= LValue = Expr
        LValue lValue = assignmentStatement.getLv();
        Expr expr = assignmentStatement.getE();
        code.append((String) visitLValue(lValue, arg));
        code.append(" = ");
        code.append((String) visitExpr(expr, arg));
        code.append(";").append("\n");
        return code.toString();
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCException {
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
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //WhileStatement ::= while Expr Block
        Expr expr = whileStatement.getGuard();
        Block block = whileStatement.getBlock();
        code.append("while(");
        code.append((String) visitExpr(expr, arg));
        code.append(") {").append("\n");
        code.append((String) visitBlock(block, arg));
        code.append("}").append("\n");
        return code.toString();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //ReturnStatement ::= : Expr
        Expr expr = returnStatement.getE();
        code.append("return ");
        code.append((String) visitExpr(expr, arg));
        code.append(";").append("\n");
        return code.toString();
    }

    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        //Ident ::= String
        return ident.getNameScope();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        //Dimension ::= Expr0 Expr1
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        //LValue ::= Ident (PixelSelector | ε ) (ChannelSelector | ε )
        //For assignment 5, only handle the case where there is no PixelSelector and no ChannelSelector.
        //This means that the LValue is just an Ident.
        Ident ident = lValue.getIdent();
        return visitIdent(ident, arg);

    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        //PixelSelector ::= [ Expr0 Expr1 ]
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
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


    public Object visitExpr(Expr expr, Object arg) throws PLCException {
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

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //ConditionalExpr ::= Expr0 Expr1 Expr2
        Expr expr0 = conditionalExpr.getGuard();
        Expr expr1 = conditionalExpr.getTrueCase();
        Expr expr2 = conditionalExpr.getFalseCase();
        code.append("(").append((String) visitExpr(expr0, arg)).append(" ? ");
        code.append((String) visitExpr(expr1, arg));
        code.append(":\n");
        code.append((String) visitExpr(expr2, arg));
        code.append(")");

        return code.toString();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        //BinaryExpr ::= Expr0 (+| - | * | / | % | < | > |
        //<= | >= |== | | | || & | && | **) Expr1
        StringBuilder code = new StringBuilder();
        Expr expr0 = binaryExpr.getLeft();
        IToken.Kind op = binaryExpr.getOp();
        Expr expr1 = binaryExpr.getRight();
        code.append("(");
        code.append((String) visitExpr(expr0, arg));
        String opString = convertOpToString(op);
        code.append(" ").append(opString).append(" ");
        code.append((String) visitExpr(expr1, arg));
//        if(opString.equals("==") || opString.equals("!=")) {
//            code.append("? 1 : 0");
//        }
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
            default -> { return ""; }
        }
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        //UnaryExpr ::= (! | - | sin | cos | atan) Expr
        StringBuilder code = new StringBuilder();
        IToken.Kind op = unaryExpr.getOp();
        Expr expr = unaryExpr.getE();
        code.append(convertOpToString(op)).append("( ");
        code.append((String) visitExpr(expr, arg)).append(" )");
        return code.toString();
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        //Generate the Java string literal corresponding
        //to this one. (You may ignore escape
        //sequences)
        return "\"" + stringLitExpr.getValue() + "\"";
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        //Generate name
        return identExpr.getNameScope();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        //This isn't even mentioned in the assignment, but I'm assuming it's just a number
        return String.valueOf(numLitExpr.getValue());
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        //Constant with value 255
        return String.valueOf(255);
    }

    @Override
    public Object visitRandomExpr(RandomExpr randExpr, Object arg) throws PLCException {
        //Generate code for a random int in [0,256)
        //using Math.floor(Math.random() * 256)
        imports.add("java.lang.Math");
        return "(int) Math.floor(Math.random() * 256)";
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostFix, Object arg) throws PLCException {
        //UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        //PixelFunctionExpr ::= ( x_cart | y_cart | a_polar | r_polar ) PixelSelector
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExp, Object arg) throws PLCException {
        //PredeclaredVarExpr ::= x | y | a | r
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        //ExpandedPixelExpr ::= Expr0 Expr1 Expr2
        //Not implemented in Assignment 5
        return null;
    }

}
