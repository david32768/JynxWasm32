package wasm;

import parse.FnType;
import parse.Op;
import parse.ValueType;

public class UnreachableInstruction extends SimpleInstruction {
    
    private final Op op;

    public UnreachableInstruction(Op op) {
        super(OpCode.UNREACHABLE,FnType.consume(ValueType.V00));
        this.op = op;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s is %s",op,simple);
        return result;
    }
    
    public static Instruction of(Op op) {
        return new UnreachableInstruction(op);
    }
    
}
