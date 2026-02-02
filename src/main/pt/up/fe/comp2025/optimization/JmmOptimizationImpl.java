package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // Create visitor that will generate the OLLIR code
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());

        // Visit the AST and obtain OLLIR code
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        System.out.println("\nOLLIR:\n\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        var config = semanticsResult.getConfig();
        if (config.containsKey("optimize") && config.get("optimize").equals("true")) {

        }
        if (semanticsResult.getConfig().containsKey("optimize") && semanticsResult.getConfig().get("optimize").equals("true")) {

            ConstantProp constantPropagation = new ConstantProp(semanticsResult.getSymbolTable());
            ConstantFold constantFolding = new ConstantFold();

            if (semanticsResult.getConfig().containsKey("extra") && semanticsResult.getConfig().get("extra").equals("true")) {
                constantPropagation.activateExtraOptimization();
            }

            do {
                constantPropagation.visit(semanticsResult.getRootNode());
                constantFolding.visit(semanticsResult.getRootNode());
            }while(constantPropagation.getChanges() > 0 || constantFolding.getChanges() > 0);

        }

        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        var config = ollirResult.getConfig();
        if (config.containsKey("registerAllocation") && Integer.parseInt(ollirResult.getConfig().get("registerAllocation")) >= 0) {
            int numRegisters = Integer.parseInt(ollirResult.getConfig().get("registerAllocation"));
            ClassUnit classUnit = ollirResult.getOllirClass();
            classUnit.buildCFGs();
            classUnit.buildVarTables();
            for (Method method : classUnit.getMethods()) {
                // Perform liveness analysis
                LivenessAnalysis liveness = new LivenessAnalysis(method);
                liveness.analyze();

                // Build and color interference graph
                InterferenceGraph ig = new InterferenceGraph(method, liveness);
                Map<String, Descriptor> newTable = ig.getNewTable();

                if (numRegisters != 0 && ig.getConfigRegisters() > numRegisters) {
                    throw new RuntimeException("Number of config registers requested is greater than the number of registers available: " + ig.getConfigRegisters() + " > " + numRegisters);
                }

                // Update the table to the new one
                method.getVarTable().putAll(newTable);

                Map<String, Descriptor> table = method.getVarTable();
                System.out.println("Var table after optimization:\n" + table);
            }
        }
        return ollirResult;
    }


}
