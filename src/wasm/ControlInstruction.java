package wasm;

import static parse.ValueType.B32;
import static parse.ValueType.V00;

import parse.BlockEntry;
import parse.ControlOp;
import parse.FnType;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class ControlInstruction extends SimpleInstruction {
    
    private final BlockEntry block;
    private final ValueType blocktype;

    public ControlInstruction(OpCode opcode, FnType fntype, int level, BlockEntry block, ValueType blocktype) {
        super(opcode, fntype, level);
        this.block = block;
        this.blocktype = blocktype;
    }

    public BlockEntry getBlock() {
        return block;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String type = blocktype == V00?"":" (" + blocktype.toString() +")";
        String result = String.format("%s%s",simple,type);
        return result;
    }
    
    @Override
    public ValueType getBlockType() {
        return blocktype;
    }
    
    public static Instruction control(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        FnType fntype;
        ValueType blocktype = V00;
        BlockEntry block = null;
        int level = typestack.getLevel();
        switch (opcode) {
            case RETURN:
                blocktype = typestack.returnType();
                fntype = typestack.getUnwind();
                if (blocktype != V00 && !fntype.lastParm().isCompatible(blocktype)) {
                    throw new IllegalStateException();
                }
                break;
            case ELSE:
                --level;
                block = typestack.getCurrentBlock();
                blocktype = block.getVt();
                fntype = FnType.consume(blocktype);
                break;
            case END:
                --level;
                block = typestack.getCurrentBlock();
                blocktype = block.getVt();
                if (block.isFallThroughToEnd()) {
                    fntype = FnType.unary(blocktype);
                } else if (block.isBranchToEnd()) {
                    fntype = typestack.getUnwind();
                } else {
                    fntype = typestack.getUnwind();
                }
                break;
            case UNREACHABLE:
                fntype = typestack.getUnwind();
                break;
            case BLOCK: case LOOP:
                fntype = FnType.consume(V00);
                blocktype = ((ControlOp)op).getBlockType();
                break;
            case IF:
                fntype = FnType.consume(B32);
                blocktype = ((ControlOp)op).getBlockType();
                break;
            default:
                throw new AssertionError();
        }
        return new ControlInstruction(opcode,fntype,level,block,blocktype);
    }
    
    public static Instruction combine(Instruction compareinst, Instruction ifinst) {
        OpCode compareop = compareinst.getOpCode();
        assert compareop.getOpType() == OpType.COMPARE;
        OpCode ifop = ifinst.getOpCode();
        assert ifop == OpCode.IF;
        int code = (compareop.getCode() << 8) | ifop.getCode();
        OpCode op = OpCode.getInstance(code);
        FnType fntype = compareinst.getFnType().combine(ifinst.getFnType());
        return new ControlInstruction(op, fntype, ifinst.getLevel(), null, ifinst.getBlockType());
    }
    
}
