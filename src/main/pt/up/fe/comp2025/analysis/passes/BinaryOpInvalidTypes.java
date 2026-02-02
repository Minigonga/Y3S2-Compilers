package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static pt.up.fe.comp2025.ast.TypeUtils.getNodeType;

public class BinaryOpInvalidTypes extends AnalysisVisitor {
    private String currentMethod;
    private Boolean isStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.BINARY_EXPR, this::visitCalculationExpr);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_STMT, this::visitArrayStmt);
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(Kind.TYPE, this::visitType);
        addVisit(Kind.INCREMENT_EXPR, this::visitIncrementExpr);
        addVisit(Kind.NEGATION, this::visitNegationExpr);
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }
        if (table.getFields().stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            if (isStatic){
                var message = "A static method cannot access field variables";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        varRefExpr.getLine(),
                        varRefExpr.getColumn(),
                        message,
                        null)
                );
            }
            return null;
        }

        if (hasImport(table, varRefName)) {return null;}

        // Create error report
        var message = "Variable '%s' does not exist.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                varRefExpr.getLine(),
                varRefExpr.getColumn(),
                message,
                null)
        );

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        String methodName = method.get("name");

        // Check for main method
        isStatic = method.hasAttribute("isStatic");
        if ("main".equals(methodName)) {
            // Check modifiers
            boolean isPublic = method.hasAttribute("isPublic") &&
                    Boolean.parseBoolean(method.get("isPublic"));


            if (!isPublic || !isStatic || !method.getChild(0).get("name").equals("void")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        "Main method must be declared as 'public static void' with no return type",
                        null)
                );
            }

            // Check parameters
            List<JmmNode> params = method.getChildren().stream()
                    .filter(c -> c.getKind().equals("Param"))
                    .toList();

            if (params.size() != 1) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        "Main method must have exactly one parameter",
                        null)
                );
            } else {
                JmmNode param = params.get(0);
                String paramKind = param.getChildren().get(0).getKind();
                if (!"StringArray".equals(paramKind) | !param.get("name").equals("args")) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            param.getLine(),
                            param.getColumn(),
                            "Main method parameter must be of type String[] args",
                            null)
                    );
                }
            }
        }
        // Your existing varargs check
        currentMethod = methodName;
        int counter = 0;
        boolean isLast = true;

        if (table.getParameters(currentMethod) != null) {
            for (var param : method.getChildren()) {
                if (param.getKind().equals("Param")) {
                    if (param.getChildren().getFirst().getKind().equals("Varargs")) {
                        counter++;
                        isLast = true;
                    } else {
                        isLast = false;
                    }
                }
            }

            if (!((counter == 1 && isLast) || counter == 0)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        "The varargs method is not declared correctly.",
                        null)
                );
            }
        }
        //Vê o tipo do metodo
        var type = method.getChild(0);
        if (type.getKind().equals("Id")) {
            var name = type.get("name");
            if (!hasImport(table, name) && !table.getClassName().equals(name) && !name.equals("void")) {
                var message = String.format("Method of type %s does not exist.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        message,
                        null)
                );
            }
        }

        //Check se team declarações repetidas
        Set<String> seenVarDecls = new HashSet<>();
        for (var varDecl: method.getChildren()) {
            if (varDecl.getKind().equals("VarDecl")) {
                if (!seenVarDecls.add(varDecl.get("name"))) {
                    var message = String.format("The variable %s is already declared", varDecl.get("name"));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            method.getLine(),
                            method.getColumn(),
                            message,
                            null)
                    );
                }
            }
        }

        return null;
    }

    private Void visitCalculationExpr(JmmNode binaryExprNode, SymbolTable symbolTable) {

        var var1 = getNodeType(binaryExprNode.getChildren().getFirst(),symbolTable);
        var var2 = getNodeType(binaryExprNode.getChildren().getLast(),symbolTable);
        var op = binaryExprNode.get("op");

        if (var1.getName().equals("Void") || var2.getName().equals("Void")) {
            return null;
        }
        if (op.equals("||") || op.equals("&&")) {
            if (!(TypeUtils.typeCheck(var1,"boolean",false) && TypeUtils.typeCheck(var2,"boolean",false))) {
                var message = "The binary expression has invalid operands types.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        binaryExprNode.getLine(),
                        binaryExprNode.getColumn(),
                        message,
                        null)
                );
            }
        }
        else{
            if (!(TypeUtils.typeCheck(var1,"int",false) && TypeUtils.typeCheck(var2,"int",false))) {
                var message = "The integer expression has invalid operands types.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        binaryExprNode.getLine(),
                        binaryExprNode.getColumn(),
                        message,
                        null)
                );
            }
        }


        return null;
    }

    private Void visitArrayAccessExpr(JmmNode node, SymbolTable table) {
        var var1 = getNodeType(node.getChildren().getFirst(), table);
        var var2 = getNodeType(node.getChildren().getLast(), table);
        if (!(TypeUtils.typeCheck(var1,"int",true) && TypeUtils.typeCheck(var2,"int",false))) {
            var message = "The array access expression has invalid types.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitArrayStmt(JmmNode node, SymbolTable table) {
        var var1 = node.get("name");
        var boolCheck = false;
        for (var variable : table.getLocalVariables(currentMethod)) {
            if (var1.equals(variable.getName())) {
                if (!(TypeUtils.typeCheck(variable.getType(),"int",true))) {
                    var message = "The array statement has invalid types.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            node.getLine(),
                            node.getColumn(),
                            message,
                            null)
                    );
                }
                else{
                    boolCheck = true;
                    break;
                }
            }
        }
        for (var param : table.getParameters(currentMethod)) {
            if (var1.equals(param.getName())) {
                if (!(TypeUtils.typeCheck(param.getType(),"int",true))) {
                    var message = "The array statement has invalid types.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            node.getLine(),
                            node.getColumn(),
                            message,
                            null)
                    );
                } else{
                    boolCheck = true;
                }
            }
        }
        if (!boolCheck){
            var message = "The array statement has invalid variables.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }
        System.out.println(node.getChildren().get(1));
        var var2 = getNodeType(node.getChildren().getFirst(), table);
        var var3 = getNodeType(node.getChildren().getLast(), table);
        if (!(TypeUtils.typeCheck(var2,"int",false) && TypeUtils.typeCheck(var3,"int",false))) {
            var message = "The array access expression has invalid types.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitMethodCallExpr(JmmNode methodNode, SymbolTable table) {
        String calledMethod = methodNode.get("method");
        System.out.println(isStatic);
        System.out.println(calledMethod);
        var first = getNodeType(methodNode.getChildren().getFirst(), table);
        if (first.getName().equals(table.getClassName())) {
            if (table.getSuper()!=null) {
                return null;
            }
            for (var method: table.getMethods()){
                if (method.equals(calledMethod)) {
                    var parameters = table.getParameters(calledMethod);
                    for (int i = 0; i < parameters.size(); i++) {
                        var parameterType = parameters.get(i).getType();
                        var typeCheck = getNodeType(methodNode.getChildren().get(i+1), table);
                        if (!(TypeUtils.equalTypes(parameterType, typeCheck))) {
                            var message = "The parameters are incorrect.";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    methodNode.getLine(),
                                    methodNode.getColumn(),
                                    message,
                                    null)
                            );
                            return null;
                        }
                    }
                    return null;
                }
            }
        }
        else if (methodNode.getChildren().getFirst().getKind().equals("ObjectCallExpr")) {
            if (isStatic) {
                var message = "Cannot use this inside a static method";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodNode.getLine(),
                        methodNode.getColumn(),
                        message,
                        null)
                );
                return null;
            }
            for (var method : table.getMethods()){
                if (method.equals(methodNode.get("method"))){

                    return null;
                }
            }
        }
        else{
            if(hasImport(table, methodNode.getChild(0).get("name")) || hasImport(table, first.getName())) {
                return null;
            }
        }
        var message = "The method call is incorrect.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                methodNode.getLine(),
                methodNode.getColumn(),
                message,
                null)
        );
        return null;
    }


    private Void visitType(JmmNode typeNode, SymbolTable table) {
        if (typeNode.getKind().equals("Varargs")) {
            var a = typeNode.getParent().getKind();
            if (!a.equals("Param")) {
                var message = "Varargs should be a parameter.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        typeNode.getLine(),
                        typeNode.getColumn(),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitNegationExpr(JmmNode node, SymbolTable table) {
        var type = getNodeType(node.getChildren().getFirst(), table);
        if (!(TypeUtils.typeCheck(type, "boolean", false))) {
                var message = "Negation expression has invalid type.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        node.getLine(),
                        node.getColumn(),
                        message,
                        null)
                );
        }
        return null;
    }

    private Void visitIncrementExpr(JmmNode node, SymbolTable table) {
        var type = getNodeType(node.getChildren().getFirst(), table);
        if (!TypeUtils.typeCheck(type, "int", false)) {
            var message = "Increment should be an integer.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }
}

