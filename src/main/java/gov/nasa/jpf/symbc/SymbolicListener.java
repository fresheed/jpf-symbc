/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.*;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.INVOKESTATIC;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.*;

import java.util.HashMap;
import java.util.Map;

//import gov.nasa.jpf.symbc.numeric.SymbolicInteger;

public class SymbolicListener extends PropertyListenerAdapter implements PublisherExtension {

    /*
     * Locals to preserve the value that was held by JPF prior to changing it in order to turn off state matching during
     * symbolic execution no longer necessary because we run spf stateless
     */

    private Map<String, SLPrintHelper.MethodSummary> allSummaries;
    private String currentMethodName = "";

    public SymbolicListener(Config conf, JPF jpf) {
        System.out.println("EDITED SYMBOLIC LISTENER 2");
        jpf.addPublisherExtension(ConsolePublisher.class, this);
        allSummaries = new HashMap<String, SLPrintHelper.MethodSummary>();
    }

    @Override
    public void propertyViolated(Search search) {
        System.out.println("prop viol");
        VM vm = search.getVM();

        ChoiceGenerator<?> cg = vm.getChoiceGenerator();
        if (!(cg instanceof PCChoiceGenerator)) {
            ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
            while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
                prev_cg = prev_cg.getPreviousChoiceGenerator();
            }
            cg = prev_cg;
        }
        if ((cg instanceof PCChoiceGenerator) && ((PCChoiceGenerator) cg).getCurrentPC() != null) {
            PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
            String error = search.getLastError().getDetails();
            error = "\"" + error.substring(0, error.indexOf("\n")) + "...\"";
            if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
                SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
                PCAnalyzer pa = new PCAnalyzer();
                pa.solve(pc, solver);
            } else
                pc.solve();

            Pair<String, String> pcPair = new Pair<String, String>(pc.toString(), error);// (pc.toString(),error);

