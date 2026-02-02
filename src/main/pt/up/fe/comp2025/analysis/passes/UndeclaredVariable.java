package pt.up.fe.comp2025.analysis.passes;

import org.antlr.v4.runtime.misc.Pair;
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
import java.util.Objects;
import java.util.Set;

import static pt.up.fe.comp2025.ast.TypeUtils.getNodeType;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitRetExpr);
        addVisit(Kind.NEW_CLASS, this::visitNewClassExpr);
        addVisit(Kind.ARRAY_INIT_EXPR, this::visitArrayInitExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.CLASS_METHOD_CALL_EXPR, this::visitClassMethodCallExpr);
    }

    private Void visitImportDecl(JmmNode import_, SymbolTable symbolTable) {
        String raw = import_.get("name");
        String name = String.join(".", raw.replaceAll("[\\[\\]\\s]", "").split(","));
        if (!hasImport(symbolTable, name)) {
            System.out.println("Import " + name + " not found");
            return null;
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        //Ver o tipo do metodo
        if (method.get("name").equals("void")) {
            var message = "Cannot use void as a name of a method";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    method.getLine(),
                    method.getColumn(),
                    message,
                    null)
            );
        }
        if (!table.getReturnType(method.get("name")).getName().equals("void")) {
            if (!method.getChildren().getLast().getKind().equals("ReturnStmt")) {
                var message = "Return statement is missing";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        message,
                        null)
                );
            }
        }
        currentMethod = method.get("name");

        return null;
    }

    private Void visitParam(JmmNode param, SymbolTable table) {
        var type = param.getChild(0);
        if (type.getKind().equals("Id")) {
            var name = type.get("name");
            if (!hasImport(table, name) && !table.getClassName().equals(name)) {
                var message = String.format("Parameter of type %s does not exist.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        param.getLine(),
                        param.getColumn(),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitRetExpr(JmmNode statement, SymbolTable table) {
        //se for void nao pode ter return (no nosso caso, porque nao temos "return null;")
        var type = table.getReturnType(currentMethod);
        var typeName = type.getName();
        if (typeName.equals("void")) {
            var message = "The type of the expression in a return statement is not compatible with the method return type (void).";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    statement.getLine(),
                    statement.getColumn(),
                    message,
                    null)
            );
        }

        if (getNodeType(statement.getChild(0), table).getName().equals("everything")){
            return null;
        }

        if (!(Objects.equals(table.getReturnType(currentMethod).getName(), getNodeType(statement.getChild(0), table).getName()) &&
        Objects.equals(table.getReturnType(currentMethod).isArray(), getNodeType(statement.getChild(0), table).isArray()))) {
            var message = "The type of the expression in a return statement is not compatible with the method return type.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    statement.getLine(),
                    statement.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitNewClassExpr(JmmNode newClass, SymbolTable table) {
        var className = newClass.get("name");
        if (hasImport(table, className)) {
            return null;
        }

        if (table.getClassName().equals(className)) {
            return null;
        }
        var message = String.format("Class not imported.");
        addReport(Report.newError(
                Stage.SEMANTIC,
                newClass.getLine(),
                newClass.getColumn(),
                message,
                null)
        );

        return null;
    }

    private Void visitArrayInitExpr(JmmNode arrayInitExpr, SymbolTable table) {
        for (var child: arrayInitExpr.getChildren()) {
            var nodeType = getNodeType(child, table);
            if (!(nodeType.getName() == "int" && !nodeType.isArray())) { //Não sabemos se pode ter 1 variavel na lista tipo [10, a, 30]
                var message = String.format("Array value of type %s is not supported.", child.getKind());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        arrayInitExpr.getLine(),
                        arrayInitExpr.getColumn(),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var first = getNodeType(assignStmt.getChildren().getFirst(), table);
        var last = getNodeType(assignStmt.getChildren().getLast(), table);
        if (hasImport(table,first.getName()) && hasImport(table,last.getName())){
            return null;
        };
        if (last.getName().equals("this")) {
            System.out.println(table.getClassName());
            System.out.println(first.getName());
            if (first.getName().equals(table.getClassName()) | first.getName().equals(table.getSuper())){
                return null;
            }
            else{
                var message = String.format("Wrong usage of this as an Object.", last.getName(), first.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        assignStmt.getLine(),
                        assignStmt.getColumn(),
                        message,
                        null)
                );
            }
        }
        else if (!(TypeUtils.equalTypes(first, last) || table.getSuper() != null && first.getName().equals(table.getSuper()) && last.getName().equals(table.getClassName()))) {
            var message = String.format("%s is not a %s.", last.getName(), first.getName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    assignStmt.getLine(),
                    assignStmt.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {

        if (classDecl.hasAttribute("superClass")) {
            if (!hasImport(table, classDecl.get("superClass"))) {
                var message = String.format("Super class %s does not exist.", classDecl.get("superClass"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null)
                );
            }
        }
        Set<String> seenImports = new HashSet<>();
        for (String imp : table.getImports()) {
            String className = imp.contains(".") ? imp.substring(imp.lastIndexOf('.') + 1) : imp;
            if (!seenImports.add(className)) {
                var message = String.format("The import %s is already declared", imp);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null)
                );
            }
        }

        //Checka se tem metodos duplicados
        Set<String> seenMethods = new HashSet<>();
        for (var method: table.getMethods()) {
            if (!seenMethods.add(method)) {
                var message = String.format("The method %s is already declared", method);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null)
                );
            }
        }

        //Checka se tem fields duplicados
        Set<String> seenFields = new HashSet<>();
        for (var field: table.getFields()) {
            if (!seenFields.add(field.getName())) {
                var message = String.format("The field %s is already declared", field.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null)
                );
            }
        }

        //Checka se tem parametros duplicados para cada metodo
        Set<String> seenParams = new HashSet<>();
        for (var methodName: table.getMethods()) {
            var methodParams = table.getParameters(methodName);
            if(!methodParams.isEmpty()) {
                seenParams.clear();
                for (var param: methodParams) {
                    if (!seenParams.add(param.getName())) {
                        var message = String.format("Parameter %s is already declared", param.getName());
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                classDecl.getLine(),
                                classDecl.getColumn(),
                                message,
                                null)
                        );
                    }
                }
            }

        }

        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        if ( varDecl.get("name").equals("void")) {
            var message = "Cannot create a variable with the name \"void\"";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    varDecl.getLine(),
                    varDecl.getColumn(),
                    message,
                    null)
            );
        }
        var type = varDecl.getChild(0);
        if (type.getKind().equals("Id")) {
            var name = type.get("name");
            if (!hasImport(table, name) && !table.getClassName().equals(name)) {
                var message = String.format("Variable of type %s does not exist.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        varDecl.getLine(),
                        varDecl.getColumn(),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitClassMethodCallExpr(JmmNode methodCall, SymbolTable table) {
        var methodName = methodCall.get("method");
        boolean wrongCall = true;
        for (var method: table.getMethods()) {
            if (methodName.equals(method)) {
                var parameters = table.getParameters(methodName);
                System.out.println("Childrens are :" + methodCall.getChildren());
                if (parameters.size() != methodCall.getChildren().size()) {
                    var message = "The parameters are incorrect.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            methodCall.getLine(),
                            methodCall.getColumn(),
                            message,
                            null)
                    );
                    return null;
                }
                for (int i = 0; i < parameters.size(); i++) {
                    var parameterType = parameters.get(i).getType();
                    var typeCheck = getNodeType(methodCall.getChildren().get(i), table).getName().equals("this") ? new Type(table.getClassName(), false) : getNodeType(methodCall.getChildren().get(i), table);
                    if (!(TypeUtils.equalTypes(parameterType, typeCheck))) {
                        System.out.println("Here");
                        var message = "The parameters are incorrect.";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                methodCall.getLine(),
                                methodCall.getColumn(),
                                message,
                                null)
                        );
                        return null;
                    }
                }
                wrongCall = false;
                break;
            }
        }
        if (wrongCall) {
            var message = String.format("Wrong class method call: %s", methodName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    methodCall.getLine(),
                    methodCall.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }
}
