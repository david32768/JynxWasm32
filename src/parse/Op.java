package parse;

import wasm.Instruction;
import wasm.OpCode;

public interface Op {
    
    public OpCode getOpCode();

    public default Instruction getInstruction(TypeStack typestack) {
            return getOpCode().getOpType().getInstruction(this, typestack);
    }

}
