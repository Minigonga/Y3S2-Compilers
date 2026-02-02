package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";

    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;

    public OllirExprGeneratorVisitor(SymbolTable table, OptUtils ollirTypes, TypeUtils types) {
        this.table = table;
        this.types = types; // OK to keep this private
        this.ollirTypes = ollirTypes;      // Use the passed-in instance
    }


    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(PARENTHESIS_EXPR, this::visitParenthesisExpr);
        addVisit(NEGATION, this::visitNegationExpr);
        addVisit(BINARY_EXPR, this::visitBinExpr); //repeated code
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(OBJECT_CALL_EXPR, this::visitObjectCallExpr);
        addVisit(ARRAY_DECLARATION, this::visitArrayDeclaration);
        addVisit(ARRAY_INIT_EXPR, this::visitArrayInitExpr);
        addVisit(IF_EXPR, this::visitIfExpr);
        addVisit(ELSEIF_EXPR, this::visitIfExpr);
        addVisit(BOOLEAN_EXPR, this::visitBooleanLiteral);
        addVisit(LENGTH, this::visitLengthExpr);
        addVisit(ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(NEW_CLASS, this::visitNewClassExpr);
        addVisit(CLASS_METHOD_CALL_EXPR, this::visitClassMethodCallExpr);
        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitParenthesisExpr(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    private OllirExprResult visitObjectCallExpr(JmmNode node, Void unused) {
        Type type = types.getExprType(node);
        String typeString = ollirTypes.toOllirType(type); //TODO num sei se troco
        return new OllirExprResult("this"+typeString);
    }

    private OllirExprResult visitNegationExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var nextNode = visit(node.getChild(0));
        var temp = ollirTypes.nextTemp();
        Type type = types.getExprType(node);
        String typeString = ollirTypes.toOllirType(type);
        computation.append(nextNode.getComputation());
        code.append(temp + typeString);
        computation.append(temp + typeString + SPACE + ASSIGN + typeString + SPACE + "!" + typeString + SPACE + nextNode.getCode() + END_STMT);
        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitArrayInitExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String tempVar = ollirTypes.nextTemp() + ".array.i32";
        computation.append(tempVar)
                .append(" :=.array.i32 new(array, ")
                .append(node.getChildren().size() + ".i32")
                .append(").array.i32")
                .append(END_STMT);

        code.append(tempVar);
        int count = 0;
        for (var nodeChild : node.getChildren()) {
            var visited = visit(nodeChild);
            computation.append(visited.getComputation());
            computation.append(ollirTypes.arrayTempCut(tempVar) + "[" + count++ + ".i32" + "]" + ".i32" + SPACE + ASSIGN + ".i32 " + visited.getCode() + END_STMT);
        }
        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        String op = node.get("op");
        StringBuilder computation = new StringBuilder();
        var leftExprResult = visit(node.getChild(0));
        var rightExprResult = visit(node.getChild(1));
        if (op.equals("&&")) {
            StringBuilder code = new StringBuilder();
            computation.append(NL);
            String thenTemp = ollirTypes.nextTemp("then");
            String andTemp = ollirTypes.nextTemp("andTmp");
            String endifTemp = ollirTypes.nextTemp("endif");

            computation.append(leftExprResult.getComputation());
            computation.append("if (")
                    .append(leftExprResult.getCode())
                    .append(") goto ")
                    .append(thenTemp)
                    .append(END_STMT);

            computation.append(andTemp + ".bool ")
                    .append(ASSIGN)
                    .append(".bool ")
                    .append("0.bool")
                    .append(END_STMT);

            computation.append("goto ")
                    .append(endifTemp)
                    .append(END_STMT);

            computation.append(thenTemp)
                    .append(":")
                    .append(NL);
            computation.append(rightExprResult.getComputation());

            computation.append(NL);

            computation.append(andTemp + ".bool ")
                    .append(ASSIGN)
                    .append(".bool ")
                    .append(rightExprResult.getCode())
                    .append(END_STMT);

            computation.append(endifTemp).append(":").append("\n");

            code.append(andTemp).append(".bool");

            return new OllirExprResult(code.toString(), computation.toString());
        }

        // code to compute the children
        computation.append(leftExprResult.getComputation());
        computation.append(rightExprResult.getComputation());

        // code to compute self
        Type resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);
        String code;
        if (node.getParent().getKind().equals("AssignStmt")) {
            code = node.getParent().getChild(0).get("name") + resOllirType;
        } else {
            code = ollirTypes.nextTemp() + resOllirType;
        }

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(leftExprResult.getCode()).append(SPACE);

        Type type = types.getExprType(node);
        computation.append(node.get("op")).append(ollirTypes.toOllirType(type)).append(SPACE)
                .append(rightExprResult.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var id = node.get("name");
        Type type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);
        if (ollirTypes.checkIfField(id, node, table)){
            var temp = ollirTypes.nextTemp();
            computation.append( temp + ollirType + SPACE + ASSIGN + ollirType + SPACE + "getfield(this, " + id + ollirType + ")" + ollirType + END_STMT);
            code.append(temp + ollirType);
        }
        else {

            code.append(id + ollirType);
        }
        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitMethodCallExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder codeBuilder = new StringBuilder();

        JmmNode receiverNode = node.getChild(0);
        var visited = visit(node.getChild(0));
        var receiverCode = visited.getCode();
        computation.append(visited.getComputation());
        String methodName = node.get("method");

        //Visit all parameters
        List<OllirExprResult> paramResults = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            OllirExprResult paramResult = visit(node.getChild(i));
            computation.append(paramResult.getComputation());
            paramResults.add(paramResult);
        }

        // Get return type from your type system
        Type returnType = types.getExprType(node);
        String ollirReturnType = ollirTypes.toOllirType(returnType);

        // Determine invocation type based on receiver
        boolean isStaticCall = isStaticMethodCall(receiverNode);

        // Generate temp variable for non-void methods
        String resultVar = "";
        if (!returnType.getName().equals("void")) {
            resultVar = ollirTypes.nextTemp() + ollirReturnType;
            computation.append(resultVar).append(ASSIGN).append(ollirReturnType).append(" ");
        }
        if (isStaticCall) {
            codeBuilder.append("invokestatic(")
                    .append(receiverNode.get("name"))
                    .append(", \"")
                    .append(methodName)
                    .append("\"");
        } else {
            codeBuilder.append("invokevirtual(")
                    .append(receiverCode)
                    .append(", \"")
                    .append(methodName)
                    .append("\"");
        }

        for (OllirExprResult param : paramResults) {
            codeBuilder.append(", ").append(param.getCode());
        }

        codeBuilder.append(")").append(ollirReturnType);
        computation.append(codeBuilder).append(END_STMT);

        return returnType.getName().equals("void")
                ? new OllirExprResult("", computation.toString())
                : new OllirExprResult(resultVar, computation.toString());
    }

    private boolean isStaticMethodCall(JmmNode receiverNode) {
        if (receiverNode.getKind().equals("VarRefExpr")) {
            String receiverName = receiverNode.get("name");
            return Character.isUpperCase(receiverName.charAt(0)) || types.hasImport(receiverName);
        }
        return false;
    }

    private OllirExprResult visitArrayDeclaration(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        JmmNode sizeExpr = node.getChild(0);
        OllirExprResult sizeResult = visit(sizeExpr);
        computation.append(sizeResult.getComputation());

        // Generate array creation code
        String tempVar = ollirTypes.nextTemp() + ".array.i32";
        computation.append(tempVar)
                .append(" :=.array.i32 new(array, ")
                .append(sizeResult.getCode())
                .append(").array.i32")
                .append(END_STMT);

        code.append(tempVar);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitLengthExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        JmmNode variable = node.getChild(0);
        OllirExprResult variableResult = visit(variable);
        computation.append(variableResult.getComputation());

        // Generate array creation code
        String tempVar = ollirTypes.nextTemp() + ".i32";
        computation.append(tempVar)
                .append(" :=.i32 arraylength(" + variableResult.getCode() + ").i32")
                .append(END_STMT);

        code.append(tempVar);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        if (node.get("value").equals("true")) {
            return new OllirExprResult("1.bool");
        }
        return new OllirExprResult("0.bool");
    }

    private OllirExprResult visitIfExpr(JmmNode node, Void unused) {
        var condition = visit(node.getChild(0));

        return condition;
    }

    private OllirExprResult visitArrayAccessExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        OllirExprResult lhs = visit(node.getChild(0));
        OllirExprResult rhs = visit(node.getChild(1));

        String tempVar = ollirTypes.nextTemp();
        Type type = types.getExprType(node);
        String nodeType = ollirTypes.toOllirType(type);
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());
        computation.append(tempVar + nodeType + SPACE + ASSIGN + nodeType + SPACE + lhs.getCode() + "[" + rhs.getCode() + "]" + nodeType + END_STMT);
        code.append(tempVar+nodeType);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitNewClassExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String tempVar = ollirTypes.nextTemp();
        Type type = types.getExprType(node);
        String nodeType = ollirTypes.toOllirType(type);
        computation.append(tempVar + nodeType + SPACE + ASSIGN + nodeType + SPACE + "new(" + node.get("name") + ")" + nodeType + END_STMT);
        computation.append("invokespecial(" + tempVar + nodeType + ", \"<init>\").V" + END_STMT);
        code.append(tempVar + nodeType);
        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitClassMethodCallExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        var returnType = table.getReturnType(node.get("method"));
        String ollirReturnType = ollirTypes.toOllirType(returnType);

        List<OllirExprResult> paramResults = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            OllirExprResult paramResult = visit(node.getChild(i));
            computation.append(paramResult.getComputation());
            paramResults.add(paramResult);
        }

        computation.append("invokevirtual(")
                .append(table.getClassName())
                .append(", \"").append(node.get("method")).append("\"");

        for (OllirExprResult param : paramResults) {
            computation.append(", ").append(param.getCode());
        }

        computation.append(")").append(ollirReturnType).append(END_STMT);

        return new OllirExprResult("", computation.toString());
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
