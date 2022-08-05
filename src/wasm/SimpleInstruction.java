package wasm;

import static parse.ValueType.B32;
import static parse.ValueType.V00;
import static wasm.OpCode.*;

import parse.FnType;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;

public class SimpleInstruction implements Instruction {

    private final OpCode opcode;
    private final FnType fntype;
    private final int level;
    
    public SimpleInstruction(OpCode opcode, FnType fntype, int level) {
        this.opcode = opcode;
        this.fntype = fntype;
        this.level = level;
    }

    @Override
    public FnType getFnType() {
        return fntype;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    @Override
    public String toString() {
        String result = String.format("%s",opcode.getWasmOp());
        return result;
    }

    public static Instruction binary(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.binary(vt);
        return new SimpleInstruction(opcode,fntype,typestack.getLevel());
    }
    
    public static Instruction unary(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.unary(vt);
        return new SimpleInstruction(opcode,fntype,typestack.getLevel());
    }
    
    public static Instruction transform(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getPrefix();
        ValueType vt_last = opcode.getSuffix();
        if (vt == null || vt_last == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.transform(vt, vt_last);
        return new SimpleInstruction(opcode,fntype,typestack.getLevel());
    }

    public static Instruction compare(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        ValueType vt = opcode.getSignedPrefix();
        if (vt == null) {
            throw new AssertionError();
        }
        FnType fntype = FnType.compare(vt);
        if (opcode == OpCode.I32_EQZ || opcode == OpCode.I64_EQZ) {
            fntype = FnType.transform(B32, vt);
        }
        return new SimpleInstruction(opcode,fntype,typestack.getLevel());
    }
    
    public static Instruction parametric(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        FnType fntype = null;
        ValueType vt;
        switch(opcode) {
            case DROP:
                vt = typestack.peek();
                fntype = FnType.consume(vt);
                break;
            case SELECT:
                vt = typestack.peekNext();
                fntype = new FnType(vt,vt,vt,B32);
                break;
            case NOP:
                fntype = FnType.produce(V00);
                break;
            default:
                throw new AssertionError();
        }
        return new SimpleInstruction(opcode, fntype,typestack.getLevel());
    }
    
    public static Instruction combine(Instruction compareinst, Instruction select) {
        OpCode compareop = compareinst.getOpCode();
        assert compareop.getOpType() == OpType.COMPARE;
        OpCode selectop = select.getOpCode();
        assert selectop == OpCode.SELECT;
        int code = (compareop.getCode() << 8) | selectop.getCode();
        OpCode opcode = OpCode.getInstance(code);
        FnType fntype = compareinst.getFnType().combine(select.getFnType());
        return new SimpleInstruction(opcode, fntype, select.getLevel());
    }
    
}

