package parse;

import static parse.ValueType.V00;

public class BranchTarget {
    
    private final int br2level;
    private final FnType unwind;

    public BranchTarget(int br2level, FnType unwind) {
        this.br2level = br2level;
        this.unwind = unwind;
    }

    public int getBr2level() {
        return br2level;
    }

    public FnType getUnwind() {
        return unwind;
    }

    public boolean needUnwind() {
        int numparms = unwind.numParms();
        ValueType rvt = unwind.getRtype();
        assert rvt == V00 || unwind.lastParm().isCompatible(rvt);
        int reqnum = rvt == V00? 0: 1;
        return numparms != reqnum;
    }
}
