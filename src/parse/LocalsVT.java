package parse;

public class LocalsVT {

    private final int count;
    private final ValueType vt;

    public LocalsVT(int count, ValueType vt) {
        this.count = count;
        this.vt = vt;
    }

    public ValueType vt() {
        return vt;
    }

    public int count() {
        return count;
    }
    
    
}
