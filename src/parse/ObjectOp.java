package parse;

import static parse.Reason.M109;
import static wasm.OpCode.*;

import wasm.OpCode;

public class ObjectOp implements Op {

    private final OpCode opcode;
    private final int index;
    private final int index2;

    public ObjectOp(OpCode opcode, int index, int index2) {
        this.opcode = opcode;
        this.index = index;
        this.index2 = index2;
    }

    @Override
    public OpCode getOpCode() {
        return opcode;
    }

    public int getIndex() {
        return index;
    }

    public int getIndex2() {
        return index2;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",opcode.getWasmOp(),index);
    }
    
    public static ObjectOp getInstance(OpCode opcode,Section section) {
        int index2 = 0;
        int index;
        switch(opcode) {
            case GLOBAL_GET:
            case GLOBAL_SET:
                index = section.globalidx();
                break;
            case LOCAL_GET:
            case LOCAL_SET:
            case LOCAL_TEE:
                index = section.localidx();
                break;
            case CALL:
                index = section.funcidx();
                break;
            case CALL_INDIRECT:
                index = section.typeidx();
                index2 = section.tableidx();
                if (index2 != 0) { // future table number?
                    // "zero flag expected"
                    throw new ParseException(M109,"opcode = %s flag = %d",opcode,index2);
                }
                break;
            default:
                throw new AssertionError();
        }
        return new ObjectOp(opcode,index,index2);
    }

}
