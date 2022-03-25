package parse;

import wasm.OpCode;

public class SimpleOp implements Op {

    private final OpCode opcode;
    
    private SimpleOp(OpCode opcode) {
        this.opcode = opcode;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    public static SimpleOp getInstance(OpCode opcode, Section section) {
        return new SimpleOp(opcode);
    }
    
    @Override
    public String toString() {
        String result = String.format("%s",opcode.getWasmOp());
        return result;
    }

}

