package wasm;

import parse.FnType;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class UnreachableInstruction extends SimpleInstruction {
    
    private final Op op;

    public UnreachableInstruction(Op op, int level) {
        super(OpCode.UNREACHABLE,FnType.consume(ValueType.V00),level);
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
    
    public static Instruction unreachable(Op op, TypeStack typestack) {
        return new UnreachableInstruction(op, typestack.getLevel());
    }
    
}
