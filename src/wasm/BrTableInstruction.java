package wasm;

import parse.BrTableOp;
import parse.BranchTarget;
import parse.FnType;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class BrTableInstruction extends SimpleInstruction {

    private final BranchTarget[] targets;


    public BrTableInstruction(OpCode opcode, FnType fntype, int level, BranchTarget[] targets) {
        super(opcode, fntype,level);
        this.targets = targets;
    }

    public BranchTarget[] getTargets() {
        return targets.clone();
    }

    @Override
    public ValueType getBlockType() {
        if(targets.length == 0) {
            return ValueType.V00;
        }
        ValueType result = targets[0].getUnwind().getRtype();
        for (int i = 1; i < targets.length; ++i) {
            ValueType vti = targets[i].getUnwind().getRtype();
            if (!vti.isCompatible(result)) {
                String msg = String.format("target at pos %d is %s which differs from first %s",i,vti,result);
                throw new IllegalStateException(msg);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s %d",simple,targets.length);
        return result;
    }
    
    public static Instruction brtable(Op op, TypeStack typestack) {
        FnType fntype = typestack.getUnwind();
        int[] br2levels = ((BrTableOp)op).getTargets();
        int count = br2levels.length;
        BranchTarget[] targets = new BranchTarget[br2levels.length];
        for (int i = 0; i < count; i++) {
            int br2label = br2levels[i];
            targets[i] = typestack.getTarget(br2label, false);
        }
        return new BrTableInstruction(op.getOpCode(),fntype,typestack.getLevel(),targets);
    }
}
