package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2025.ast.Kind.*;

public class JmmSymbolTableBuilder {

    // In case we want to already check for some semantic errors during symbol table building.
    private List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
    }

    public JmmSymbolTable build(JmmNode root) {

        reports = new ArrayList<>();

        List<String> importStatements = buildImports(root);
        JmmNode classDecl = null;
        for (var child: root.getChildren()) {
            if (Kind.CLASS_DECL.check(child)) {
                classDecl = child;
            }
        }
        SpecsCheck.checkArgument(classDecl != null, () -> "Expected at least one class declaration.");
        String className = classDecl.get("name");
        String superClassName = null;
        if(classDecl.hasAttribute("superClass")) superClassName = classDecl.get("superClass");
        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(importStatements, className, superClassName, fields, methods, returnTypes, params, locals);
    }

    private List<String> buildImports(JmmNode root) {
        List<String> importStatements = new ArrayList<>();

        for (var child : root.getChildren()) {
            if (Kind.IMPORT_DECL.check(child)) {
                String importParts = child.get("name").substring(1, child.get("name").length() - 1);
                importStatements.add(String.join(".", importParts.split(", ")));
            }
        }

        return importStatements;
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            Type returnType = TypeUtils.newVoidType();
            if (!name.equals("main")) {
                var returnT = method.getChildren(TYPE).getFirst();
                returnType = TypeUtils.convertType(returnT);
            }
            map.put(name, returnType);
        }

        return map;
    }


    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var params = method.getChildren(PARAM).stream()
                    .map(param -> {
                        var typeNode = param.getChildren().get(0);
                        var type = TypeUtils.convertType(typeNode);
                        var paramName = param.get("name");

                        return new Symbol(type, paramName);
                    }).toList();

            map.put(name, params);
        }

        return map;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        var map = new HashMap<String, List<Symbol>>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var locals = new ArrayList<Symbol>();

            for (var varDecl : method.getChildren(VAR_DECL)) {
                var nodeType = varDecl.getChild(0); // Get the type name

                Type type = TypeUtils.convertType(nodeType);

                locals.add(new Symbol(type, varDecl.get("name")));
            }

            map.put(name, locals);
        }

        return map;
    }

    private List<String> buildMethods(JmmNode classDecl) {

        var methods = classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();

        return methods;
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        for (var varDecl : classDecl.getChildren(VAR_DECL)) {

            var typeNode = varDecl.getChild(0);
            var type = TypeUtils.convertType(typeNode);
            var fieldName = varDecl.get("name");

            fields.add(new Symbol(type, fieldName));
        }

        return fields;
    }

}
