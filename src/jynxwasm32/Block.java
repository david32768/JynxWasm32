package jynxwasm32;

import java.util.BitSet;
import wasm.OpCode;

public class Block {

    private final OpCode op;
    private final BitSet endVars;
    private final BitSet startVars;

    private boolean reached;

    public Block(OpCode op, BitSet startvars, BitSet endvars) {
        this.op = op;
        this.startVars = (BitSet)startvars.clone();
        this.endVars = (BitSet)endvars.clone();
        this.reached = false;
    }

    public OpCode op() {
        return op;
    }

    public BitSet startVars() {
        return (BitSet)startVars.clone();
    }
    
    public BitSet endVars() {
        return (BitSet)endVars.clone();
    }
    
    public void updateVarsForBranch(BitSet current) {
        if (op != OpCode.LOOP) {
            endVars.and(current);
            reached = true;
        }
    }

    public void updateVarsIfReached(BitSet current) {
        endVars.and(current);
        reached = true;
    }

    public boolean isReached() {
        return reached;
    }
    
}
