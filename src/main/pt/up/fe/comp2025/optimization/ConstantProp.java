package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

public class ConstantProp  extends AJmmVisitor<Boolean, Boolean> {
    private final Map<String, JmmNode> constants = new HashMap<>();
    private final Map<String, List<JmmNode>> toRemove = new HashMap<>();
    private final Map<String, JmmNode> variables = new HashMap<>();
    private final SymbolTable symbolTable;
    private String currentMethod;
    private int changes = 0;
    private boolean extraOptimization = false;

    public ConstantProp(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor(){
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(BOOLEAN_EXPR, this::visitBooleanOrVarExpr);
        addVisit(VAR_REF_EXPR, this::visitBooleanOrVarExpr);
        addVisit(WHILE_STMT, this::visitWhileStmt);

        setDefaultVisit(this::defaultVisit);

    }

    private Boolean visitWhileStmt(JmmNode jmmNode, Boolean aBoolean) {
        System.out.println("Inside while stmt...\n");
        System.out.println("Children: " + jmmNode.getChildren());

        // Visit the condition to process any constants
        visit(jmmNode.getChild(0), aBoolean);

        visit(jmmNode.getChild(1), aBoolean);

        return aBoolean;
    }

    private Boolean visitBooleanOrVarExpr(JmmNode jmmNode, Boolean aBoolean) {
        System.out.println("Inside boolean / Var expr..\n");


        if (jmmNode.getParent().getKind().equals("AssignStmt")){
            if ((jmmNode.getParent().getChildren().getFirst().getKind().equals("VarRefExpr") && jmmNode.getParent().getChildren().getFirst().get("name").equals(jmmNode.get("name")) ) ){
                return false;
            }
            else if ((jmmNode.getParent().getChildren().getFirst().getKind().equals("BooleanExpr") && jmmNode.getParent().getChildren().getFirst().get("value").equals(jmmNode.get("value")))){
                return false;
            }
        }


        int index = jmmNode.getIndexOfSelf();
        if (jmmNode.getParent().getChildren().get(index).getKind().equals("VarRefExpr")){
            if (constants.containsKey(jmmNode.get("name"))){
                JmmNode optimizedNode = constants.get(jmmNode.get("name"));
                JmmNode parent = jmmNode.getParent();
                parent.removeChild(index);
                parent.add(optimizedNode,index);
                System.out.println("New parent: " + parent.getChildren());
                changes++;
            }
        }
        else if (jmmNode.getParent().getChildren().get(index).getKind().equals("BooleanExpr")){
            if (constants.containsKey(jmmNode.get("value"))){
                JmmNode optimizedNode = constants.get(jmmNode.get("value"));
                JmmNode parent = jmmNode.getParent();
                parent.removeChild(index);
                parent.add(optimizedNode,index);
                changes++;
            }
        }

        return true;
    }

    private Boolean visitVarDecl(JmmNode jmmNode, Boolean aBoolean) {
        variables.put(jmmNode.get("name"), jmmNode);
        return true;
    }



    private Boolean visitAssignStmt(JmmNode jmmNode, Boolean aBoolean) {


        JmmNode left = jmmNode.getChild(0);
        JmmNode right = jmmNode.getChild(1);

        visit(left,aBoolean);
        visit(right,aBoolean);

        if (!(left.getKind().equals("VarRefExpr"))){
            return false;
        }


        boolean isBooleanExpr = right.getKind().equals("BooleanExpr") && (right.get("value").equals("true") || right.get("value").equals("false"));
        boolean isIntegerLiteral = right.getKind().equals("IntegerLiteral");
        String leftName = left.get("name");

        if ((!isIntegerLiteral && !isBooleanExpr)) {
            constants.remove(leftName);
        }
        else {
            List<Symbol> localVariables = symbolTable.getLocalVariables(currentMethod);

            boolean isLocal = false;
            for (var localVariable : localVariables){
                if (localVariable.getName().equals(leftName)){
                    isLocal = true;
                    break;
                }
            }

            if (isLocal){
                constants.put(leftName,right);
                if (!toRemove.containsKey(leftName)){
                    toRemove.put(leftName,new ArrayList<>());
                }
                toRemove.get(leftName).add(jmmNode);
            }
        }

        return true;
    }

    private Boolean visitMethodDecl(JmmNode jmmNode, Boolean aBoolean) {
        currentMethod = jmmNode.get("name");

        //New Method, clear old constants and check for new ones
        constants.clear();
        toRemove.clear();
        variables.clear();

        for (var child : jmmNode.getChildren()) {
            visit(child, aBoolean);
        }

        if (extraOptimization) constProp();

        return true;
    }


    private void constProp() {

        System.out.println("Using constant propagation to switch constants to their value...\n");
        for(Map.Entry<String, JmmNode> assignment : constants.entrySet()) {
            if(toRemove.containsKey(assignment.getKey())) {
                List<JmmNode> assigns = toRemove.get(assignment.getKey());
                for(JmmNode assign : assigns) {
                    assign.getParent().removeChild(assign);
                    changes++;
                }
            }
            if(variables.containsKey(assignment.getKey())) {
                JmmNode declaration = variables.get(assignment.getKey());
                declaration.getParent().removeChild(declaration);
                changes++;
            }
        }
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

    public void activateExtraOptimization(){
        extraOptimization = true;
    }
}
