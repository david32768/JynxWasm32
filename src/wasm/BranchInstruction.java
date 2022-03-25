package wasm;

import static parse.ValueType.B32;
import static parse.ValueType.V00;

import parse.BranchOp;
import parse.BranchTarget;
import parse.FnType;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class BranchInstruction extends SimpleInstruction {
    
    private final BranchTarget target;

    public BranchInstruction(OpCode opcode, FnType fntype,int level, BranchTarget target) {
        super(opcode, fntype,level);
        this.target = target;
    }

    public BranchTarget getTarget() {
        return target;
    }

    @Override
    public ValueType getBlockType() {
        return target.getUnwind().getRtype();
    }
    
    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s(%d)",simple,target.getBr2level());
        return result;
    }
    
    public static Instruction branch(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        int br2level = ((BranchOp)op).getBr2level();
        FnType fntype;
        BranchTarget target;
        switch (opcode) {
            case BR:
                target = typestack.getTarget(br2level, true);
                fntype = typestack.getUnwind();
                break;
            case BR_IF:
                target = typestack.getTarget(br2level, false);
                ValueType vtr = target.getUnwind().getRtype();
                if (vtr == V00) {
                    fntype = FnType.consume(B32);
                } else {
                    fntype = new FnType(vtr,vtr,B32);
                }
                break;
            default:
                throw new AssertionError();
        }
        return new BranchInstruction(opcode, fntype, typestack.getLevel(),target);
    }

    public static Instruction combine(Instruction compareinst, Instruction brinst) {
        OpCode compareop = compareinst.getOpCode();
        assert compareop.getOpType() == OpType.COMPARE;
        OpCode ifop = brinst.getOpCode();
        assert ifop == OpCode.BR_IF;
        int code = (compareop.getCode() << 8) | ifop.getCode();
        OpCode op = OpCode.getInstance(code);
        FnType fntype = compareinst.getFnType().changeRType(V00);
        return new BranchInstruction(op, fntype, brinst.getLevel(),((BranchInstruction)brinst).target);
    }
    
}
