package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            arg = new HashMap<String, NameDef>();
        }
        HashMap<String, NameDef> symbolTable = (HashMap<String, NameDef>) arg;
        Type t = program.getType();
        String id = program.getIdent().getName();
        List<NameDef> paramList = program.getParamList();
        for(NameDef param : paramList) {
            String paramName = param.getIdent().getName();
            Type paramType = param.getType();
            if(paramType == Type.VOID) {
                throw new TypeCheckException("Parameter " + paramName + " has no type");
            }
            if(symbolTable.containsKey(paramName)) {
                throw new TypeCheckException("Parameter " + paramName + " is already defined");
            } else {
                symbolTable.put(paramName, param);
            }
        }
        Type returnType = (Type) visitBlock(program.getBlock(), arg);
        if(returnType != t && t != Type.VOID && returnType != null) {
            throw new TypeCheckException("Return type does not match function type");
        }
        if(t == Type.VOID && returnType != null) {
            throw new TypeCheckException("Return type does not match function type");
        }

        //absolutely no idea what I am supposed to return
        return null;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        randomExpr.setType(Type.INT);
        //That's uh, it?
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        //ReturnStatement ::= Expr
        //Expr must be properly typed
        //ReturnStatement Type is the same as Expr Type
        Expr expr = returnStatement.getE();
        visitExpr(expr, arg);
        Type t = expr.getType();
        return t;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        stringLitExpr.setType(Type.STRING);
        //That's uh, it?
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        //UnaryExpr ::= (! | - | sin | cos | atan) Expr
        //Expr must be properly typed
        //Allowed type combinations:
        //Operator      Type        UnaryExpr Type
        //!             int         int
        //!             pixel       pixel
        //-             int         int
        //cos           int         int
        //sin           int         int
        //atan          int         int
        //Anything else is an error
        //UnaryExpr Type is the same as result type
        Expr expr = unaryExpr.getE();
        visitExpr(expr, arg);
        Type exprType = expr.getType();
        IToken.Kind op = unaryExpr.getOp();
        switch(op) {
            case BANG -> {
                if(exprType == Type.INT) {
                    unaryExpr.setType(Type.INT);
                } else if(exprType == Type.PIXEL) {
                    unaryExpr.setType(Type.PIXEL);
                } else {
                    printUnaryError(unaryExpr, exprType);
                }
            }
            case MINUS, RES_cos, RES_sin, RES_atan -> {
                if(exprType == Type.INT) {
                    unaryExpr.setType(Type.INT);
                } else {
                    printUnaryError(unaryExpr, exprType);
                }
            }
            default -> {
                throw new TypeCheckException("Invalid unary operator: " + op + " at line " + unaryExpr.getLine() + " and column " + unaryExpr.getColumn()+ ".");
            }
        }

        return null;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        //UnaryExprPostfix::=PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
        //Just gotta make sure that everything is properly typed
        //PrimaryExpr ::=STRING_LIT | NUM_LIT | IDENT | ( Expr ) | Z | rand | x | y | a | r | ExpandedPixel | PixelFunctionExp
        Expr expr = unaryExprPostfix.getPrimary();
        visitExpr(expr, arg);
        Type exprType = expr.getType();

        PixelSelector pixelSelector = unaryExprPostfix.getPixel();
        visitPixelSelector(pixelSelector, arg);
        ColorChannel channel = unaryExprPostfix.getColor();

        if(pixelSelector == null) {
            if(channel == null) {
                throw new TypeCheckException("At least one of PixelSelector or ChannelSelector should be present in order to create a UnaryExprPostfix object, at line " + unaryExprPostfix.getLine() + " and column " + unaryExprPostfix.getColumn() + ".");
            }
            if(exprType == Type.IMAGE) {
                unaryExprPostfix.setType(Type.IMAGE);
            } else if(exprType == Type.PIXEL) {
                unaryExprPostfix.setType(Type.INT);
            } else {
                printUnaryPostfixError(unaryExprPostfix, exprType);
            }
        }
        else {
            //PixelSelector is not null
            if(channel == null) {
                if(exprType == Type.IMAGE) {
                    unaryExprPostfix.setType(Type.PIXEL);
                }
                else {
                    printUnaryPostfixError(unaryExprPostfix, exprType);
                }
            }
            else {
                if(exprType == Type.IMAGE) {
                    unaryExprPostfix.setType(Type.INT);
                }
                else {
                    printUnaryPostfixError(unaryExprPostfix, exprType);
                }
            }
        }

        //PrimaryExpr.type  PixelSelector   ChannelSelector UnaryExprPostfix.type
        //Pixel             No              Yes             Int ****
        //Image             No              Yes             image
        //Image             Yes             No              pixel
        //Image             Yes             Yes             int *****

        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        //WhileStatement ::= Expr Block
        //Expr must be properly typed
        //Expr.type = int
        //Must use scope
        //Block must be properly typed
        Expr expr = whileStatement.getGuard();
        visitExpr(expr, arg);
        Type exprType = expr.getType();
        if(exprType != Type.INT) {
            throw new TypeCheckException("While statement guard must be of type int, at line " + whileStatement.getLine() + " and column " + whileStatement.getColumn() + ".");
        }
        Block block = whileStatement.getBlock();
        HashMap<String, NameDef> scope = new HashMap<>();
        //Iterate over the current scope and add all the declarations to the new scope
        scope.putAll(((HashMap<String, NameDef>) arg));
        visitBlock(block, scope);


        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        //WriteExpr ::= Expr
        //Expr must be properly typed
        Expr expr = statementWrite.getE();
        visitExpr(expr, arg);
        Type exprType = expr.getType();

        return null;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        //ZExpr.type = int
        zExpr.setType(Type.INT);
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        LValue lValue = statementAssign.getLv();
        Type t = (Type) visitLValue(lValue, arg);
        if(t == null) {
            throw new TypeCheckException("LValue type is null, at line " + statementAssign.getLine() + " and column " + statementAssign.getColumn() + ".");
        }
        Expr expr = statementAssign.getE();
        visitExpr(expr, arg);

        Type exprType = expr.getType();
        if(exprType == null) {
            throw new TypeCheckException("Expr type is null, at line " + statementAssign.getLine() + " and column " + statementAssign.getColumn() + ".");
        }
        if(t == Type.IMAGE) {
            if(exprType == Type.IMAGE || exprType == Type.STRING || exprType == Type.PIXEL) {
                return null;
            }
            else {
                printAssignmentError(statementAssign, t, exprType);
            }
        }
        else if(t == Type.PIXEL) {
            if(exprType == Type.PIXEL || exprType == Type.INT) {
                return null;
            }
            else {
                printAssignmentError(statementAssign, t, exprType);
            }
        }
        else if(t == Type.INT) {
            if(exprType == Type.INT || exprType == Type.PIXEL) {
                return null;
            }
            else {
                printAssignmentError(statementAssign, t, exprType);
            }
        }
        else if(t == Type.STRING) {
            if(exprType == Type.VOID) {
                printAssignmentError(statementAssign, t, exprType);
            }
            else {
                return null;
            }
        }
        else {
            throw new TypeCheckException("Invalid LValue type: " + t + " at line " + statementAssign.getLine() + " and column " + statementAssign.getColumn() + ".");
        }
        //LValue.type   Expr.type
        //image         image
        //              pixel
        //              string
        //pixel         pixel
        //              int
        //int           int
        //              pixel
        //string        string
        //              int
        //              pixel
        //              image

        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        //BinaryExpr ::= Expr0 (+| - | * | / | % | < | > |
        //<= | >= |== | | | || & | && | **) Expr1

        //Exp0 and Expr1 must both be properly typed
        //Allowed type combinations:
        //Operator      Expr0.type  Expr1.type  BinaryExpr.type
        //|, &          pixel       pixel       pixel
        //||, &&        int         int         int
        //<, >, <=, >=  int         int         int
        //==            int         int         int
        //==            pixel       pixel       int
        //==            image       image       int
        //==            string      string      int
        //**            int         int         int
        //**            pixel       int         pixel
        //+             int         int         int
        //+             pixel       pixel       pixel
        //+             image       image       image
        //+             string      string      string
        //-             int         int         int
        //-             pixel       pixel       pixel
        //-             image       image       image
        //*, / , %      int         int         int
        //*, / , %      pixel       pixel       pixel
        //*, / , %      image       image       image
        //*, / , %      pixel       int         pixel
        //*, / , %      image       int         image

        //BinaryExpr.Type = Result type of the operation
        //Anything not listed in the above table is an error
        HashMap<String,NameDef> symbolTable = (HashMap<String,NameDef>) arg;
        Expr expr0 = binaryExpr.getLeft();
        visitExpr(expr0, symbolTable);
        Type t1 = expr0.getType();
        Expr expr1 = binaryExpr.getRight();
        visitExpr(expr1, symbolTable);
        Type t2 = expr1.getType();
        IToken.Kind op = binaryExpr.getOp();
        switch(op) {
            case BITOR,BITAND -> {
                if(t1 == Type.PIXEL && t2 == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            case OR,AND,GE,LE,GT,LT -> {
                if(t1 == Type.INT && t2 == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            case EQ -> {
                if(t1 == Type.INT && t2 == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.PIXEL && t2 == Type.PIXEL) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.IMAGE && t2 == Type.IMAGE) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.STRING && t2 == Type.STRING) {
                    binaryExpr.setType(Type.INT);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            case EXP -> {
                if(t1 == Type.INT && t2 == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.PIXEL && t2 == Type.INT) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            case PLUS -> {
                if(t1 == Type.INT && t2 == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.PIXEL && t2 == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else if(t1 == Type.IMAGE && t2 == Type.IMAGE) {
                    binaryExpr.setType(Type.IMAGE);
                }
                else if(t1 == Type.STRING && t2 == Type.STRING) {
                    binaryExpr.setType(Type.STRING);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            case MINUS -> {
                if(t1 == Type.INT && t2 == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.PIXEL && t2 == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else if(t1 == Type.IMAGE && t2 == Type.IMAGE) {
                    binaryExpr.setType(Type.IMAGE);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            case TIMES,DIV,MOD -> {
                if(t1 == Type.INT && t2 == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else if(t1 == Type.PIXEL && t2 == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else if(t1 == Type.IMAGE && t2 == Type.IMAGE) {
                    binaryExpr.setType(Type.IMAGE);
                }
                else if(t1 == Type.PIXEL && t2 == Type.INT) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else if(t1 == Type.IMAGE && t2 == Type.INT) {
                    binaryExpr.setType(Type.IMAGE);
                }
                else {
                    printErrorBinaryExpr(binaryExpr, t1, t2);
                }
            }
            default -> {
                throw new TypeCheckException("Invalid binary expression at line " + binaryExpr.getLine() + " column " + binaryExpr.getColumn() + ".");
            }
        }

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
                return visitReturnStatement((ReturnStatement) statement, arg);
            }
        }
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        //ConditionalExpr ::= Expr0 ? Expr1 ? Expr2
        //Expr0, Expr1, Expr2 must be properly typed
        //Expr0 must be of type int
        //Expr1 and Expr2 must be of the same type, but not void
        //ConditionalExpr must be of the same type as Expr1 and Expr2
        //ConditionalExpr.Type = Expr1.Type = Expr2.Type
        Map<String, Type> symbolTable = (Map<String, Type>) arg;
        Expr expr0 = conditionalExpr.getGuard();
        visitExpr(expr0, arg);
        if(expr0.getType() != Type.INT) {
            throw new TypeCheckException("Type mismatch, error at line " + expr0.getLine()+ " column " + expr0.getColumn());
        }


        Expr expr1 = conditionalExpr.getTrueCase();
        visitExpr(expr1, arg);

        Expr expr2 = conditionalExpr.getFalseCase();
        visitExpr(expr2, arg);

        if(expr1.getType() != expr2.getType()) {
            throw new TypeCheckException("Type mismatch, error at line " + expr2.getLine()+ " column " + expr2.getColumn());
        }
        //Don't think I need to check for void here, since it should be checked in visitExpr
        conditionalExpr.setType(expr1.getType());

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
        Expr expr = declaration.getInitializer();

        if(expr != null) {
            visitExpr(expr, symbolTable);
            //Expr type must be compatible with NameDef.Type
            //LValue.type   Expr.type
            //image         image
            //              pixel
            //              string
            //pixel         pixel
            //              int
            //int           int
            //              pixel
            //string        string
            //              int
            //              pixel
            //              image
            //Iterate over all elements of expr to make sure it doesn't use the newly declared variable
            Type left = nameDef.getType();
            Type right = expr.getType();
            if(left == Type.IMAGE) {
                if(right != Type.IMAGE && right != Type.PIXEL && right != Type.STRING) {
                    throw new TypeCheckException("Type mismatch, error at line " + expr.getLine()+ " column " + expr.getColumn());
                }
            }
            else if(left == Type.PIXEL) {
                if(right != Type.PIXEL && right != Type.INT) {
                    throw new TypeCheckException("Type mismatch, error at line " + expr.getLine()+ " column " + expr.getColumn());
                }
            }
            else if(left == Type.INT) {
                if(right != Type.INT && right != Type.PIXEL) {
                    throw new TypeCheckException("Type mismatch, error at line " + expr.getLine()+ " column " + expr.getColumn());
                }
            }
            else if(left == Type.STRING) {
                if(right != Type.STRING && right != Type.INT && right != Type.PIXEL && right != Type.IMAGE) {
                    throw new TypeCheckException("Type mismatch, error at line " + expr.getLine()+ " column " + expr.getColumn());
                }
            }
        } else {
            if(nameDef.getType() == Type.IMAGE) {
                if(nameDef.getDimension() == null) {
                    throw new TypeCheckException("Image must have a dimension, error at line " + nameDef.getLine()+ " column " + nameDef.getColumn());
                }
            }
        }
        visitNameDef(nameDef, symbolTable);

        //If dimension is not null, then Type = image and Expr = image
        //Make sure that nameDef.Ident.getName() is not already in the symbol table
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        if(dimension == null) {
            return null;
        }
        //Dimension ::= Expr0 Expr1
        //Expr0 and Expr1 must be properly typed
        //Expr0 and Expr1 must be of type int
        Expr expr0 = dimension.getWidth();
        visitExpr(expr0, arg);
        if(expr0.getType() != Type.INT) {
            throw new TypeCheckException("Type mismatch, error at line " + expr0.getLine()+ " column " + expr0.getColumn());
        }
        Expr expr1 = dimension.getHeight();
        visitExpr(expr1, arg);
        if(expr1.getType() != Type.INT) {
            throw new TypeCheckException("Type mismatch, error at line " + expr1.getLine()+ " column " + expr1.getColumn());
        }
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        //ExpandedPixel ::= [ Expr0 , Expr1 , Expr2 ]
        //Expr0, Expr1, Expr2 must be properly typed
        //Expr0, Expr1, Expr2 must be of type int
        //ExpandedPixel.Type = pixel
        Expr expr0 = expandedPixelExpr.getRedExpr();
        visitExpr(expr0, arg);
        if(expr0.getType() != Type.INT) {
            throw new TypeCheckException("Type mismatch, error at line " + expr0.getLine()+ " column " + expr0.getColumn());
        }
        Expr expr1 = expandedPixelExpr.getGrnExpr();
        visitExpr(expr1, arg);
        if(expr1.getType() != Type.INT) {
            throw new TypeCheckException("Type mismatch, error at line " + expr1.getLine()+ " column " + expr1.getColumn());
        }
        Expr expr2 = expandedPixelExpr.getBluExpr();
        visitExpr(expr2, arg);
        if(expr2.getType() != Type.INT) {
            throw new TypeCheckException("Type mismatch, error at line " + expr2.getLine()+ " column " + expr2.getColumn());
        }
        expandedPixelExpr.setType(Type.PIXEL);



        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        HashMap<String,NameDef> symbolTable = (HashMap<String, NameDef>) arg;
        String identName = ident.getName();
        if(symbolTable.containsKey(identName)) {
            return null;
        } else {
            throw new TypeCheckException("Identifier not found, error at line " + ident.getLine()+ " column " + ident.getColumn());
        }
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        //IdentExpr is used when it's being used as an expression
        //Like a = b + 2
        //b would be an IdentExpr
        //IdentExpr.name has been defined and is in the current scope
        HashMap<String,NameDef> symbolTable = (HashMap<String, NameDef>) arg;
        String ident = identExpr.getName();
        if(symbolTable.containsKey(ident)) {
            NameDef d = symbolTable.get(ident);
            identExpr.setType(d.getType());
        } else {
            throw new TypeCheckException("Identifier not found, error at line " + identExpr.getLine()+ " column " + identExpr.getColumn());
        }
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        //LValue ::= Ident (PixelSelector | ε ) (ChannelSelector | ε )
        //Ident must be in the current scope
        HashMap<String,NameDef> symbolTable = (HashMap<String, NameDef>) arg;
        Ident ident = lValue.getIdent();
        visitIdent(ident, arg);
        PixelSelector pixelSelector = lValue.getPixelSelector();
        if(pixelSelector != null) {
            visitPixelSelector(pixelSelector, arg);
        }
        ColorChannel channelSelector = lValue.getColor();

        return symbolTable.get(ident.getName()).getType();
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
            //Honestly I don't know what to do here, since test 17 makes it so that the same identifier name can be used in different scopes
            //so I guess I have three possible conclusions:
            //A: The only variables visible in a scope are the ones that are declared in that scope, excluding the ones that are used in the guard of a while loop
            //B: You can just plain out use the same identifier name in different scopes
            //C: You can just plain out use the same identifier name ALWAYS, as long as they're different types
            throw new TypeCheckException("Identifier " + ident.getName() + " already exists in the symbol table, error at line " + ident.getLine() + ", column " + ident.getColumn());
        }
        symbolTable.put(ident.getName(), nameDef);

        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        //Well in this case I guess that we just gotta make sure that the type is int
        numLitExpr.setType(Type.INT);
        //Literally no idea what else to do here
        //todo: come back here lol
        int value = numLitExpr.getValue();


        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        //PixelFunctionExpr ::= ( x_cart | y_cart | a_polar | r_polar ) PixelSelector
        //PixelSelector ::= [ Expr , Expr ]
        //PixelSelector must be properly typed
        //PixelFunctionExpr.type = int

        IToken.Kind k = pixelFuncExpr.getFunction();
        PixelSelector pixelSelector = pixelFuncExpr.getSelector();
        visitPixelSelector(pixelSelector, arg);
        pixelFuncExpr.setType(Type.INT);
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        //PixelSelector ::= Expr0 Expr1
        //Expr0 and Expr1 must be properly typed
        //Expr0.type = Expr1.type = int
        if(pixelSelector == null) {
            return null;
        }
        Expr expr0 = pixelSelector.getX();
        visitExpr(expr0, arg);
        if(expr0.getType() != Type.INT) {
            throw new TypeCheckException("PixelSelector must have type int, error at line " + pixelSelector.getLine() + ", column " + pixelSelector.getColumn());
        }
        Expr expr1 = pixelSelector.getY();
        visitExpr(expr1, arg);
        if(expr1.getType() != Type.INT) {
            throw new TypeCheckException("PixelSelector must have type int, error at line " + pixelSelector.getLine() + ", column " + pixelSelector.getColumn());
        }

        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        //PredeclaredVarExpr ::= x | y | a | r
        //PredeclaredVarExpr.type = int
        predeclaredVarExpr.setType(Type.INT);
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

    public void printErrorBinaryExpr(BinaryExpr binaryExpr,Type t1, Type t2) throws TypeCheckException {
        throw new TypeCheckException("Type mismatch, type 1: " + t1 + " type 2: " + t2 + " error at line " + binaryExpr.getLine() + " column " + binaryExpr.getColumn());
    }

    public void printUnaryError(UnaryExpr unaryExpr, Type t) throws TypeCheckException {
        throw new TypeCheckException("Type mismatch, type: " + t + " error at line " + unaryExpr.getLine() + " column " + unaryExpr.getColumn());
    }

    public void printUnaryPostfixError(UnaryExprPostfix unaryExprPostfix, Type t) throws TypeCheckException {
        throw new TypeCheckException("Type mismatch, type: " + t + " error at line " + unaryExprPostfix.getLine() + " column " + unaryExprPostfix.getColumn());
    }


    private void printAssignmentError(AssignmentStatement statementAssign, Type t, Type exprType) throws TypeCheckException {
        throw new TypeCheckException("Type mismatch, type: " + t + " exprType: " + exprType + " error at line " + statementAssign.getLine() + " column " + statementAssign.getColumn());
    }
}
