package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerator implements ASTVisitor {
    //So CodeGenerator is run after the type checker, so we can assume that the types are correct, and we can just generate the code
    //by converting the AST to Java code
    private final String packageName;

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
        String type = program.getType().toString();
        String ident = program.getIdent().toString();
        List<NameDef> paramList = program.getParamList();
        Block block = program.getBlock();
        ArrayList<String> paramNames = new ArrayList<>();
        for(NameDef param : paramList) {
            paramNames.add(param.getIdent().toString());
        }
        ArrayList<String> paramTypes = new ArrayList<>();
        for(NameDef param : paramList) {
            paramTypes.add(param.getType().toString());
        }
        code.append("public class ").append(ident).append(" {\n");
        code.append("public static ").append(type).append(" apply(");
        for(int i = 0; i < paramNames.size(); i++) {
            code.append(paramTypes.get(i)).append(" ").append(paramNames.get(i));
            if(i != paramNames.size() - 1) {
                code.append(", ");
            }
        }
        code.append(") {\n");
        code.append((String) block.visit(this, arg));
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
        String type = nameDef.getType().toString();
        String ident = nameDef.getIdent().toString();
        code.append(type).append(" ").append(ident);
        if(expr != null) {
            code.append(" = ").append((String) visitExpr(expr, arg));
        }
        code.append(";").append("\n");
        return code.toString();
    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        StringBuilder code = new StringBuilder();
        //ConditionalExpr ::= Expr0 Expr1 Expr2
        Expr expr0 = conditionalExpr.getGuard();
        Expr expr1 = conditionalExpr.getTrueCase();
        Expr expr2 = conditionalExpr.getFalseCase();
        code.append("if (").append((String) visitExpr(expr0, arg)).append(") {\n");
        code.append((String) visitExpr(expr1, arg));
        code.append("} else {\n");
        code.append((String) visitExpr(expr2, arg));
        code.append("}\n");

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
        code.append((String) visitExpr(expr0, arg));
        //todo: convert op to java code correctly
        code.append(" ").append(op.toString()).append(" ");
        code.append((String) visitExpr(expr1, arg));
        return code.toString();

    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        //UnaryExpr ::= (! | - | sin | cos | atan) Expr
        //Not implemented in Assignment 5
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        //Generate the Java string literal corresponding
        //to this one. (You may ignore escape
        //sequences)
        return stringLitExpr.getValue();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        //Generate name
        return identExpr.getName();
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
        int x = (int) Math.floor(Math.random()*256);
        return String.valueOf(x);
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

}
