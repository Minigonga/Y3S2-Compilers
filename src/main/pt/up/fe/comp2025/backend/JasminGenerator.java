package pt.up.fe.comp2025.backend;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.ClassType;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.lang.annotation.ElementType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    private int labelCounter;

    private int currentStackLimit;
    private int stackLimit;
    private int localsLimit;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final JasminUtils types;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        types = new JasminUtils(ollirResult);

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(NewInstruction.class, this::generateNewInstruction);
        generators.put(InvokeStaticInstruction.class, this::generateStaticInstruction);
        generators.put(InvokeSpecialInstruction.class, this::generateSpecialInstruction);
        generators.put(InvokeVirtualInstruction.class, this::generateVirtualInstruction);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCondInstruction);
        generators.put(GotoInstruction.class, this::generateGoToInstruction);
        generators.put(OpCondInstruction.class, this::generateOpCondInstruction);
        generators.put(UnaryOpInstruction.class, this::generateUnaryInstruction);
        generators.put(ArrayLengthInstruction.class, this::generateArrayLengthInstruction);
    }

    private String generateArrayLengthInstruction(ArrayLengthInstruction arrayLengthInstruction) {
        StringBuilder code = new StringBuilder();
        code.append(apply(arrayLengthInstruction.getOperands().getFirst()));
        code.append("arraylength" + NL);
        adjustStack(1);
        return code.toString();
    }

    private String generateUnaryInstruction(UnaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();

        // Load operand
        code.append(apply(instruction.getOperand()));

        OperationType opType = instruction.getOperation().getOpType();

        adjustStack(1);
        adjustStack(-1);

        adjustLocals(1);

        switch (opType) {
            case NOTB -> {
                code.append("iconst_1").append("\n");
                code.append("ixor").append("\n");
            }
            default -> {
                throw new NotImplementedException(opType);
            }
        }

        return code.toString();
    }


    private String generateOpCondInstruction(OpCondInstruction opCondInstruction) {
        StringBuilder code = new StringBuilder();
        BinaryOpInstruction cond = (BinaryOpInstruction) opCondInstruction.getCondition();

        // Load operands
        code.append(apply(cond.getLeftOperand()));
        code.append(apply(cond.getRightOperand()));

        // Choose the correct comparison
        String op;
        switch (cond.getOperation().getOpType()) {
            case LTH -> op = "if_icmplt";
            case LTE -> op = "if_icmple";
            case GTH -> op = "if_icmpgt";
            case GTE -> op = "if_icmpge";
            case EQ  -> op = "if_icmpeq";
            case NEQ -> op = "if_icmpne";
            default -> throw new NotImplementedException(cond.getOperation().getOpType());
        }

        adjustStack(-1);
        code.append(op).append(" ").append(opCondInstruction.getLabel()).append(NL);
        return code.toString();
    }

    private String generateGoToInstruction(GotoInstruction gotoInstruction) {
        return "goto " + gotoInstruction.getLabel() + NL;
    }

    private String generateSingleOpCondInstruction(SingleOpCondInstruction singleOpCondInstruction) {
        StringBuilder code = new StringBuilder();
        Instruction condition = singleOpCondInstruction.getCondition();

        code.append(apply(condition.toInstruction())).append("ifne").append(" ").append(singleOpCondInstruction.getLabel()).append(NL);
        adjustStack(-1);

        return code.toString();

    }

    private String generateGetFieldInstruction(GetFieldInstruction getFieldInstruction) {
        StringBuilder code = new StringBuilder();
        Element first = getFieldInstruction.getOperands().getFirst();
        Element second = getFieldInstruction.getOperands().get(1);

        code.append("\t").append(this.apply(first));
        code.append("\tgetfield ").append(types.getType(first.getType())).append("/").append(((Operand) second).getName());
        String type;
        if (second.getType() instanceof ClassType classType) {
            type = "L" + types.getType(second.getType())+";";

        } else {
            type = types.getType(second.getType());
        }
        code.append(" ").append(type).append(NL);
        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction putFieldInstruction) {
        StringBuilder code = new StringBuilder();

        adjustStack(1);

        Element first = putFieldInstruction.getOperands().getFirst();

        Element second = putFieldInstruction.getOperands().get(1);

        Element last = putFieldInstruction.getOperands().getLast();

        code.append("\t").append(this.apply(first));

        code.append(this.apply(last));

        code.append("\tputfield ").append(types.getType(first.getType())).append("/").append(((Operand) second).getName());

        String type;
        if (second.getType() instanceof ClassType classType) {
            type = "L" + types.getType(second.getType())+";";

        } else {
            type = types.getType(second.getType());
        }
        code.append(" ").append(type).append(NL);

        //System.out.println(code.toString());

        adjustStack(-2);

        return code.toString();
    }


    private String generateVirtualInstruction(InvokeVirtualInstruction invokeVirtualInstruction) {
        StringBuilder code = new StringBuilder();

        code.append(apply(invokeVirtualInstruction.getOperands().getFirst()));
        LiteralElement method = (LiteralElement) invokeVirtualInstruction.getOperands().get(1);
        List<Element> args = invokeVirtualInstruction.getOperands().subList(2, invokeVirtualInstruction.getOperands().size());

        for (var arg : args) {
            code.append(apply(arg));
        }

        code.append("invokevirtual ").append(types.getType(invokeVirtualInstruction.getOperands().getFirst().getType()) + "/" + method.getLiteral() + "(");
        for (Element element : args){
            String type = types.getType(element.getType());
            if (element.getType() instanceof ClassType classType) {
                code.append("L").append(type).append(";");
            } else {
                code.append(type);
            }
        }

        currentStackLimit = 0;

        var retType = types.getType(invokeVirtualInstruction.getReturnType());

        if (!retType.equals("V")){
            adjustStack(1);
        }

        code.append(")").append(retType).append(NL);
        return code.toString();
    }

    private String getImportClassName(String name){
        //TODO Esta maneira nao parece ser a mais correta
        if (name.contains(".")) return name.split(": ")[1].split("\\.")[0];
        else return name;
    }

    private String generateStaticInstruction(InvokeStaticInstruction invokeStaticInstruction) {
        StringBuilder code = new StringBuilder();

        String importedClass = this.getImportClassName(invokeStaticInstruction.getOperands().getFirst().toString());

        LiteralElement second = (LiteralElement) invokeStaticInstruction.getOperands().get(1);



        for (Element element : invokeStaticInstruction.getOperands()){;
            if (Objects.equals(types.getType(element.getType()), "I") || Objects.equals(types.getType(element.getType()), "Z") || Objects.equals(types.getType(element.getType()), "V") || Objects.equals(types.getType(element.getType()), "[I")) code.append(this.apply(element));
        }

        code.append("invokestatic ").append(importedClass);

        code.append("/").append(second.getLiteral())
                .append("(");

        for (Element element : invokeStaticInstruction.getOperands()){
            if (Objects.equals(types.getType(element.getType()), "I") || Objects.equals(types.getType(element.getType()), "Z") || Objects.equals(types.getType(element.getType()), "V") || Objects.equals(types.getType(element.getType()), "[I")){
                code.append(types.getType(element.getType()));
            }
        }

        code.append(")");

        currentStackLimit = 0;

        var retType = types.getType(invokeStaticInstruction.getReturnType());

        if (!retType.equals("V")){
            adjustStack(1);
        }

        code.append(retType).append(NL);

        return code.toString();
    }

    private String generateSpecialInstruction(InvokeSpecialInstruction invokeSpecialInstruction) {
        StringBuilder code = new StringBuilder();

        code.append(this.apply(invokeSpecialInstruction.getCaller()))
                .append("invokespecial ");
        for (Element element : invokeSpecialInstruction.getOperands()){
            code.append(types.getType(element.getType()));
        }
        code.append("/<init>")
                .append("(")
                .append(")");


        var retType = types.getType(invokeSpecialInstruction.getReturnType());

        currentStackLimit = 0;

        if (!retType.equals("V")){
            adjustStack(1);
        }
        code.append(retType).append(NL);

        return code.toString();
    }

    private String generateNewInstruction(NewInstruction newInstruction) {
        StringBuilder code = new StringBuilder();
        var instructionType = newInstruction.getCaller().getType();
        if (!types.getType(instructionType).equals("[I")) {
            code.append("\nnew ").append(types.getType(instructionType))
                    .append(NL);
            adjustStack(1);
        }
        else{
            code.append(apply(newInstruction.getOperands().get(1)));
            code.append("newarray int" + NL);
            adjustStack(1);
            adjustStack(-1);
        }

        return code.toString();
    }

    private String apply(TreeNode node) {
        var code = new StringBuilder();

        // Print the corresponding OLLIR code as a comment
        //code.append("; ").append(node).append(NL);

        code.append(generators.apply(node));

        return code.toString();
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        types.setImports(classUnit.getImports());

        var fullSuperClass = types.getFullSuperClass(classUnit.getSuperClass());

        code.append(".super ").append(fullSuperClass).append(NL);
        for (var fields : classUnit.getFields()) {
            if (fields.getFieldType() instanceof ClassType classType) {
                code.append(".field public  '"+ fields.getFieldName() + "' L" + types.getType(fields.getFieldType()) +";"+ NL);
            } else {
                code.append(".field public  '"+ fields.getFieldName() + "' " + types.getType(fields.getFieldType()) + NL);
            }
        }
        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial %s/<init>()V
                    return
                .end method
                """.formatted(fullSuperClass);
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        //System.out.println("STARTING METHOD " + method.getMethodName());
        // set method
        currentMethod = method;

        currentStackLimit = 0;
        stackLimit = 0;
        localsLimit = 0;
        adjustLocals(method.getParams().size());

        var code = new StringBuilder();

        // calculate modifier
        var modifier = types.getModifier(method.getMethodAccessModifier());

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        //antes as duas linhas de baixo tavam = "I" que ear para ints
        var params = method.getParams();
        var returnType = method.getReturnType() != null ? types.getType(method.getReturnType()) : "";

        if (methodName.equals("main")) {
            code.append("\n.method ").append(modifier).append("static ").append(methodName)
                    .append("(");
        }
        else code.append("\n.method ").append(modifier)
                .append(methodName)
                .append("(");

        for (var param: params) {
            if ((param.getType() instanceof ClassType classType)) {
                code.append("L").append(types.getFullSuperClass(classType.getName())).append(";");
            } else {
                if (param.getType().toString().equals("STRING")) {
                    code.append("Ljava/lang/String;");
                } else {
                    code.append(types.getType(param.getType()));
                }
            }
        }

        code.append(")").append(returnType).append(NL);

        StringBuilder instrCode = new StringBuilder();

        // Calculate instructions before to know stack limit
        for (var inst : method.getInstructions()) {
            for (var label : method.getLabels().entrySet()){
                if (label.getValue().equals(inst)) instrCode.append(label.getKey()).append(":").append(NL);
            }

            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            instrCode.append(instCode);
        }

        // Add limits
        code.append(TAB).append(".limit stack ").append(stackLimit).append(NL);
        code.append(TAB).append(".limit locals ").append(localsLimit).append(NL);

        code.append(instrCode);

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        //System.out.println("ENDING METHOD " + method.getMethodName());
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        // store value in the stack in destination
        var lhs = assign.getDest();
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }
        if (lhs instanceof ArrayOperand) {
            code.append(apply(lhs));
            code.append(apply(assign.getRhs()));
            int lastIndex = code.lastIndexOf("iaload");
            adjustStack(1);
            if (lastIndex != -1) {
                int endOfLine = code.indexOf("\n", lastIndex);
                if (endOfLine == -1) {
                    endOfLine = code.length();
                }
                code.delete(lastIndex, endOfLine + 1);
            }
            code.append("iastore");
            adjustStack(-3);
        }else {
            if (assign.getRhs().getInstType().toString().equals("BINARYOPER")) {
                BinaryOpInstruction binaryOp = (BinaryOpInstruction) assign.getRhs();
                OperationType opType = binaryOp.getOperation().getOpType();

                if (opType == OperationType.ADD || opType == OperationType.SUB) {
                    Element left = binaryOp.getLeftOperand();
                    Element right = binaryOp.getRightOperand();

                    LiteralElement literal = null;
                    Operand variable = null;

                    if (left instanceof LiteralElement && right instanceof Operand) {
                        literal = (LiteralElement) left;
                        variable = (Operand) right;
                    } else if (right instanceof LiteralElement && left instanceof Operand) {

                        literal = (LiteralElement) right;
                        variable = (Operand) left;
                    }

                    if (literal != null && variable != null && variable.getName().equals(((Operand) lhs).getName())) {
                        int value = Integer.parseInt(literal.getLiteral());
                        if (opType == OperationType.SUB) {
                            value = -value; // Convert subtraction to negative increment
                        }

                        // Check if value fits in iinc range (-128 to 127)
                        if (value >= -128 && value <= 127) {
                            int regIndex = currentMethod.getVarTable().get(variable.getName()).getVirtualReg();
                            return "iinc " + regIndex + " " + value + NL;
                        }
                    }
                }
            }
            code.append(apply(assign.getRhs()));
            var operand = (Operand) lhs;
            var reg = currentMethod.getVarTable().get(operand.getName());
            // get register
            var type = reg.getVarType();
            adjustLocals(reg.getVirtualReg());
            String middle = (reg.getVirtualReg() < 4) ? "_" : " ";
            if (type instanceof ArrayType || type instanceof ClassType) {
                code.append("astore").append(middle).append(reg.getVirtualReg()).append(NL);
                adjustStack(-1);
            } else {
                code.append("istore").append(middle).append(reg.getVirtualReg()).append(NL);
                adjustStack(-1);
            }
        }
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        adjustStack(1);
        adjustLocals(0);
        int value = Integer.parseInt(literal.getLiteral());
        if (value >= -1 && value <= 5) return "\ticonst_" + value + NL;
        if (value >= -128 && value <= 127) return "\tbipush " + value + NL;
        else if (value >= -32768 && value <= 32767) return "\tsipush " + value + NL;
        return "\tldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {

        var reg = currentMethod.getVarTable().get(operand.getName());
        int regNum = reg.getVirtualReg();
        adjustLocals(regNum);

        adjustStack(1);

        if (operand instanceof ArrayOperand){
            StringBuilder code = new StringBuilder();
            code.append((regNum < 4 ? "aload_" + regNum : "aload " + regNum) + NL);
            if (!((ArrayOperand) operand).getIndexOperands().isEmpty())  code.append(apply(((ArrayOperand) operand).getIndexOperands().get(0)));
            code.append("iaload" + NL);
            adjustStack(-1);
            return code.toString();
        }
        else if (reg.getVarType() instanceof ArrayType || reg.getVarType() instanceof ClassType) {
            return (regNum < 4 ? "aload_" + regNum : "aload " + regNum) + NL;
        } else {
            return (regNum < 4 ? "iload_" + regNum : "iload " + regNum) + NL;
        }
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        // load values on the left and on the right

        if (binaryOp.getLeftOperand().isLiteral()) {
            var left =  Integer.parseInt(((LiteralElement) binaryOp.getLeftOperand()).getLiteral());
            if (left != 0) {
                code.append(apply(binaryOp.getLeftOperand()));
            }
        } else {
            code.append(apply(binaryOp.getLeftOperand()));
        }
        if (binaryOp.getRightOperand().isLiteral()) {
            var right =  Integer.parseInt(((LiteralElement) binaryOp.getRightOperand()).getLiteral());
            if (right != 0) {
                code.append(apply(binaryOp.getRightOperand()));
            }
        } else {
            code.append(apply(binaryOp.getRightOperand()));
        }

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "add";
            case MUL -> "mul";
            case SUB -> "sub";
            case DIV -> "div";
            case LTH -> "if_icmplt";
            case LTE -> "if_icmple";
            case GTH -> "if_icmpgt";
            case GTE -> "if_icmpge";
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            case ANDB -> "ifne";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        Integer value = null;
        //Optimizations
        if (binaryOp.getOperation().getOpType() == OperationType.LTH){
            System.out.println("Operation type: " + binaryOp.getOperation().getOpType());
            if (binaryOp.getRightOperand() instanceof LiteralElement){
                op = "iflt";
                value = Integer.parseInt(((LiteralElement) binaryOp.getRightOperand()).getLiteral());

            }
            else if (binaryOp.getLeftOperand() instanceof LiteralElement){
                op = "ifge";
                value = Integer.parseInt(((LiteralElement) binaryOp.getLeftOperand()).getLiteral());
            }
            if (!(value != null && value == 0)){
                op = "if_icmplt";
            }
        }
        else if (binaryOp.getOperation().getOpType() == OperationType.GTE){
            if (binaryOp.getRightOperand() instanceof LiteralElement){
                op = "ifle";
                value = Integer.parseInt(((LiteralElement) binaryOp.getRightOperand()).getLiteral());
            }
            else if (binaryOp.getLeftOperand() instanceof LiteralElement){
                op = "ifgt";
                value = Integer.parseInt(((LiteralElement) binaryOp.getLeftOperand()).getLiteral());
            }
            if (!(value != null && value == 0)){
                op = "if_icmpge";
            }

        }

        if (types.isComparisonOperation(binaryOp.getOperation().getOpType())) {
            // Handle comparison operations (LTH, LTE, GTH, GTE, EQ, NEQ)
            String trueLabel = "j_true_" + labelCounter;
            String endLabel = "j_end" + labelCounter++;

            code.append(op).append(" ").append(trueLabel).append(NL);
            code.append("iconst_0").append(NL); // false case
            adjustStack(1);

            code.append("goto ").append(endLabel).append(NL);
            code.append(trueLabel).append(":").append(NL);
            code.append("iconst_1").append(NL); // true case
            adjustStack(1);

            code.append(endLabel).append(":").append(NL);
        } else {
            // Handle arithmetic operations (ADD, MUL, SUB, DIV)
            code.append("i").append(op).append(NL);
        }

        adjustStack(-1);
        adjustLocals(1);

        return code.toString();
    }

    private void adjustStack(int delta) {
        currentStackLimit += delta;
        stackLimit = Math.max(stackLimit, currentStackLimit);
    }

    private void adjustLocals(int reg){
        localsLimit = Math.max(localsLimit, reg + 1);
    }


    private String generateReturn(ReturnInstruction returnInst) {

        if (returnInst.hasReturnValue()) {
            return "\t" + this.apply(returnInst.getOperand().orElse(null)) + NL + "ireturn";
        }
        if (returnInst.getOperand().isPresent()) {
            if (Objects.equals(Objects.requireNonNull(returnInst.getOperand().orElse(null)).getType().toString(), "INT32")
                    || Objects.equals(Objects.requireNonNull(returnInst.getOperand().orElse(null)).getType().toString(), "BOOLEAN"))
                return "ireturn";
            else return "areturn";

        }
        return "return";
    }
}