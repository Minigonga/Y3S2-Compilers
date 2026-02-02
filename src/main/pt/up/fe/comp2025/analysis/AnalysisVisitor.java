package pt.up.fe.comp2025.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of AnalysisPass that automatically visits nodes using preorder traversal.
 */
public abstract class AnalysisVisitor extends PreorderJmmVisitor<SymbolTable, Void> implements AnalysisPass {

    private List<Report> reports;

    public AnalysisVisitor() {
        reports = new ArrayList<>();
        setDefaultValue(() -> null);
    }

    protected void addReport(Report report) {
        reports.add(report);
    }

    protected List<Report> getReports() {
        return reports;
    }


    @Override
    public List<Report> analyze(JmmNode root, SymbolTable table) {
        // Visit the node
        visit(root, table);

        // Return reports
        return getReports();
    }

    public Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
    }


    public Boolean hasImport(SymbolTable table, String importName) {
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
}
