package parse;

import wasm.OpCode;

public class BlockEntry {

    private final OpCode opcode;
    private final int stackptr;
    private final ValueType vt;
    
    private boolean branchToEnd;
    private boolean fallThroughToEnd;
    
    public BlockEntry(OpCode opcode,int stackptr, ValueType vt) {
        this.opcode = opcode;
        this.stackptr = stackptr;
        this.vt = vt;
        this.branchToEnd = false;
        this.fallThroughToEnd = false;
    }

    public BlockEntry(OpCode opcode,BlockEntry block, ValueType vt) {
        assert opcode == OpCode.ELSE;
        this.opcode = opcode;
        this.stackptr = block.getStackptr();
        this.vt = vt;
        this.branchToEnd = block.isBranchToEnd();
        this.fallThroughToEnd = false;
    }

    public boolean isEndReachable() {
        return branchToEnd || fallThroughToEnd;
    }

    public boolean isOnlyBranchToEnd() {
        return branchToEnd && !fallThroughToEnd;
    }

    public boolean isOnlyFallThroughToEnd() {
        return !branchToEnd && fallThroughToEnd;
    }

    public boolean isBranchToEnd() {
        return branchToEnd;
    }

    public boolean isFallThroughToEnd() {
        return fallThroughToEnd;
    }

    public void setBranchToEnd() {
        this.branchToEnd = opcode != OpCode.LOOP;
    }

    public void setFallThroughToEnd() {
        this.fallThroughToEnd = true;
    }

    public int getStackptr() {
        return stackptr;
    }

    public ValueType getVt() {
        return vt;
    }

    public OpCode getOpCode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format("op = %s vt = %s stackptr = %d stack size = %d",
                opcode.name(),vt,stackptr,stackptr);
    }

}
