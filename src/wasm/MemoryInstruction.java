package wasm;

import static parse.ValueType.I32;
import static parse.ValueType.V00;

import parse.FnType;
import parse.MemoryOp;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class MemoryInstruction extends SimpleInstruction {
    
    private final ValueType memtype;
    private final int offset;
    private final int alignment;

    public MemoryInstruction(OpCode opcode, FnType fntype, ValueType memtype, int offset, int alignment, int level) {
        super(opcode, fntype,level);
        this.memtype = memtype;
        this.offset = offset;
        int membytes = memtype.bitlength()/8;
        if ((1 << alignment) > membytes) {
            String msg = String.format("alignment hint = %d membytes = %d",alignment,membytes);
            throw new IllegalArgumentException(msg);
        }
        this.alignment = alignment;
    }

    public int getOffset() {
        return offset;
    }

    public int getAlignment() {
        return alignment;
    }

    public ValueType getMemType() {
        return memtype;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s(%s,+%s)",simple,alignment,offset);
        return result;
    }

   public static Instruction memload(Op op, TypeStack typestack) {
       OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.transform(vt, I32);
        int alignment = ((MemoryOp)op).getAlignment();
        int offset = ((MemoryOp)op).getOffset();
        ValueType memtype = opcode.getMemType();
        if (memtype == null) {
            throw new AssertionError();
        }
        return new MemoryInstruction(opcode,fntype,memtype,offset,alignment,typestack.getLevel());
    }

    public static Instruction memstore(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = new FnType(V00,I32,vt);
        int alignment = ((MemoryOp)op).getAlignment();
        int offset = ((MemoryOp)op).getOffset();
        ValueType memtype = opcode.getMemType();
        if (memtype == null) {
            throw new AssertionError();
        }
        return new MemoryInstruction(opcode,fntype,memtype,offset,alignment,typestack.getLevel());
    }
    
}
