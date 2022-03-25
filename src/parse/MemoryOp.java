package parse;

import wasm.OpCode;

public class MemoryOp implements Op {
    
    private final OpCode opcode;
    private final int offset;
    private final int alignment;

    public MemoryOp(OpCode opcode, int offset, int alignment) {
        this.opcode = opcode;
        this.offset = offset;
        this.alignment = alignment;
    }

    public int getOffset() {
        return offset;
    }

    public int getAlignment() {
        return alignment;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format("%s(%s,+%s)",opcode.getWasmOp(),alignment,offset);
    }

    public static MemoryOp getInstance(OpCode opcode, Section section) {
        int alignment = section.getU32();   // memarg(a o) not memory(o a) !
        int offset = (int)section.getU32L(); // storage class treats it as unsigned
        return new MemoryOp(opcode, offset, alignment);
    }
    
}
