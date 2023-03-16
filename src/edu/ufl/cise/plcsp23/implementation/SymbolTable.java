package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.ast.NameDef;
import edu.ufl.cise.plcsp23.ast.Type;

import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    private final HashMap<String, NameDef> symbolMap;

    public SymbolTable(List<NameDef> paramList) throws PLCException{
        //Honestly I don't even think I need to put anything in here
        symbolMap = new HashMap<String, NameDef>();
        for(NameDef n : paramList) {
            Type paramType = n.getType();
            if(paramType == Type.VOID) {
                throw new TypeCheckException("Parameter " + symbolMap + " has no type");
            }
            if(symbolMap.containsKey(n.getIdent().getName())) {
                throw new TypeCheckException("Parameter " + symbolMap + " is already defined");
            }
            else {
                symbolMap.put(n.getIdent().getName(), n);
            }
        }

    }

    public SymbolTable(SymbolTable parent) throws PLCException{
        symbolMap = new HashMap<String, NameDef>();
        symbolMap.putAll(parent.symbolMap);
        if(symbolMap.containsKey("1")) {
            INumLitImplementation numLit2 = new INumLitImplementation(null,null,symbolMap.get("1").getFirstToken().getSourceLocation().line(),0);
            NameDef nameDef2 = new NameDef(numLit2,null,null,null);
            symbolMap.replace("1", nameDef2);
        } else {
            INumLitImplementation numLit = new INumLitImplementation(null,null,1,0);
            NameDef nameDef = new NameDef(numLit,null,null,null);
            symbolMap.put("1", nameDef);
        }
    }

    public NameDef get(String s) throws PLCException {
        return symbolMap.get(s);
    }

    public void put(String s, NameDef n) throws PLCException{
        if(symbolMap.containsKey(s)) {
            //Honestly I don't know what to do here, since test 17 makes it so that the same identifier name can be used in different scopes
            //If I had any say in this I would forbid shadowing, but I don't so I guess I'll just have to deal with it
            //todo: dear god please fix this
            if (symbolMap.containsKey("1")) {
                //If this is an nested scope, then we can just allow it
                symbolMap.replace(s, n);
            }
            else {
                throw new TypeCheckException("Identifier " + s + " already exists in the symbol table, error at line " + s + ", column " + s);
            }
        }
        else {
            symbolMap.put(s, n);
        }
    }

    public void put(List<NameDef> paramList) throws PLCException {
        for(NameDef n : paramList) {
            symbolMap.put(n.getIdent().getName(), n);
            Type paramType = n.getType();
            if(paramType == Type.VOID) {
                throw new TypeCheckException("Parameter " + symbolMap + " has no type");
            }
            if(symbolMap.containsKey(n.getIdent().getName())) {
                throw new TypeCheckException("Parameter " + symbolMap + " is already defined");
            }
            else {
                symbolMap.put(n.getIdent().getName(), n);
            }
        }
    }

}
