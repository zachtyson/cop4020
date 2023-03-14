package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//Straight from notes:
//A name can only be used once
//A variable must be declared before it is used
//A variable must be initialized before it is referenced
public class ASTVisitorImplementation implements ASTVisitor {
    //Assuming that symbol table is a hash map
    //So this part is for checking types and scope


    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        //Program contains
        //Type
        //Ident
        //Parameter List
        //Block
        //Unlike the notes, in this implementation, the declaration and statement lists
        //are within the block, not the program class
        //
        //From my understanding of the notes, arg is the symbol table
        //arg would contain global variables as well as the parameters
        //arg would be passed to the block, then declarations, then statements, etc
        if(arg == null) {
            arg = new HashMap<String, Type>();
        }
        visitBlock(program.getBlock(), arg);
        //absolutely no idea what I am supposed to return
        return null;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        //Block contains
        //Declaration List
        //Statement List
        //Honestly I don't know what I am supposed to do here either
        List<Declaration> decList = block.getDecList();
        List<Statement> statementList = block.getStatementList();
        for (Declaration dec : decList) {
            //Declaration = NameDef
            //Declaration = NameDef = Expr
            visitDeclaration(dec, arg);
        }
        for (Statement statement : statementList) {
            //Again, a statement can either be
            //LValue = Expr
            //write Expr
            //while Expr Block
            //: Expr (this is a return statement)
            if(statement instanceof AssignmentStatement) {
                visitAssignmentStatement((AssignmentStatement) statement, arg);
            }
            else if(statement instanceof WriteStatement) {
                visitWriteStatement((WriteStatement) statement, arg);
            }
            else if(statement instanceof WhileStatement) {
                visitWhileStatement((WhileStatement) statement, arg);
            }
            else if(statement instanceof ReturnStatement) {
                visitReturnStatement((ReturnStatement) statement, arg);
            }
        }
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        //Make sure that all declarations are properly typed
        //And then we just add each declaration to the symbol table?
        //The scope of each identifier should last until the end of the program, excluding nested identifiers created in while loops
        //Declaration::= NameDef (Expr | ε )
        //NameDef ::= Type IDENT | Type Dimension IDENT
        //Type ::= image | pixel | int | string (void is not allowed here)
        //NameDef must be properly typed (explored in visitNameDef)
        //Expr must be properly typed if it exists (todo: explore this in visitExpr)
        //if Expr exists, it must be compatible with NameDef.Type
        //if NameDef.Type == Image then either Expr != null or NameDef.Dimension != null, or both

        HashMap<String,NameDef> symbolTable = (HashMap<String, NameDef>) arg;
        NameDef nameDef = declaration.getNameDef();
        visitNameDef(nameDef, symbolTable);
        Expr expr = declaration.getInitializer();
        if(expr != null) {
            visitExpr(expr, symbolTable);
            //Expr type must be compatible with NameDef.Type
            if(expr.getType() != nameDef.getType()) {
                throw new TypeCheckException("Type mismatch, error at line " + nameDef.getLine()+ " column " + nameDef.getColumn());
            }
        } else {
            if(nameDef.getType() == Type.IMAGE) {
                if(nameDef.getDimension() == null) {
                    throw new TypeCheckException("Image must have a dimension, error at line " + nameDef.getLine()+ " column " + nameDef.getColumn());
                }
            }
        }

        //If dimension is not null, then Type = image and Expr = image
        //Make sure that nameDef.Ident.getName() is not already in the symbol table
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        //NameDef ::= Type IDENT | Type Dimension IDENT
        //Type ::= image | pixel | int | string | void
        //Dimension ::= [Expr, Expr]
        //If dimension is not null, then Type MUST be image
        //If dimension is not null, then dimension must be a proper type
        //Ident name must not already be in the symbol table
        //Type cannot be void unless it's a function return type, which wouldn't appear here
        //If these pass, then we can add the name to the symbol table
        HashMap<String,NameDef> symbolTable = (HashMap<String, NameDef>) arg;
        Type type = nameDef.getType();
        Ident ident = nameDef.getIdent();
        Dimension dimension = nameDef.getDimension();
        if(dimension != null) {
            if(type != Type.IMAGE) {
                throw new TypeCheckException("Non null dimension must have type image, error at line " + ident.getLine() + ", column " + ident.getColumn());
            }
            visitDimension(dimension, arg);
            //todo: while loop type check here
        }
        if(type == Type.VOID) {
            throw new TypeCheckException("Type cannot be void, error at line " + ident.getLine() + ", column " + ident.getColumn());
        }
        if(symbolTable.containsKey(ident.getName())) {
            throw new TypeCheckException("Identifier " + ident.getName() + " already exists in the symbol table, error at line " + ident.getLine() + ", column " + ident.getColumn());
        }
        symbolTable.put(ident.getName(), nameDef);

        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return null;
    }

    public Object visitExpr(Expr expr, Object arg) throws PLCException {
        return null;
    }
}
