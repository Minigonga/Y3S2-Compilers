package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;

    private List<String> imports;

    public JasminUtils(OllirResult ollirResult) {
        // Can be useful to have if you expand this class with more methods
        this.ollirResult = ollirResult;
    }


    public String getModifier(AccessModifier accessModifier) {
        return accessModifier != AccessModifier.DEFAULT ?
                accessModifier.name().toLowerCase() + " " :
                "";
    }

    public String getType(Type type) {
        if (type instanceof ClassType classType) {
            return getFullSuperClass(classType.getName());
        }

        if (type instanceof ArrayType arrayType) {
            var elementType = arrayType.getElementType();
            if (elementType instanceof ClassType ct && ct.getName().equals("String[]")) {
                return "[Ljava/lang/String;";
            }
        }
        //System.out.println("Type: " + type.toString());
        return switch (type.toString()) {
            case "INT32[]" -> "[I";
            case "INT", "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "VOID" -> "V";
            case "STRING[]" -> "[Ljava/lang/String;";
            default -> "";
        };
    }

    public boolean isComparisonOperation(OperationType opType) {
        return opType == OperationType.LTH ||
                opType == OperationType.LTE ||
                opType == OperationType.GTH ||
                opType == OperationType.GTE ||
                opType == OperationType.EQ ||
                opType == OperationType.NEQ;
    }

    public String getFullSuperClass(String superClass) {
        if (superClass == null) {
            return "java/lang/Object";
        }
        for (String imp : imports) {
            if (imp.endsWith("." + superClass)) {
                return imp.replace('.', '/');  // Convert to JVM path format
            }
        }
        return superClass;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
}
