package parse;

import wasm.OpCode;

public class BrTableOp implements  Op {

    private final OpCode opcode;
    private final int[] targets;

    public BrTableOp(OpCode opcode, int[] targets) {
        this.opcode = opcode;
        this.targets = targets;
    }


    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    public int[] getTargets() {
        return targets;
    }

    @Override
    public String toString() {
        return String.format("%s %d",opcode.getWasmOp(),targets.length);
    }
    
    public static BrTableOp getInstance(OpCode opcode, Section section) {
        int count = section.vecsz();
        int[] targets = new int[count+1];
        for (int i = 0; i < count + 1; i++) {
            int br2label = section.labelidx();
            targets[i] = br2label;
        }
        return new BrTableOp(opcode,targets);
    }
}
