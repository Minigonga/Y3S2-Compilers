package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

/**
 * Utility methods regarding types.
 */
public class TypeUtils {


    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }

    public static Type newVoidType() {
        return new Type("void", false);
    }

    public static Type convertType(JmmNode typeNode) {

        var kind = typeNode.getKind();
        var name = typeNode.get("name");
        var isArray = false;
        if ("Array".equals(kind) || "Varargs".equals(kind) || "StringArray".equals(kind)) {
            isArray = true;
        }

        return new Type(name, isArray);
    }

    public static boolean equalTypes(Type type1, Type type2) {
        if (type1.getName().equals("everything") || type2.getName().equals("everything")) {
            return true;
        }
        return type1.getName().equals(type2.getName()) && type1.isArray() == type2.isArray();
    }

    public static boolean typeCheck(Type type1, String name, Boolean array) {
        if (type1.getName().equals("everything")) {
            return true;
        }
        return type1.getName().equals(name) && type1.isArray() == array;
    }
    public Boolean isFromClass(String className) {
        if (className.equals(table.getSuper())) {
            return true;
        }
        return className.equals(table.getClassName());
    }
    public Boolean hasImport(String importName) {
        for (var importStmt : table.getImports()) {
            if (importStmt.equals(importName)) {
                return true;
            }

            if (importStmt.endsWith("." + importName)) {
                return true;
            }

            int lastDotIndex = importStmt.lastIndexOf('.');
            if (lastDotIndex != -1) {
                String simpleName = importStmt.substring(lastDotIndex + 1);
                if (simpleName.equals(importName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Type getRetType(JmmNode node) {
        var parent = node.getParent();
        if (parent == null) {
            return null;
        }
        return table.getReturnType(parent.get("name"));
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public Type getExprType(JmmNode expr) {
        var kind = expr.getKind();

        if(kind.equals("BooleanExpr") || kind.equals("Negation")) {
            return new Type("boolean", false);
        }
        else if(kind.equals("BinaryExpr")) {
            var operation = expr.get("op");
            if(operation.equals("&&")|| operation.equals("||") || operation.equals("<") || operation.equals(">")) {
                return new Type("boolean", false);
            } else {
                return new Type("int", false);
            }
        }
        else if(kind.equals("IntegerLiteral") || kind.equals("Length") || kind.equals("IncrementExpr")) {
            return new Type("int", false);
        }
        else if(kind.equals("VarRefExpr")) {
            var id = expr.get("name");
            var methodName = getCurrentMethodName(expr);

            // Check parameters
            for (var varDecl : table.getParameters(methodName)) {
                if (varDecl.getName().equals(id)) {
                    return varDecl.getType();
                }
            }

            // Check local variables
            for (var varDecl : table.getLocalVariables(methodName)) {
                if (varDecl.getName().equals(id)) {
                    return varDecl.getType();
                }
            }

            // Check fields
            for (var varDecl : table.getFields()) {
                if (varDecl.getName().equals(id)) {
                    return varDecl.getType();
                }
            }

            //Case if "io" and "io" is imported class, checks the parent
            if (hasImport(id)) {
                return getExprType(expr.getParent());
            }


            throw new RuntimeException("Undeclared variable: " + id);
        }
        else if(kind.equals("MethodCallExpr")) {
            String methodName = expr.get("method");
            Type returnType = table.getReturnType(methodName);

            if (returnType != null) {
                return returnType;
            }

            JmmNode receiver = expr.getChild(0);
            if (hasImport(receiver.get("name")) || receiver.getKind().equals("VarRefExpr")) {
                JmmNode parent = expr.getParent();
                if (parent.getKind().equals("AssignStmt")) {
                    return getExprType(parent.getChild(0));
                } else if (parent.getKind().equals("ReturnStmt")) {
                    return table.getReturnType(parent.getParent().get("name"));
                } else {
                    return new Type("void", false);
                }
            }

            throw new RuntimeException("Unknown method return type for: " + methodName);
        }
        else if (kind.equals("ArrayAccessExpr")) {
            Type arrayType = getExprType(expr.getChild(0));
            return new Type(arrayType.getName(), false); // Return element type
        }
        else if(kind.equals("ArrayDeclaration")) {
            return new Type("int", true);
        }
        else if(kind.equals("ParenthesizedExpr")) {
            return getExprType(expr.getChildren().getFirst());
        }
        else if(kind.equals("ArrayInitExpr")) {
            if (expr.getNumChildren() > 0) {
                Type elementType = getExprType(expr.getChild(0));
                return new Type(elementType.getName(), true);
            }
            return new Type("int", true); // Default to int array if empty
        }
        else if(kind.equals("NewClass")) {
            return new Type(expr.get("name"), false);
        }
        else if (kind.equals("ObjectCallExpr")) {
            return new Type(table.getClassName(), false);
        }
        else if (kind.equals("ThisExpr")) {
            return new Type(table.getClassName(), false);
        }

        // For statements that don't produce values
        return new Type("void", false);
    }


    public static Type getNodeType(JmmNode node, SymbolTable table) {
        var kind = node.getKind();
        if(kind.equals("BooleanExpr") || kind.equals("Negation")){
            return new Type("boolean", false);
        }
        else if(kind.equals("BinaryExpr")){
            var operation = node.get("op");
            if(operation.equals("&&")|| operation.equals("||") || operation.equals("<") || operation.equals(">")){
                return new Type("boolean", false);
            }else{
                return new Type("int", false);
            }
        }
        else if(kind.equals("IntegerLiteral") || kind.equals("Length") || kind.equals("IncrementExpr")){
            return new Type("int", false);
        }
        else if(kind.equals("VarRefExpr")){
            var id = node.get("name");
            var methodName = getCurrentMethodName(node);
            for (var varDecl : table.getParameters(methodName)) {
                System.out.println(varDecl);
                if (varDecl.getName().equals(id)) {
                    return varDecl.getType();
                }
            }
            // Var is a declared variable, return
            for (var varDecl : table.getLocalVariables(methodName)) {
                if (varDecl.getName().equals(id)) {
                    return varDecl.getType();
                }
            }

            for (var varDecl : table.getFields()) {
                if (varDecl.getName().equals(id)) {
                    return varDecl.getType();
                }
            }
            return new Type("Void", false);
        }
        else if(kind.equals("MethodCallExpr")){
            if (table.getReturnType(node.get("method"))!=null){
                return table.getReturnType(node.get("method"));
            }
            return new Type("everything", false);
        }
        else if (kind.equals("ArrayAccessExpr")){
            //System.out.println(getNodeType(node.getChild(0),table));
            return new Type(getNodeType(node.getChild(0),table).getName(),false);
        }
        else if(kind.equals("ArrayDeclaration")){
            return new Type("int",true);
        }
        else if(kind.equals("ParenthesisExpr")){
            return getNodeType(node.getChildren().getFirst(), table);
        }
        else if(kind.equals("ArrayInitExpr")){
            return new Type("int",true);
        }
        else if(kind.equals("NewClass")){
            return new Type(node.get("name"), false);
        }
        else if (kind.equals("ObjectCallExpr")){
            return new Type(node.get("value"), false);
        }

        return new Type("Void",false);
    }
    private static String getCurrentMethodName(JmmNode node) {
        while (node != null) {
            if (node.getKind().equals("MethodDecl")) {
                return node.get("name");
            }
            node = node.getParent();
        }
        return null;
    }


}
