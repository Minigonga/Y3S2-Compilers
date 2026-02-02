package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static pt.up.fe.comp2025.ast.Kind.TYPE;

/**
 * Utility methods related to the optimization middle-end.
 */
public class OptUtils {


    private final AccumulatorMap<String> temporaries;

    private final TypeUtils types;

    public OptUtils(TypeUtils types) {
        this.types = types;
        this.temporaries = new AccumulatorMap<>();
    }


    public String nextTemp() {

        return nextTemp("tmp");
    }

    public String nextTemp(String prefix) {

        // Subtract 1 because the base is 1
        var nextTempNum = temporaries.add(prefix) - 1;

        return prefix + nextTempNum;
    }

    public String arrayTempCut(String temp) {
        int dotIndex = temp.indexOf(".");
        if (dotIndex == -1) return temp;
        return temp.substring(0, dotIndex);
    }

    public String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        return toOllirType(types.convertType(typeNode));
    }

    public String toOllirType(Type type) {
        if (type.isArray()) {
            return ".array" + toOllirType(type.getName());
        }
        return toOllirType(type.getName());
    }

    public String getMethodName(JmmNode node) {
        var parent = node.getParent();
        while (parent != null && !parent.getKind().equals("MethodDecl")) {
            parent = parent.getParent();
        }
        return parent.get("name");
    }

    private String toOllirType(String typeName) {

        String type;

        switch (typeName) {
            case "int" -> type = ".i32";
            case "boolean" -> type = ".bool";
            case "[]" -> type = ".array";
            case "String" -> type = ".String";
            case "void" -> type = ".V";
            default -> {
                if (types.hasImport(typeName) || types.isFromClass(typeName)) {
                    return "." + typeName; // or maybe "." + typeName
                } else {
                    throw new NotImplementedException(typeName);
                }
            }
        }

        return type;
    }

    public boolean checkIfField(String variable, JmmNode node, SymbolTable table) {
        var methodName = getMethodName(node);
        for (var param : table.getParameters(methodName)) {
            if (param.getName().equals(variable)) {
                return false;
            }
        }
        for (var localVariable : table.getLocalVariables(methodName)) {
            if (localVariable.getName().equals(variable)) {
                return false;
            }
        }
        for (var field : table.getFields()) {
            if (field.getName().equals(variable)) {
                return true;
            }
        }
        return false;
    }
}
