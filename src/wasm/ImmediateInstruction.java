package wasm;

import static parse.ValueType.I32;
import static wasm.OpCode.*;

import parse.FnType;
import parse.ImmediateOp;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class ImmediateInstruction extends SimpleInstruction {
    
    private final Number imm1;

    public ImmediateInstruction(OpCode opcode, FnType fntype, Number imm1, int level) {
        super(opcode,fntype,level);
        this.imm1 = imm1;
    }

    @Override
    public Number getImm1() {
        return imm1;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s(%s)",simple,imm1);
        return result;
    }
    
    public static Instruction constant(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.produce(vt);
        Number imm1 = ((ImmediateOp)op).getImm1();
        return new ImmediateInstruction(opcode,fntype,imm1,typestack.getLevel());
    }
    
    public static Instruction memfn(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        FnType fntype;
        switch(opcode) {
            case MEMORY_GROW:
                fntype = FnType.unary(I32);
                break;
            case MEMORY_SIZE:
                fntype = FnType.produce(I32);
                break;
            default:
                throw new AssertionError();
        }
        int zero = ((ImmediateOp)op).getImm1().intValue();
        if (zero != 0) {
            throw new IllegalArgumentException();
        }
        return new ImmediateInstruction(opcode,fntype,zero,typestack.getLevel());
    }
    
}
