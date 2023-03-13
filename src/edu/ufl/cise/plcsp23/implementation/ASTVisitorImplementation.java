package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.PLCException;
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
        visitBlock(program.getBlock(), arg);
        //absolutely no idea what I am supposed to return
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
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        //Make sure that all declarations are properly typed
        //And then we just add each declaration to the symbol table?
        //The scope of each identifier should last until the end of the program, excluding nested identifiers created in while loops
        //Declaration = NameDef
        //Declaration = NameDef = Expr
        //NameDef = Type Ident | Type Dimension Ident
        //Type = image | pixel | int | string | void
        //CANNOT be void however, void is reserved function return type
        //Dimension = [Expr, Expr]
        NameDef nameDef = declaration.getNameDef();
        Expr expr = declaration.getInitializer();
        //If dimension is not null, then Type = image and Expr = image
        //Make sure that nameDef.Ident.getName() is not already in the symbol table
    }
}
