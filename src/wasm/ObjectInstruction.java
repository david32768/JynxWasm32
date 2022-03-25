package wasm;

import static wasm.OpCode.*;

import parse.FnType;
import parse.Global;
import parse.Local;
import parse.ObjectOp;
import parse.Op;
import parse.TypeStack;
import parse.ValueType;
import parse.WasmModule;

public class ObjectInstruction extends SimpleInstruction {

    private final Number imm;
    private final Object obj;

    public ObjectInstruction(OpCode opcode, FnType fntype, Number imm, Object obj, int level) {
        super(opcode,fntype,level);
        this.imm = imm;
        this.obj = obj;
    }

    public Object getObject() {
        return obj;
    }

    public Number getImm() {
        return imm;
    }

    @Override
    public String toString() {
        String simple = super.toString();
        String result = String.format("%s(%s)",simple,imm);
        return result;
    }
    
    public static Instruction variable(Op op, TypeStack typestack) {
        OpCode opcode = op.getOpCode();
        int index = ((ObjectOp)op).getIndex();
        FnType fntype;
        Object obj;
        WasmModule module = typestack.getModule();
        switch(opcode) {
            case GLOBAL_GET:
                Global global = module.atglobidx(index); 
                fntype = FnType.produce(global.getType());
                obj = global;
                break;
            case GLOBAL_SET:
                global = module.atglobidx(index); 
                fntype = FnType.consume(global.getType());
                obj = global;
                break;
            case LOCAL_GET:
                Local local = typestack.getLocal(index);
                fntype = FnType.produce(local.getType());
                obj = local;
                break;
            case LOCAL_SET:
                local = typestack.getLocal(index);
                fntype = FnType.consume(local.getType());
                obj = local;
                break;
            case LOCAL_TEE:
                local = typestack.getLocal(index);
                fntype = FnType.unary(local.getType());
                obj = local;
                break;
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
        return new ObjectInstruction(opcode,fntype,index,obj,typestack.getLevel());
    }

}
