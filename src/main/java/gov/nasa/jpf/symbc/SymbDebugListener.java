package gov.nasa.jpf.symbc;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.vm.*;

/**
 * Created by fresheed on 11.07.19.
 */
public class SymbDebugListener extends ListenerAdapter {

    @Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction){
        //System.out.println("Debug listener activated");
    }

    @Override
    public void choiceGeneratorSet(VM vm, ChoiceGenerator<?> newCG) {
        //System.out.println("Set CG: "+newCG.getClass().getCanonicalName());
    }
}
