package parse;

import wasm.OpCode;

public class ConstantOp implements Op {
    
    private final OpCode opcode;
    private final Number imm1;

    public ConstantOp(OpCode opcode, Number imm1) {
        this.opcode = opcode;
        this.imm1 = imm1;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    public Number getConstant() {
        return imm1;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",opcode.getWasmOp(),imm1);
    }
    
    public static ConstantOp getInstance(OpCode opcode, Section section) {
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        Number imm1 = section.getImm(vt);
        return new ConstantOp(opcode, imm1);
    }
    
}
