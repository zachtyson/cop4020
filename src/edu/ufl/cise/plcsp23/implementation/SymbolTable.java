package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.ast.NameDef;
import edu.ufl.cise.plcsp23.ast.Type;

import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    private final HashMap<String, Symbol> symbolMap;

    private int currentScope = 0;

    public SymbolTable(List<NameDef> paramList) throws PLCException{
        //Honestly I don't even think I need to put anything in here
        symbolMap = new HashMap<String, Symbol>();
        for(NameDef n : paramList) {
            Type paramType = n.getType();
            if(paramType == Type.VOID) {
                throw new TypeCheckException("Parameter " + symbolMap + " has no type");
            }
            if(symbolMap.containsKey(n.getIdent().getName())) {
                throw new TypeCheckException("Parameter " + symbolMap + " is already defined");
            }
            else {
                symbolMap.put(n.getIdent().getName(), new Symbol(n, currentScope));
            }
        }

    }

    public SymbolTable(SymbolTable parent) throws PLCException{
        currentScope = parent.currentScope + 1;
        symbolMap = new HashMap<String, Symbol>();
        symbolMap.putAll(parent.symbolMap);
    }

    public NameDef get(String s) throws PLCException {
        Symbol symbol = symbolMap.get(s);
        if(symbol == null) {
            return null;
        }
        NameDef nameDef = symbol.getNameDef();
        return nameDef;
    }

    public void put(String s, NameDef n) throws PLCException{
        Symbol symbol = symbolMap.get(s);
        if(symbol == null) {
            symbolMap.put(s, new Symbol(n, currentScope));
        }
        else {
            if(currentScope == symbol.getScope()) {
                throw new TypeCheckException("Variable " + s + " is already defined");
            }
            else {
                symbolMap.put(s, new Symbol(n, currentScope));
            }
        }
    }

    public void put(List<NameDef> paramList) throws PLCException {
        for(NameDef n : paramList) {
            symbolMap.put(n.getIdent().getName(), new Symbol(n, currentScope));
            Type paramType = n.getType();
            if(paramType == Type.VOID) {
                throw new TypeCheckException("Parameter " + symbolMap + " has no type");
            }
            if(symbolMap.containsKey(n.getIdent().getName())) {
                throw new TypeCheckException("Parameter " + symbolMap + " is already defined");
            }
            else {
                symbolMap.put(n.getIdent().getName(), new Symbol(n, currentScope));
            }
        }
    }

}

class Symbol {
    //A symbol is literally like the exact same as a NameDef, but it's not an AST node
    //Scope is also stored here, but I'm not sure how to implement it
    private NameDef nameDef;

    private int scope = 0;

    private String name;

    public Symbol(NameDef nameDef, int scope) {
        this.nameDef = nameDef;
        this.scope = scope;
    }

    public Symbol(Symbol symbol) {
        this.nameDef = symbol.nameDef;
        this.scope = symbol.scope;
        this.name = symbol.name;
    }

    public NameDef getNameDef() {
        return nameDef;
    }

    public int getScope() {
        return scope;
    }

    public String getName() {
        return name;
    }



}
