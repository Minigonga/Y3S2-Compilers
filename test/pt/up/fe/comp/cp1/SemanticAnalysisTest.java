package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest {

    @Test
    public void symbolTable() {

        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/SymbolTable.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varNotDeclared() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarNotDeclared.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void classNotImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ClassNotImported.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intPlusObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntPlusObject.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void boolTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/BoolTimesInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayPlusInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayPlusInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayAccessOnInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayAccessOnInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayIndexNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayIndexNotInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void assignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssignIntToBool.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentFail.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentPassExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentPassExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentPassImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentPassImports.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void intInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntInIfCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInWhileCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToUndeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToUndeclaredMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToMethodAssumedInExtends() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToMethodAssumedInExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodAssumedInImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToMethodAssumedInImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void incompatibleArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleArguments.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assumeArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssumeArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Varargs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInit() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInit.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }


    @Test
    public void _varargsWrongUse1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_VarargsWrongUse1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _varargsWrongUse2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_VarargsWrongUse2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _varargsWrongUse3() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_VarargsWrongUse3.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _methodDuplicated() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_MethodDuplicated.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _fieldDuplicated() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_FieldDuplicated.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _paramDuplicated() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ParamDuplicated.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _undeclaredVariable() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_UndeclaredVariable.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _incrementPass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_IncrementPass.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void _typeWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_TypeWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _fieldMain() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_FieldMain.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _paramsNumberMain() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ParamsNumber.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }
    @Test
    public void _paramsMain() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ParamMain.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _paramsMainName() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ParamMainName.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _arrayAtribAndOp() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ArrayAtribAndOp.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _negativeNumberBoolFail() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_NegativeNumberBoolFail.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _classMethodCallPass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ClassMethodCallPass.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _methodCallWithParamsPass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_MethodCallWithParamsPass.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _classNotImported1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ClassNotImported1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _classNotImported2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ClassNotImported2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _duplicatedImports() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_DuplicatedImports.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _duplicatedVariables() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_DuplicatedVariables.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _variableNameLengthPass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_VariableNameLengthPass.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _returnIntButBoolFail() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ReturnIntButBoolFail.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _ThisOnStaticMain() { //Supostamente está mal mas está a dar bem
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ThisOnStaticMain.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _ThisOnMethodParameter() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ThisOnMethodParameter.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _OwnObjectInstantiationInMain() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_OwnObjectInstantiationInMain.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _ImportedCMethodTypePass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ImportedCMethodTypePass.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _ArrayStmt() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ArrayStmt.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _ComplexArrayAccess() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ComplexArrayAccess.jmm"));
        TestUtils.noErrors(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _NumberNegation() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_NumberNegation.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void _VoidMethodNameFail() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_VoidMethodNameFail.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void _MethodDefinedClass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_MethodDefinedClass.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void _MethodCallClass() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_MethodCallClass.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void _ClassMethodCallParam() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ClassMethodCallParam.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void _NotImportedUtilization() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_NotImportedUtilization.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void _VoidVarName() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_VoidVarName.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void _ReturnMethodFail() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_ReturnMethodFail.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void _UseThisInClassMethodCall() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/_UseThisInClassMethodCall.jmm"));
        TestUtils.noErrors(result);
    }
}
