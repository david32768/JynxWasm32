package parse;

import wasm.OpCode;

public class ControlOp implements  Op {
    
    private final OpCode opcode;
    private final ValueType blocktype;

    public ControlOp(OpCode opcode, ValueType blocktype) {
        this.opcode = opcode;
        this.blocktype = blocktype;
    }

    public ValueType getBlockType() {
        return blocktype;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",opcode.getWasmOp(),blocktype);
    }
    
    public static ControlOp getInstance(OpCode opcode, Section section) {
        ValueType blocktype = ValueType.X32;
        switch (opcode) {
            case RETURN:
            case ELSE:
            case END:
            case UNREACHABLE:
                break;
            case BLOCK: case LOOP:
                blocktype = section.getBlockType();
                break;
            case IF:
                blocktype = section.getBlockType();
                break;
            default:
                throw new AssertionError();
        }
        return new ControlOp(opcode,blocktype);
    }
}
