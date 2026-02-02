package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.type.Type;

import java.util.*;

public class LivenessAnalysis {
    private Method method;
    private final Map<Node, List<Operand>> useLists;
    private final Map<Node, List<Operand>> defLists;
    private final Map<Node, List<Operand>> inLists;
    private final Map<Node, List<Operand>> outLists;


    public LivenessAnalysis(Method method) {
        this.useLists = new HashMap<>();
        this.defLists = new HashMap<>();
        this.inLists = new HashMap<>();
        this.outLists = new HashMap<>();
        this.method = method;
    }

    public void analyze() {
        buildUseLists();
        buildDefLists();
        initializeInOutSets();

        List<Instruction> instructions = new ArrayList<>(method.getInstructions());
        Collections.reverse(instructions);

        boolean changed;
        do {
            changed = false;

            Map<Instruction, List<Operand>> oldInLists = new HashMap<>();
            Map<Instruction, List<Operand>> oldOutLists = new HashMap<>();
            for (Instruction instruction : instructions) {
                oldInLists.put(instruction, new ArrayList<>(inLists.get(instruction)));
                oldOutLists.put(instruction, new ArrayList<>(outLists.get(instruction)));
            }

            for (Instruction instruction : instructions) {
                Set<Operand> out = new HashSet<>();
                for (Node succ : instruction.getSuccessors()) {
                    if (succ != null && succ.getNodeType() != NodeType.END) { // skip END nodes
                        List<Operand> succIn = inLists.get(succ);
                        if (succIn != null) {
                            out.addAll(succIn);
                        }
                    }
                }
                outLists.put(instruction, new ArrayList<>(out));

                Set<Operand> in = new HashSet<>(useLists.get(instruction));
                Set<Operand> tempOut = new HashSet<>(out);
                List<Operand> removing = defLists.get(instruction);
                for (Operand defOperand : removing) {
                    tempOut.removeIf(op -> op.getName().equals(defOperand.getName()));
                }
                in.addAll(tempOut);
                inLists.put(instruction, new ArrayList<>(in));
            }

            for (Instruction instruction : instructions) {
                if (!inLists.get(instruction).equals(oldInLists.get(instruction)) ||
                        !outLists.get(instruction).equals(oldOutLists.get(instruction))) {
                    changed = true;
                    break;
                }
            }

        } while (changed);
    }

    private void buildUseLists() {
        for (Instruction instruction : method.getInstructions()) {
            List<Operand> use = processUse(instruction);
            useLists.put(instruction, use);
        }
    }

    private List<Operand> processUse(Instruction instruction) {
        List<Operand> use = new ArrayList<>();
        use = switch (instruction.getInstType()) {
            case ASSIGN -> {
                AssignInstruction assign = (AssignInstruction) instruction;
                yield processAssign(assign);
            }
            case BINARYOPER -> {
                BinaryOpInstruction binary = (BinaryOpInstruction) instruction;
                yield processBinaryOper(binary);
            }
            case UNARYOPER -> {
                UnaryOpInstruction unary = (UnaryOpInstruction) instruction;
                yield processUnaryOper(unary);
            }
            case CALL -> {
                CallInstruction call = (CallInstruction) instruction;
                yield processCall(call);
            }
            case RETURN -> {
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                yield processReturn(returnInstruction);
            }
            case PUTFIELD -> {
                PutFieldInstruction put = (PutFieldInstruction) instruction;
                yield processPutField(put);
            }
            case GETFIELD -> {
                GetFieldInstruction get = (GetFieldInstruction) instruction;
                yield processGetField(get);
            }
            case BRANCH -> {
                CondBranchInstruction branch = (CondBranchInstruction) instruction;
                yield processCondBranch(branch);
            }
            case NOPER -> {
                SingleOpInstruction single = (SingleOpInstruction) instruction;
                yield processNoper(single);
            }
            default -> use;
        };
        return use;
    }

