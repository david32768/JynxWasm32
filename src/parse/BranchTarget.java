package parse;

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
    
}
