package parse;

import static wasm.OpCode.MEMORY_COPY;

import wasm.OpCode;

public class ImmediateOp implements Op {
    
    private final OpCode opcode;
    private final Number imm1;

    public ImmediateOp(OpCode opcode, Number imm1) {
        this.opcode = opcode;
        this.imm1 = imm1;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    public Number getImm1() {
        return imm1;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",opcode.getWasmOp(),imm1);
    }
    
    public static ImmediateOp getConstant(OpCode opcode, Section section) {
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        Number imm1 = section.getImm(vt);
        return new ImmediateOp(opcode, imm1);
    }
    
    public static ImmediateOp getMemfn(OpCode opcode, Section section) {
        long imm = section.getUByte();
        if (opcode == MEMORY_COPY) {
            int zero2 = section.getUByte();
            imm = (imm << 32) | Integer.toUnsignedLong(zero2);
        }
        if (imm != 0) {
            String msg = String.format("zero flag expected%nopcode = %s flag(s) = %x",opcode,imm);
            throw new IllegalArgumentException(msg);
        }
        return new ImmediateOp(opcode,imm);
    }
    
}
