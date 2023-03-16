package edu.ufl.cise.plcsp23.implementation;

import edu.ufl.cise.plcsp23.ast.NameDef;

import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, NameDef> symbolMap;

    public SymbolTable() {
        //Honestly I don't even think I need to put anything in here
    }

    public SymbolTable(SymbolTable parent) {
        symbolMap = new HashMap<String, NameDef>();
        symbolMap.putAll(parent.symbolMap);
    }

    public NameDef get(String s) {
        return symbolMap.get(s);
    }

    public void put(String s, NameDef n) {
        symbolMap.put(s, n);
    }

}
