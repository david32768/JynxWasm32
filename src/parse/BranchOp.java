package parse;

import wasm.OpCode;

public class BranchOp implements  Op {
    
    private final OpCode opcode;
    private final int br2level;

    public BranchOp(OpCode opcode, int br2level) {
        this.opcode = opcode;
        this.br2level = br2level;
    }

    public int getBr2level() {
        return br2level;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format("%s(%d)",opcode.getWasmOp(),br2level);
    }
    
    public static BranchOp getInstance(OpCode opcode, Section section) {
        int br2level = section.labelidx();
        return new BranchOp(opcode, br2level);
    }
}
