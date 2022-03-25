package wasm;

import parse.FnType;
import parse.ValueType;

public interface Instruction {

    int getLevel();
    FnType getFnType();
    OpCode getOpCode();
    
    
    default ValueType getBlockType() {
        throw new UnsupportedOperationException();
    }
    
    default Number getImm1() {
        throw new UnsupportedOperationException();
    }
    
}