    private List<Operand> processAssign(AssignInstruction assign) {
        return processUse(assign.getRhs());
    }

    private List<Operand> processBinaryOper(BinaryOpInstruction binary) {
        List<Operand> operands = new ArrayList<>();
        if (binary.getOperands() != null) {
            for (Element operand : binary.getOperands()) {
                if (operand instanceof Operand) operands.add((Operand) operand);
            }
        }
        return operands;
    }

    private List<Operand> processUnaryOper(UnaryOpInstruction unary) {
        List<Operand> operands = new ArrayList<>();
        if (unary.getOperand() != null) {
            if (unary.getOperand() instanceof Operand) operands.add((Operand) unary.getOperand());
        }
        return operands;
    }

    private List<Operand> processCall(CallInstruction call) {
        List<Operand> operands = new ArrayList<>();
        if (call.getOperands() != null) {
            for (Element operand : call.getOperands()) {
                if (operand instanceof Operand) operands.add((Operand) operand);
            }
        }
        return operands;
    }

    private List<Operand> processReturn(ReturnInstruction returnInstruction) {
        List<Operand> operands = new ArrayList<>();

        if (returnInstruction.hasReturnValue()) {
            var operand = returnInstruction.getOperand();
            operand.ifPresent(element -> operands.add((Operand) element));
        }
        return operands;
    }

    private List<Operand> processPutField(PutFieldInstruction put) {
        List<Operand> operands = new ArrayList<>();
        if (put.getOperands() != null) {
            for (Element operand : put.getOperands()) {
                if (operand instanceof Operand) operands.add((Operand) operand);
            }
        }
        return operands;
    }

    private List<Operand> processGetField(GetFieldInstruction get) {
        List<Operand> operands = new ArrayList<>();
        if (get.getOperands() != null) {
            for (Element operand : get.getOperands()) {
                if (operand instanceof Operand) operands.add((Operand) operand);
            }
        }
        return operands;
    }

    private List<Operand> processCondBranch(CondBranchInstruction branch) {
        List<Operand> operands = new ArrayList<>();
        if (branch.getOperands() != null) {
            for (Element operand : branch.getOperands()) {
                if (operand instanceof Operand) operands.add((Operand) operand);
            }
        }
        return operands;
    }

    private List<Operand> processNoper(SingleOpInstruction single) {
        List<Operand> operands = new ArrayList<>();
        if (single.getSingleOperand() != null) {
            if (single.getSingleOperand() instanceof Operand) operands.add((Operand) single.getSingleOperand());
        }
        return operands;
    }


    private void buildDefLists() {
        for (Instruction instruction : method.getInstructions()) {
            List<Operand> operand = new ArrayList<>();
            if (instruction.getInstType() == InstructionType.ASSIGN) {
                AssignInstruction assign = (AssignInstruction) instruction;
                Element element = assign.getDest();
                if (element instanceof Operand op) {
                    operand.add(op);
                }
            }
            defLists.put(instruction, operand);
        }
    }

    private void initializeInOutSets() {
        for (Instruction instruction : method.getInstructions()) {
            inLists.put(instruction, new ArrayList<>());
            outLists.put(instruction, new ArrayList<>());
        }
    }

    public List<Operand> getInList(Instruction instruction) {
        return inLists.get(instruction);
    }
    public List<Operand> getOutList(Instruction instruction) {
        return outLists.get(instruction);
    }
    public List<Operand> getUseList(Instruction instruction) {
        return useLists.get(instruction);
    }
    public List<Operand> getDefList(Instruction instruction) {
        return defLists.get(instruction);
    }
    public Map<Node, List<Operand>> getUseLists() {
        return useLists;
    }
    public Map<Node, List<Operand>> getDefLists() {
        return defLists;
    }
    public Map<Node, List<Operand>> getInLists() {
        return inLists;
    }
    public Map<Node, List<Operand>> getOutLists() {
        return outLists;
    }
}
