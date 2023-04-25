package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.runtime.FileURLIO;
import edu.ufl.cise.plcsp23.runtime.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
        if(type == "image") {
            imports.add("java.awt.image.BufferedImage");
            type = "BufferedImage";
        }
        if(type == "pixel") {
            type = "int";
        }
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
            String paramType = paramTypes.get(i);
            if(paramType == "image") {
                imports.add("java.awt.image.BufferedImage");
                paramType = "BufferedImage";
            }
            if(paramType == "pixel") {
                paramType = "int";
            }
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
        String exprCode = (String) visitExpr(expr, arg);
        String type = Type.convertToString(nameDef.getType());
        if(type.equals("pixel") || type.equals("PIXEL")) {
            type = "int";
            //Initializers when var is pixel
            //Java type is int, rhs is pixel, use
            //PixelOps.pack
            //see cg11c
            imports.add("edu.ufl.cise.plcsp23.runtime.PixelOps");
            String ident = nameDef.getIdent().getNameScope();
            code.append(type).append(" ").append(ident).append(";\n");
            code.append(ident).append(" = ").append(exprCode).append(";\n");
            return code.toString();

        } else if (type.equals("image") || type.equals("IMAGE")) {
            type = "BufferedImage";
            imports.add("java.awt.image.BufferedImage");
            if(nameDef.getDimension() == null) {
                //If NameDef.dimension == null
                //There must be an initializer from which the
                //size can be determined.
                //Initializer has type string.
                //Assume this is a url or filename. Use
                //FileURLIO.readImage.
                //see cg20.
                //Initializer has type image. Use
                //ImageOps.cloneImage.
                //see cg11

                //So the initializer can be either a string or an image

                //String:
                imports.add("edu.ufl.cise.plcsp23.runtime.FileURLIO");
                String ident = nameDef.getIdent().getNameScope();
                code.append(type).append(" ").append(ident).append(";\n");
                if(expr.getType() == Type.STRING) {
                    code.append(ident).append(" = FileURLIO.readImage(").append(exprCode).append(");\n");

                }
                else if(expr.getType() == Type.IMAGE) {
                    imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                    code.append(ident).append(" = ImageOps.cloneImage(").append(exprCode).append(");\n");
                }

            }
            else {
                //If NameDef.dimension != null, an image of
                //this size is created. (Default pixel values are
                //ff000000).
                //If no initializer, use ImageOps.makeImage.
                //see cg10a
                //If string initializer, use readImage overload
                //with size parameters.
                //see cg11b
                //If image initializer, use copyAndResize
                //see cg11a

                //So like [2, 3] is the size of the image, I think
                //Default pixel values are ff000000 so
                String dimensionString = visitDimension(nameDef.getDimension(), arg).toString();
                if(expr == null) {
                    //use ImageOps.makeImage
                    imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                    String ident = nameDef.getIdent().getNameScope();
                    code.append(type).append(" ").append(ident).append(";\n");
                    code.append(ident).append(" = ImageOps.makeImage(").append(dimensionString).append(");\n");

                }
                else {
                    if(expr.getType() == Type.STRING) {
                        //use readImage overload with size parameters
                        imports.add("edu.ufl.cise.plcsp23.runtime.FileURLIO");
                        String ident = nameDef.getIdent().getNameScope();
                        code.append(type).append(" ").append(ident).append(";\n");
                        code.append(ident).append(" = FileURLIO.readImage(").append(exprCode).append(", ").append(dimensionString).append(");\n");
                    }
                    else if (expr.getType() == Type.IMAGE) {
                        //use copyAndResize
                        imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                        String ident = nameDef.getIdent().getNameScope();
                        code.append(type).append(" ").append(ident).append(";\n");
                        code.append(ident).append(" = ImageOps.copyAndResize(").append(exprCode).append(", ").append(dimensionString).append(");\n");
                    }
                    else if (expr.getType() == Type.PIXEL) {
                        //use ImageOps.makeImage
                        imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                        String ident = nameDef.getIdent().getNameScope();
                        code.append(type).append(" ").append(ident).append(";\n");
                        code.append(ident).append(" = ImageOps.makeImage(").append(dimensionString).append(");\n");
                        code.append("ImageOps.setAllPixels(").append(ident).append(", ").append(exprCode).append(");\n");
                    }
                }
            }
            return code.toString();
        }
        String ident = nameDef.getIdent().getNameScope();
        code.append(type).append(" ").append(ident).append(";\n");
        if(expr != null) {
            code.append(ident).append(" = ");
            boolean hasEndingParen = false;
            if(nameDef.getType() == Type.STRING) {
                code.append("String.valueOf(");
                hasEndingParen = true;
            }

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
        String exprCode = (String) visitExpr(expr, arg);

        String LValueCode = (String) visitLValue(lValue, arg);
        Type type = lValue.getIdent().getDef().getType();

        boolean hasAlreadyAppended = false;
        boolean hasEndingParen = false;

        if(type == Type.PIXEL) {

            code.append("PixelOps.pack(");
            hasAlreadyAppended = true;
            hasEndingParen = true;
        }
        if(type == Type.IMAGE && lValue.getPixelSelector() == null && lValue.getColor() == null) {
            //Variable type is image, no pixel selector, no color channel
            //Right side is string.
            //Read image from url or file, but copy into lhs image, resizing source image.
            //Use readImage and copyInto.
            //See cg26a
            //Right side is image.
            //Copy image into destination, resizing.
            //Use copyInto.
            //See cg20
            //Right side is pixel
            //Set all pixels to given pixel value.
            //Use ImageOps.setAllPixels
            //See cg20a

            Type exprType = expr.getType();
            if(exprType == Type.STRING) {
                imports.add("edu.ufl.cise.plcsp23.runtime.FileURLIO");
                imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                code.append("ImageOps.copyInto(FileURLIO.readImage(").append(exprCode).append("), ").append(lValue.getIdent().getNameScope()).append(")");
                if(hasEndingParen) {
                    code.append(")");
                }
                code.append(";").append("\n");
                return code.toString();
            }
            else if(exprType == Type.IMAGE) {
                imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                code.append("ImageOps.copyInto(").append(exprCode).append(", ").append(lValue.getIdent().getNameScope()).append(")");
                if(hasEndingParen) {
                    code.append(")");
                }
                code.append(";").append("\n");
                return code.toString();
            }
            else if(exprType == Type.PIXEL) {
                imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                code.append("ImageOps.setAllPixels(").append(lValue.getIdent().getNameScope()).append(", ").append(exprCode).append(")");
                if(hasEndingParen) {
                    code.append(")");
                }
                code.append(";").append("\n");
                return code.toString();
            }
        }
        //Variable type is image with pixel selector, no color channel
        else if (type == Type.IMAGE && lValue.getPixelSelector() != null && lValue.getColor() == null) {
            //Define implicit loop over all pixels in image with loop
            //For example
            //int hshift = 0.
            //int vshift = h/2.
            //c[x,y]= im0[x+hshift, y+vshift].
            //Translates to
            //int hshift=0;
            //int vshift=(h/2);
            //for (int y = 0; y != c.getHeight(); y++){
            //for (int x = 0; x != c.getWidth(); x++){
            //ImageOps.setRGB(c,x,y,
            //ImageOps.getRGB(im0,(x+hshift0),(y+vshift0)));
            //}
            String ident = lValue.getIdent().getNameScope();
            String pixelSelectorCode = (String) visitPixelSelector(lValue.getPixelSelector(), arg);
//            code.append("for(int y = 0; y != ").append(ident).append(".getHeight(); y++) {\n");
//            code.append("for(int x = 0; x != ").append(ident).append(".getWidth(); x++) {\n");
//            code.append("ImageOps.setRGB(").append(ident).append(", x, y, ");
//            String ident2 = ((IdentExpr) assignmentStatement.getE()).getNameScope();
//            IdentExpr identExpr = (IdentExpr) assignmentStatement.getE();
//            code.append("ImageOps.getRGB(").append(ident2).append(", ");
//            code.append(pixelSelectorCode).append("))");
//            code.append(");\n");
//            code.append("}\n");
//            code.append("}\n");
//            if(expr instanceof ExpandedPixelExpr) {
//                //BufferedImage expected = ImageOps.makeImage(size, size);
//                //		for (int x = 0; x != expected.getWidth(); x++) {
//                //			for (int y = 0; y != expected.getHeight(); y++) {
//                //				ImageOps.setRGB(expected, x, y, PixelOps.pack((x - y), 0, (y - x)));
//                //			}
//                //		}
//                //im[x,y] = [x-y, 0, y-x].
//            }
            if(expr instanceof ExpandedPixelExpr) {
                ExpandedPixelExpr expandedPixelExpr = (ExpandedPixelExpr) expr;
                code.append("for(int y = 0; y != ").append(ident).append(".getHeight(); y++) {\n");
                code.append("for(int x = 0; x != ").append(ident).append(".getWidth(); x++) {\n");
                code.append("ImageOps.setRGB(").append(ident).append(", x, y, ");
                String r = (String) visitExpandedPixelExpr(expandedPixelExpr, arg);

                code.append(r);
                code.append(");\n");
                code.append("}\n");
                code.append("}\n");
                return code.toString();
            }
            if(expr instanceof UnaryExprPostfix) {
                //System.out.println("UnaryExprPostfix:" + exprCode);
                code.append("for(int y = 0; y != ").append(ident).append(".getHeight(); y++) {\n");
                code.append("for(int x = 0; x != ").append(ident).append(".getWidth(); x++) {\n");
                code.append("ImageOps.setRGB(").append(ident).append(", x, y, ");
                code.append(exprCode);
                code.append(");\n");
                code.append("}\n");
                code.append("}\n");
                return code.toString();
            }
            if(expr instanceof ConditionalExpr) {
                code.append("for(int y = 0; y != ").append(ident).append(".getHeight(); y++) {\n");
                code.append("for(int x = 0; x != ").append(ident).append(".getWidth(); x++) {\n");
                code.append("ImageOps.setRGB(").append(ident).append(", x, y, ");
                code.append("(");
                code.append(exprCode);
                code.append(")");
                code.append(");\n");
                code.append("}\n");
                code.append("}\n");
                return code.toString();
            }
            String exprType = expr.getClass().toString();
            code.append(exprType);
            code.append("int i = 0 /0; //todo: implement me\n");
            return code.toString();

            //how am I supposed to get the identifier of the image on the right side of the assignment?
            //I can't just use the identifier of the lvalue, since that's the identifier of the image on the left side of the assignment
            //I can't just use the identifier of the image on the right side of the assignment, since that's not an lvalue
            //so I'm just gonna assume it's a UnaryExprPostfix
            //todo LMAO


        }
        //Variable type is image with pixel selector and color channel
        else if (type == Type.IMAGE && lValue.getPixelSelector() != null && lValue.getColor() != null) {
            //todo also cause idk how to do this
            //im[x,y]:grn = Z.
            //im[x,y]:blu = Z.

            //for (int y = 0; y != im.getHeight(); y++){
            //for (int x = 0; x != im.getWidth(); x++){
            //ImageOps.setRGB(im,x,y,
            //PixelOps.setGrn(ImageOps.getRGB(im,x,y),255));
            //}
            //}
            //for (int y = 0; y != im.getHeight(); y++){
            //for (int x = 0; x != im.getWidth(); x++){
            //ImageOps.setRGB(im,x,y,
            //PixelOps.setBlu(ImageOps.getRGB(im,x,y),255));
            //}
            //}

            code.append("for(int y = 0; y != ").append(lValue.getIdent().getNameScope()).append(".getHeight(); y++) {\n");
            code.append("for(int x = 0; x != ").append(lValue.getIdent().getNameScope()).append(".getWidth(); x++) {\n");
            code.append("ImageOps.setRGB(").append(lValue.getIdent().getNameScope()).append(", x, y, ");
            String lValueColor = lValue.getColor().toString();
            //capitalize first letter of color
            lValueColor = lValueColor.substring(0, 1).toUpperCase() + lValueColor.substring(1);

            code.append("PixelOps.set").append(lValueColor).append("(ImageOps.getRGB(").append(lValue.getIdent().getNameScope()).append(", x, y), ");
            code.append(exprCode).append("))");
            code.append(";\n");
            code.append("}\n");
            code.append("}\n");
            return code.toString();

        }
        if(!hasAlreadyAppended) {
            code.append(LValueCode);
            code.append(" = ");
        }

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
        code.append(";").append("\n");
        return code.toString();
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg){
        StringBuilder code = new StringBuilder();
        //WriteStatement ::= write Expr
        //There is an implementation of
        //ConsoleIO.write where EXPR is an Image, so
        //this case can be handled uniformly.
        //However, if the expr has type Pixel, then
        //ConsoleIO.writePixel must be invoked
        //instead.
        imports.add("edu.ufl.cise.plcsp23.runtime.ConsoleIO");
        Expr expr = writeStatement.getE();
        if(expr.getType() == Type.PIXEL) {
            code.append("ConsoleIO.writePixel(");
            code.append((String) visitExpr(expr, arg));
            code.append(");").append("\n");
            return code.toString();
        }
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
            if(op != IToken.Kind.LE && op != IToken.Kind.LT && op != IToken.Kind.GE && op != IToken.Kind.GT && op != IToken.Kind.EQ && op != IToken.Kind.OR && op != IToken.Kind.AND) {
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
        //Generate comma separated code to evaluate
        //the two expressions
        Expr expr0 = dimension.getWidth();
        Expr expr1 = dimension.getHeight();
        String expr0Code = (String) visitExpr(expr0, arg);
        String expr1Code = (String) visitExpr(expr1, arg);
        return expr0Code + ", " + expr1Code;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg){
        //LValue ::= Ident (PixelSelector | ε ) (ChannelSelector | ε )
        //For assignment 5, only handle the case where there is no PixelSelector and no ChannelSelector.
        //This means that the LValue is just an Ident.
        Ident ident = lValue.getIdent();
        return visitIdent(ident, arg);

    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg){
        //PixelSelector ::= [ Expr0 Expr1 ]
        //Generate comma separated code to evaluate
        //the two expressions. (Visit children, adding
        //comma in between)
        //see test cg21.
        //is this, not the same as a dimension?

        Expr expr0 = pixelSelector.getX();
        Expr expr1 = pixelSelector.getY();
        String expr0String = (String) visitExpr(expr0,arg);
        String expr1String = (String) visitExpr(expr1,arg);
        return expr0String + " , " + expr1String;
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
            IToken.Kind bOp = ((BinaryExpr) expr0).getOp();
            if(bOp == IToken.Kind.EXP) {
                code.append(" != 0");
            }
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
        if(expr0.getType() == Type.IMAGE && expr1.getType() == Type.IMAGE) {
            //Expr0.type == IMAGE
            //Expr1..type == IMAGE
            //OP ∈ {+,-,*,/,%).
            //Use ImageOps.binaryImageImageOp
            //See test cg6_4
            imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
            String expr0Code = (String) visitExpr(expr0, arg);
            String expr1Code = (String) visitExpr(expr1, arg);
            String opString = convertOpToString(op);
            code.append("ImageOps.binaryImageImageOp(ImageOps.OP.").append(op).append(", ").append(expr0Code).append(", ").append(expr1Code).append("))");
            return code.toString();

        }
        else if(expr0.getType() == Type.IMAGE && expr1.getType() == Type.INT) {
            //Expr0.type == IMAGE
            //Expr1..type == INT
            //OP ∈ {+,-,*,/,%).
            //Use ImageOps.binaryImageScalarOp
            //See test cg6_5
            imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
            String expr0Code = (String) visitExpr(expr0, arg);
            String expr1Code = (String) visitExpr(expr1, arg);
            String opString = convertOpToString(op);
            code.append("ImageOps.binaryImageScalarOp(ImageOps.OP.").append(op).append(", ").append(expr0Code).append(", ").append(expr1Code).append("))");
            return code.toString();
        }
        else if (expr0.getType() == Type.PIXEL && expr1.getType() == Type.PIXEL) {
            //Expr0.type == PIXEL
            //Expr1..type == PIXEL
            //OP ∈ {+,-,*,/,%).
            //Use PixelOps.binaryImagePixelOp
            //See test cg6_6
            //pretty sure this is a typo and should be binaryPackedPixelPixelOp since there is no binaryImagePixelOp in PixelOps
            imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
            String expr0Code = (String) visitExpr(expr0, arg);
            String expr1Code = (String) visitExpr(expr1, arg);
            String opString = convertOpToString(op);
            code.append("ImageOps.binaryPackedPixelPixelOp(").append("ImageOps.OP.").append(op).append(", ").append(expr0Code).append(", ").append(expr1Code).append("))");
            return code.toString();
        }
        else if (expr0.getType() == Type.PIXEL && expr1.getType() == Type.INT) {
            imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
            String expr0Code = (String) visitExpr(expr0, arg);
            String expr1Code = (String) visitExpr(expr1, arg);
            String opString = convertOpToString(op);
            code.append("ImageOps.binaryPackedPixelIntOp(").append("ImageOps.OP.").append(op).append(", ").append(expr0Code).append(", ").append(expr1Code).append("))");
            return code.toString();
        }
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
        //Check if the left expression is a binary expression
        //If it is, then we need to cast it to an int
        //If it isn't, then we don't need to cast it to an int
        if(expr0 instanceof BinaryExpr) {
            code.append("(").append(expr0Code);
            IToken.Kind bOp = ((BinaryExpr) expr0).getOp();
            if(bOp == IToken.Kind.AND || bOp == IToken.Kind.OR || bOp == IToken.Kind.GE || bOp == IToken.Kind.LE || bOp == IToken.Kind.EQ  || bOp == IToken.Kind.EXP || op == IToken.Kind.LT || op == IToken.Kind.GT) {
                if(op == IToken.Kind.AND || op == IToken.Kind.OR || op == IToken.Kind.GE || op == IToken.Kind.LE || op == IToken.Kind.EQ || op == IToken.Kind.LT || op == IToken.Kind.GT){
                }
                else {
                    code.append(" ? 1 : 0");
                }
            }
            code.append(")");
        } else {
            code.append(expr0Code);

        }
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
        //Implement ! and – for int.
        //Here !x can be computed using x==0 ? 1 : 0
        //You may omit sin, cos, and atan.
        Expr expr = unaryExpr.getE();
        IToken.Kind op = unaryExpr.getOp();
        if(op == IToken.Kind.MINUS) {
            return "-" + (String) visitExpr(expr, arg);
        }
        return "!" + (String) visitExpr(expr, arg) + " == 0 ? 1 : 0";
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
        Type primaryExprType = unaryExprPostFix.getPrimary().getType();
        if(primaryExprType == Type.IMAGE) {
            //case 1:
            //pixelselector exists, channel selector does not
            //use imageOps.getRGB
            if(unaryExprPostFix.getPixel() != null && unaryExprPostFix.getColor() == null) {
                imports.add("java.awt.image.*");
                imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                //Example
                //a[x,y]
                //ImageOps.getRGB(a,x,y)
                //see test cg6_0
                return "ImageOps.getRGB(" + (String) visitExpr(unaryExprPostFix.getPrimary(), arg) + ", " + (String) visitExpr(unaryExprPostFix.getPixel().getX(),arg) + ", " + (String) visitExpr(unaryExprPostFix.getPixel().getY(),arg) + ")";
            }
            else if (unaryExprPostFix.getPixel() != null && unaryExprPostFix.getColor() != null) {
                //case 2:
                //PrimaryExpr PixelSelector ChannelSelector
                //Use PixelOps method to get color from pixel
                //and ImageOps.getRGB
                //Example:
                //a[x,y]:red
                //PixelOps.red(ImageOps.getRGB(a,x,y)
                //see test cg6_1
                imports.add("java.awt.image.*");
                imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                imports.add("edu.ufl.cise.plcsp23.runtime.PixelOps");
                return "PixelOps." + unaryExprPostFix.getColor().name().toLowerCase() + "(ImageOps.getRGB(" + (String) visitExpr(unaryExprPostFix.getPrimary(), arg) + ", " + (String) visitExpr(unaryExprPostFix.getPixel().getX(),arg) + ", " + (String) visitExpr(unaryExprPostFix.getPixel().getY(),arg) + "))";
            }
            else {
                //case 3:
                //PrimaryExpr ChannelSelector
                //Use ImageOps extract routine
                //Example:
                //a:red
                //ImageOps.extractRed(a)
                //see test cg6_2
                imports.add("java.awt.image.*");
                imports.add("edu.ufl.cise.plcsp23.runtime.ImageOps");
                String c = unaryExprPostFix.getColor().name().toLowerCase();
                return "ImageOps.extract" + c.substring(0,1).toUpperCase() + c.substring(1) + "(" + (String) visitExpr(unaryExprPostFix.getPrimary(), arg) + ")";

            }

        }
        else {
            //PrimaryExpr has type pixel
            //PixelSelector and ChannelSelector must exist
            //Use PixelOps red,grn, or blu
            //Example:
            //a:red
            //PixelOps.red(a)
            //see test cg6_3
            imports.add("edu.ufl.cise.plcsp23.runtime.PixelOps");
            String c = unaryExprPostFix.getColor().name().toLowerCase();
            return "PixelOps."+c+"(" + (String) visitExpr(unaryExprPostFix.getPrimary(), arg) + ")";
        }
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg){
        //PixelFunctionExpr ::= ( x_cart | y_cart | a_polar | r_polar ) PixelSelector
        //Not implemented in Project
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExp, Object arg){
        //PredeclaredVarExpr ::= x | y | a | r
        //Not implemented in Assignment 5

        return predeclaredVarExp.firstToken.getTokenString();
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg){
        //ExpandedPixelExpr ::= Expr0 Expr1 Expr2
        //Invoke PixelOps.pack on the values of the
        //three expressions.
        //See test cg6_7
        imports.add("edu.ufl.cise.plcsp23.runtime.PixelOps");
        Expr expr0 = expandedPixelExpr.getRedExpr();
        Expr expr1 = expandedPixelExpr.getGrnExpr();
        Expr expr2 = expandedPixelExpr.getBluExpr();
        return "PixelOps.pack(" + (String) visitExpr(expr0, arg) + ", " + (String) visitExpr(expr1, arg) + ", " + (String) visitExpr(expr2, arg) + ")";
    }

}
