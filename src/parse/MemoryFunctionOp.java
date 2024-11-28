package parse;

import static parse.Reason.M109;
import static wasm.OpCode.MEMORY_COPY;

import wasm.OpCode;

public class MemoryFunctionOp implements Op {
    
    private final OpCode opcode;
    private final int memidx1;
    private final int memidx2;

    public MemoryFunctionOp(OpCode opcode, int memidx1, int memidx2) {
        this.opcode = opcode;
        this.memidx1 = memidx1;
        this.memidx2 = memidx2;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    public int memidx1() {
        return memidx1;
    }

    public int memidx2() {
        return memidx2;
    }

    @Override
    public String toString() {
        String idx2str = opcode == MEMORY_COPY? ", " + memidx2: "";
        return String.format("%s(%d%s)",opcode.getWasmOp(),memidx1,idx2str);
    }
    
    public static MemoryFunctionOp getInstance(OpCode opcode, Section section) {
        int memidx1 = section.getUByte();
        int memidx2 = 0;
        if (opcode == MEMORY_COPY) {
            memidx2 = section.getUByte();
        }
        if (memidx1 != 0) {
            // "zero flag expected"
            throw new ParseException(M109,"opcode = %s flag(s) = %x",opcode,memidx1);
        }
        if (memidx2 != 0) {
            // "zero flag expected"
            throw new ParseException(M109,"opcode = %s flag(s) = %x",opcode,memidx2);
        }
        return new MemoryFunctionOp(opcode,memidx1,memidx2);
    }
    
}
