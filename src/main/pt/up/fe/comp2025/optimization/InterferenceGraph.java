package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.Instruction;

import java.util.*;
import java.util.stream.Collectors;

public class InterferenceGraph {
    private final Map<String, Set<String>> graph;
    private final Map<String, Integer> degreeMap;
    private final Set<String> precolored;
    private final Method method;
    private int configRegisters;

    public InterferenceGraph(Method method, LivenessAnalysis liveness) {
        this.method = method;
        this.graph = new HashMap<>();
        this.degreeMap = new HashMap<>();
        this.precolored = new HashSet<>();
        this.configRegisters = 0;

        initializePrecolored();
        buildGraph(liveness);
    }

    private void initializePrecolored() {
        // Add 'this' if it's an instance method
        if (!method.isStaticMethod()) {
            precolored.add("this");
        }

        // Add parameter names
        for (Element param : method.getParams()) {
            if (param instanceof Operand paramOperand) {
                precolored.add(paramOperand.getName());
            }
        }
    }

    private void buildGraph(LivenessAnalysis liveness) {
        Set<String> allVariables = collectAllVariables();

        for (String var : allVariables) {
            graph.put(var, new HashSet<>());
            degreeMap.put(var, 0);
        }

        for (Instruction instruction : method.getInstructions()) {
            List<Operand> defs = liveness.getDefList(instruction);
            List<Operand> liveOuts = liveness.getOutList(instruction);

            // For each defined variable:
            for (Operand def : defs) {
                String defVar = def.getName();

                // 1. Interferes with all live-out variables
                for (Operand out : liveOuts) {
                    String outVar = out.getName();
                    if (!defVar.equals(outVar)) {
                        addEdge(defVar, outVar);
                    }
                }
            }
        }
    }


    private Set<String> collectAllVariables() {
        Set<String> variables = new HashSet<>();

        if (!method.isStaticMethod()) {
            variables.add("this");
        }

        for (Element param : method.getParams()) {
            if (param instanceof Operand) {
                variables.add(((Operand) param).getName());
            }
        }

        for (Map.Entry<String, Descriptor> entry : method.getVarTable().entrySet()) {
            String varName = entry.getKey();
            Descriptor descriptor = entry.getValue();

            if (!varName.equals("this") && descriptor.getScope() != VarScope.PARAMETER) {
                variables.add(varName);
            }
        }

        return variables;
    }

    private void addEdge(String a, String b) {
        if (!graph.get(a).contains(b)) {
            graph.get(a).add(b);
            degreeMap.put(a, degreeMap.get(a) + 1);
        }
        if (!graph.get(b).contains(a)) {
            graph.get(b).add(a);
            degreeMap.put(b, degreeMap.get(b) + 1);
        }
    }

    public Map<String, Integer> colorGraph() {
        Stack<String> stack = new Stack<>();
        Map<String, Integer> colors = new HashMap<>();
        Set<String> remaining = new HashSet<>(graph.keySet());

        // Assign fixed registers to precolored
        int firstAvailableReg = precolored.isEmpty() ? 0 : precolored.size();

        for (String var : precolored) {
            Descriptor desc = method.getVarTable().get(var);
            if (desc != null) {
                colors.put(var, desc.getVirtualReg());
            } else if (var.equals("this")) {
                colors.put("this", 0);
            }
            remaining.remove(var);
        }

        // Simplify
        while (!remaining.isEmpty()) {
            Optional<String> candidate = remaining.stream().findFirst();
            String var = candidate.get();
            stack.push(var);
            remaining.remove(var);
            decrementNeighbors(var);
        }

        // Select
        while (!stack.isEmpty()) {
            String var = stack.pop();
            Set<Integer> forbiddenColors = graph.get(var).stream()
                    .filter(colors::containsKey)
                    .map(colors::get)
                    .collect(Collectors.toSet());

            int color = firstAvailableReg;
            while (forbiddenColors.contains(color)) {
                color++;
            }
            configRegisters = Math.max(configRegisters, color-firstAvailableReg+1);
            colors.put(var, color);
        }

        return colors;
    }

    private void decrementNeighbors(String var) {
        for (String neighbor : graph.get(var)) {
            if (!precolored.contains(neighbor)) {
                degreeMap.put(neighbor, degreeMap.get(neighbor) - 1);
            }
        }
    }

    public Map<String, Descriptor> getNewTable() {
        Map<String, Integer> coloring = colorGraph();
        Map<String, Descriptor> newTable = new HashMap<>();

        for (Map.Entry<String, Descriptor> entry : method.getVarTable().entrySet()) {
            String varName = entry.getKey();
            Descriptor oldDesc = entry.getValue();

            int newReg = coloring.getOrDefault(varName, oldDesc.getVirtualReg());

            newTable.put(varName, new Descriptor(
                    oldDesc.getScope(),
                    newReg,
                    oldDesc.getVarType()
            ));
        }

        return newTable;
    }

    public int getConfigRegisters() {
        return configRegisters;
    }

    public Map<String, Set<String>> getGraph() {
        return Collections.unmodifiableMap(graph);
    }
}
