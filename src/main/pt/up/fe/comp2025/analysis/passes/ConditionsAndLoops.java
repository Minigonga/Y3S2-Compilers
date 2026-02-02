package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import static pt.up.fe.comp2025.ast.TypeUtils.*;

public class ConditionsAndLoops extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_EXPR,this::visitIfExpr);
        addVisit(Kind.ELSEIF_EXPR,this::visitIfExpr);
        addVisit(Kind.WHILE_STMT,this::visitWhileExpr);
    }

    private Void visitIfExpr(JmmNode node, SymbolTable table) {
        var expr = node.getChildren().getFirst();

        Type exprType = getNodeType(expr, table);
        if(!(TypeUtils.typeCheck(exprType,"boolean",false))){
            var message = "If condition does not have a type boolean";
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

    private Void visitWhileExpr(JmmNode node, SymbolTable table) {
        var expr = node.getChildren().getFirst();

        Type exprType = getNodeType(expr, table);
        if(!(TypeUtils.typeCheck(exprType,"boolean",false))){
            var message = "While condition does not have a type boolean";
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
