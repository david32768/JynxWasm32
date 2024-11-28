package wasm;

import static parse.ValueType.I32;
import static parse.ValueType.V00;
import static wasm.OpCode.*;

import parse.FnType;
import parse.MemoryFunctionOp;
import parse.Op;
import parse.TypeStack;

public class MemoryFunctionInstruction extends SimpleInstruction {
    
    private final int memidx1;
    private final int memidx2;

    public MemoryFunctionInstruction(OpCode opcode, FnType fntype, int memidx1, int memidx2) {
        super(opcode,fntype);
        this.memidx1 = memidx1;
        this.memidx2 = memidx2;
        assert memidx1 == 0;
        assert memidx2 == 0;
    }

    public int memidx1() {
        return memidx1;
    }

    public int memidx2() {
        assert getOpCode() == OpCode.MEMORY_COPY;
        return memidx2;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String idx2str = getOpCode() == MEMORY_COPY? "  " + memidx2: "";
        String result = String.format("%s %d%s",simple,memidx1,idx2str);
        return result;
    }
    
    public static Instruction getInstance(Op op, TypeStack typestack) {
        MemoryFunctionOp memop = (MemoryFunctionOp)op;
        OpCode opcode = memop.getOpCode();
        FnType fntype = switch(opcode) {
            case MEMORY_GROW -> FnType.unary(I32);
            case MEMORY_SIZE -> FnType.produce(I32);
            case MEMORY_COPY, MEMORY_FILL -> new FnType(V00,I32,I32,I32);
            default -> throw new AssertionError();
        };
        return new MemoryFunctionInstruction(opcode,fntype,memop.memidx1(), memop.memidx2());
    }
    
}
