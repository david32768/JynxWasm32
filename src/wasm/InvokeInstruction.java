package wasm;

import static wasm.OpCode.*;

import parse.FnType;
import parse.ObjectOp;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;
import parse.WasmModule;

public class InvokeInstruction extends SimpleInstruction {

    private final Number imm;
    private final Object obj;

    public InvokeInstruction(OpCode opcode, FnType fntype, Number imm, Object obj) {
        super(opcode,fntype);
        this.imm = imm;
        this.obj = obj;
    }

    public Object getObject() {
        return obj;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s %s",simple,imm);
        return result;
    }
    
    public static Instruction invoke(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        int index = ((ObjectOp)op).getIndex();
        FnType fntype;
        Object obj;
        WasmModule module = typestack.getModule();
        switch(opcode) {
            case CALL:
                fntype = module.atfuncidx(index).getFnType();
                obj = module.atfuncidx(index);
                break;
            case CALL_INDIRECT:
                fntype = module.attypeidx(index).append(ValueType.I32);
                int index2 = ((ObjectOp)op).getIndex2();
                obj = module.attableidx(index2);
                break;
            default:
                throw new AssertionError();
        }
        return new InvokeInstruction(opcode,fntype,index,obj);
    }

}
