package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

public class ConstantFold  extends AJmmVisitor<Boolean, Boolean> {

    private int changes = 0;

    protected void buildVisitor() {
        addVisit(BINARY_EXPR, this::visitBinaryExpr);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        setDefaultVisit(this::defaultVisit);

    }

    private Boolean visitWhileStmt(JmmNode jmmNode, Boolean aBoolean) {
        return aBoolean;
    }


    private Boolean visitBinaryExpr(JmmNode jmmNode, Boolean aBoolean) {
        System.out.println("\nVisiting BinaryOp: " + jmmNode);
        System.out.println("    JmmNode: " + jmmNode.getChildren());
        String op = jmmNode.get("op");

        visit(jmmNode.getChild(0), aBoolean);
        visit(jmmNode.getChild(1), aBoolean);

        if (!(op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/"))) {
            return false;
        }
        JmmNode left = jmmNode.getChild(0);
        JmmNode right = jmmNode.getChild(1);

        int foldValue = 0;

        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
            if (op.equals("-")){
                foldValue = Integer.parseInt(left.get("value")) - Integer.parseInt(right.get("value"));
            }
            else if (op.equals("+")){
                foldValue = Integer.parseInt(left.get("value")) + Integer.parseInt(right.get("value"));
            }
            else if (op.equals("*")){
                foldValue = Integer.parseInt(left.get("value")) * Integer.parseInt(right.get("value"));
            }
            else if (op.equals("/")){
                foldValue = Integer.parseInt(left.get("value")) / Integer.parseInt(right.get("value"));
            }

            JmmNode foldedNode = new JmmNodeImpl(List.of("IntegerLiteral"));
            foldedNode.put("value", String.valueOf(foldValue));
            int index = jmmNode.getIndexOfSelf();
            JmmNode parent = jmmNode.getParent();
            parent.removeChild(index);
            parent.add(foldedNode,index);
            changes++;

        }

        return aBoolean;
    }

    private Boolean defaultVisit(JmmNode node, Boolean unused) {

        for (var child : node.getChildren()) {
            visit(child, unused);
        }

        return unused;
    }

    public int getChanges() {
        int value = changes;
        changes = 0;
        return value;
    }



}
