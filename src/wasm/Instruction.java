package wasm;

import parse.FnType;
import parse.ValueType;

public interface Instruction {

    FnType getFnType();
    OpCode getOpCode();
    
    
    default ValueType getBlockType() {
        throw new UnsupportedOperationException();
    }
    
}
