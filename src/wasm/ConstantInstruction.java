package wasm;

import parse.ConstantOp;
import parse.FnType;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class ConstantInstruction extends SimpleInstruction {
    
    private final Number imm1;

    public ConstantInstruction(OpCode opcode, FnType fntype, Number imm1) {
        super(opcode,fntype);
        this.imm1 = imm1;
    }
    
    public Number getConstant() {
        return imm1;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s %s",simple,imm1);
        return result;
    }
    
    public static Instruction constant(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.produce(vt);
        Number imm1 = ((ConstantOp)op).getConstant();
        return new ConstantInstruction(opcode,fntype,imm1);
    }
    
}
