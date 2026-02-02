package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String IMPORT = "import";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table, ollirTypes, types);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(IMPORT_DECL, this::visitImport);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(ARRAY_STMT, this::visitArrayStmt);
        //setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {


        var rhs = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();
        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        var left = node.getChild(0);
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);
        if (ollirTypes.checkIfField(left.get("name"), node, table)) {
            code.append("putfield(this, " + left.get("name") + typeString + ", " + rhs.getCode() + ")" + ".V" + END_STMT);
        }else {
            var varCode = left.get("name") + typeString;

            if (!varCode.equals(rhs.getCode())) {
                code.append(varCode);
                code.append(SPACE);

                code.append(ASSIGN);
                code.append(typeString);
                code.append(SPACE);

                code.append(rhs.getCode());

                code.append(END_STMT);
            }
        }
        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        // DONE BY the GOat
        Type retType = types.getRetType(node);

        StringBuilder code = new StringBuilder();


        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;

        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);

        System.out.println(expr.getCode());

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = ollirTypes.toOllirType(node.getChild(0));
        var id = node.get("name");
        String code = id + typeCode;
        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = node.getBoolean("isPublic", false);

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        // DONE BY GOAT PEDRO BORGES

        code.append("(");

        for (var child : node.getChildren(PARAM)) {
            var visited = visit(child);
            if (child != node.getChildren(PARAM).getLast()) code.append(visited +  ",");
            else code.append(visited);
        }

        code.append(")");


        // type
        // DONE BY THE GOAT PEDRO BORGES
        var retType = ollirTypes.toOllirType(node.getChildren().getFirst());
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var stmtsCode = node.getChildren(STMT).stream()
                .map(this::visit)
                .collect(Collectors.joining("\n   ", "   ", ""));

        if (!node.getChildren(RETURN_STMT).isEmpty()) stmtsCode = stmtsCode + visit(node.getChildren(RETURN_STMT).getLast());

        code.append(stmtsCode);

        if (node.getChildren(RETURN_STMT).isEmpty()) {
            code.append(NL);
            code.append("ret.V;");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var parentNode = node.getParent();
        var varName = node.get("name");
        if (parentNode.getKind().equals("ClassDecl")){
            code.append(".field public " + varName);
            for (var field : table.getFields()) {
                if (varName.equals(field.getName())) {
                    code.append(ollirTypes.toOllirType(field.getType()) + END_STMT);
                    return code.toString();
                }
            }
        }
        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        if (table.getSuper() != null) {
            code.append(" extends ").append(table.getSuper());
        }
        
        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

        for (var child : node.getChildren(VAR_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(NL);

        code.append(buildConstructor());
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String visitImport(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(IMPORT);

        code.append(SPACE);

        System.out.println(node);

        String fullImport = String.join(".", node.get("name").replaceAll("[\\[\\]\\s]", "").split(","));

        code.append(fullImport);

        code.append(END_STMT);

        return code.toString();
    }

    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var exprResult = exprVisitor.visit(node.getChild(0));
        System.out.println("Result is :" + exprResult.getComputation());
        code.append(exprResult.getComputation());
        code.append(exprResult.getCode());
        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var exprResult = exprVisitor.visit(node.getChild(0));
        var exprStmt = node.getChild(1).getChildren();
        System.out.println(node.getChild(1).getChildren("Brackets"));
        var whileTemp = ollirTypes.nextTemp("while");
        var ifTemp = ollirTypes.nextTemp("endif");
        code.append(whileTemp + ":\n");
        code.append(exprResult.getComputation());
        code.append("if(!.bool" + SPACE + exprResult.getCode() + ")" + SPACE + "goto " + ifTemp + END_STMT);
        if (!exprStmt.isEmpty()) {
            for (var stmt : exprStmt){
                var stmtCode = visit(stmt);
                code.append(stmtCode);
            }
        }
        code.append("goto " + whileTemp + END_STMT);
        code.append(ifTemp + ":\n");
        return code.toString();
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var ifExpr = exprVisitor.visit(node.getChild(0));
        List<String> thenTemps = new ArrayList<>();
        thenTemps.add(ollirTypes.nextTemp("then"));
        List<String> endifTemps = new ArrayList<>();
        endifTemps.add(ollirTypes.nextTemp("endif"));

        code.append(ifExpr.getComputation());

        code.append("if (")
                .append(ifExpr.getCode())
                .append(") goto ")
                .append(thenTemps.getFirst())
                .append(END_STMT);

        int countElseIfs = 0;
        while (true) {
            countElseIfs++;
            var elseIfExpr = node.getChild(countElseIfs);
            if (elseIfExpr.getKind().equals("ElseifExpr")) {
                thenTemps.add(ollirTypes.nextTemp("then"));
                endifTemps.add(ollirTypes.nextTemp("endif"));
                var elseIfExprResult = exprVisitor.visit(elseIfExpr);
                code.append(elseIfExprResult.getComputation());
                code.append("if (")
                        .append(elseIfExprResult.getCode())
                        .append(") goto ")
                        .append(thenTemps.get(countElseIfs))
                        .append(END_STMT);
            } else {
                countElseIfs--;
                break;
            }
        }
        code.append(NL);

        var elseStmts = node.getChild(countElseIfs+1).getChild(0);
        if (elseStmts != null) {
            for (var statement :elseStmts.getChildren()) {
                code.append(visit(statement, unused));
            }
        }

        code.append("goto ")
                .append(endifTemps.get(countElseIfs))
                .append(END_STMT);

        code.append(thenTemps.get(countElseIfs))
                .append(":")
                .append(NL);
        code.append(NL);

        for (int i = countElseIfs; i > 1; i--) {
            var elseIfExpr = node.getChild(i).getChild(1);
            for (var statement: elseIfExpr.getChildren()) {
                code.append(visit(statement, unused));
            }
            code.append(endifTemps.get(i))
                    .append(":")
                    .append(NL).append(NL);
            code.append("goto ")
                    .append(endifTemps.get(i-1))
                    .append(END_STMT);
            code.append(thenTemps.get(i-1))
                    .append(":")
                    .append(NL).append(NL);
        }



        var exprStmt = node.getChild(0).getChildren("Brackets");
        if (!exprStmt.isEmpty()) {
            for (var statement: exprStmt.getFirst().getChildren()) {
                code.append(visit(statement, unused));
            }
        }
        code.append(endifTemps.getFirst()).append(":").append(NL);
        return code.toString();
    }

    private String visitArrayStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        String name;
        if (ollirTypes.checkIfField(node.get("name"), node , table)){
            var temp = ollirTypes.nextTemp();
            name = temp + ".array.i32";
            code.append( temp + ".array.i32" + SPACE + ASSIGN + ".array.i32" + SPACE + "getfield(this, " + node.get("name") + ".array.i32" + ")" + ".array.i32" + END_STMT);
        }
        else {
            name = node.get("name");
        }
        var number = exprVisitor.visit(node.getChild(0));
        var rhs = exprVisitor.visit(node.getChild(1));
        code.append(number.getComputation());
        code.append(rhs.getComputation());
        var left = node.getChild(0);
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);
        code.append(name + "[" + number.getCode() + "]" + typeString + " " + ASSIGN);
        code.append(typeString + SPACE + rhs.getCode() + END_STMT);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