            // String methodName = vm.getLastInstruction().getMethodInfo().getName();
            SLPrintHelper.MethodSummary methodSummary = allSummaries.get(currentMethodName);
            if (methodSummary == null)
                methodSummary = new SLPrintHelper.MethodSummary();
            methodSummary.addPathCondition(pcPair);
            allSummaries.put(currentMethodName, methodSummary);
            System.out.println("Property Violated: PC is " + pc.toString());
            System.out.println("Property Violated: result is  " + error);
            System.out.println("****************************");
        }
        // }
    }

    @Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction,
            Instruction executedInstruction) {
        if (!vm.getSystemState().isIgnored()) {
            Instruction insn = executedInstruction;
            // SystemState ss = vm.getSystemState();
            ThreadInfo ti = currentThread;
            Config conf = vm.getConfig();

            if (insn instanceof JVMInvokeInstruction) {
                processInvoke((JVMInvokeInstruction) insn, ti, conf);
            } else if (insn instanceof JVMReturnInstruction) {
                processReturn(vm, insn, ti, conf);
            }
        }
    }

    private void processReturn(VM vm, Instruction insn, ThreadInfo ti, Config conf) {
        MethodInfo mi = insn.getMethodInfo();
        ClassInfo ci = mi.getClassInfo();
        if (null != ci) {
            String className = ci.getName();
            String methodName = mi.getName();
            String longName = mi.getLongName();
            int numberOfArgs = mi.getNumberOfArguments();

            if (((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
                    || BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null))) {
                processReturnIfSymbolicClassOrMethod(vm, insn, ti);
            }
        }
    }

    private void processReturnIfSymbolicClassOrMethod(VM vm, Instruction insn, ThreadInfo ti) {
        ChoiceGenerator<?> cg = vm.getChoiceGenerator();
        if (!(cg instanceof PCChoiceGenerator)) {
            //System.out.println("Got a NON-symbolic branching:");
            ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
            while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
                prev_cg = prev_cg.getPreviousChoiceGenerator();
            }
            cg = prev_cg;
        }
        if ((cg instanceof PCChoiceGenerator) && ((PCChoiceGenerator) cg).getCurrentPC() != null) {
            System.out.println("Got a symbolic branching:");
            PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
            // pc.solve(); //we only solve the pc
            if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
                SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
                PCAnalyzer pa = new PCAnalyzer();
                pa.solve(pc, solver);
            } else
                pc.solve();

            if (!PathCondition.flagSolved) {
                return;
            }
            // after the following statement is executed, the pc loses its solution
            getReturnInfo(insn, ti);
        }
    }

    private void getReturnInfo(Instruction insn, ThreadInfo ti) {
        // seems that nothing happens here
    }

    private void processInvoke(JVMInvokeInstruction insn, ThreadInfo ti, Config conf) {
        JVMInvokeInstruction md = insn;
        String methodName = md.getInvokedMethodName();
        int numberOfArgs = md.getArgumentValues(ti).length;

        MethodInfo mi = md.getInvokedMethod();
        ClassInfo ci = mi.getClassInfo();
        String className = ci.getName();

        StackFrame sf = ti.getTopFrame();
        String shortName = methodName;
        String longName = mi.getLongName();
        if (methodName.contains("("))
            shortName = methodName.substring(0, methodName.indexOf("("));

        if (!mi.equals(sf.getMethodInfo()))
            return;

        if ((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
                || BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null)) {
            System.out.println(String.format("-- Executing %s symbolically with %s", mi.getName(), md.getMnemonic()));

            SLPrintHelper.MethodSummary methodSummary = new SLPrintHelper.MethodSummary();

            methodSummary.setMethodName(className + "." + shortName);
            Object[] argValues = md.getArgumentValues(ti);
            String argValuesStr = "";
            for (int i = 0; i < argValues.length; i++) {
                argValuesStr = argValuesStr + argValues[i];
                if ((i + 1) < argValues.length)
                    argValuesStr = argValuesStr + ",";
            }
            methodSummary.setArgValues(argValuesStr);
            byte[] argTypes = mi.getArgumentTypes();
            String argTypesStr = "";
            for (int i = 0; i < argTypes.length; i++) {
                argTypesStr = argTypesStr + argTypes[i];
                if ((i + 1) < argTypes.length)
                    argTypesStr = argTypesStr + ",";
            }
            methodSummary.setArgTypes(argTypesStr);

            // get the symbolic values (changed from constructing them here)
            String symValuesStr = "";
            String symVarNameStr = "";

            LocalVarInfo[] argsInfo = mi.getArgumentLocalVars();

            if (argsInfo == null)
                throw new RuntimeException("ERROR: you need to turn debug option on");

            int sfIndex = 1; // do not consider implicit param "this"
            int namesIndex = 1;
            if (md instanceof INVOKESTATIC) {
                sfIndex = 0; // no "this" for static
                namesIndex = 0;
            }

            for (int i = 0; i < numberOfArgs; i++) {
                Expression expLocal = (Expression) sf.getLocalAttr(sfIndex);
                if (expLocal != null) // symbolic
                    symVarNameStr = expLocal.toString();
                else
                    symVarNameStr = argsInfo[namesIndex].getName() + "_CONCRETE" + ",";
                // TODO: what happens if the argument is an array?
                symValuesStr = symValuesStr + symVarNameStr + ",";
                sfIndex++;
                namesIndex++;
                if (argTypes[i] == Types.T_LONG || argTypes[i] == Types.T_DOUBLE)
                    sfIndex++;

            }

            // get rid of last ","
            if (symValuesStr.endsWith(",")) {
                symValuesStr = symValuesStr.substring(0, symValuesStr.length() - 1);
            }
            methodSummary.setSymValues(symValuesStr);

            currentMethodName = longName;
            allSummaries.put(longName, methodSummary);
        }
    }

    @Override
    public void publishFinished(Publisher publisher) {
        new SLPrintHelper(allSummaries).publishFinished(publisher);
    }

}
