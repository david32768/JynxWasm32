package parse;

public enum ValueType {

    I32(),
    I64(),
    F32(),
    F64(),

    V00(),
    X32(),

    I08(I32),
    I16(I32),

    U01(I32),
    U08(I32),
    U16(I32),
    U32(I32),

    B32(I32),

    U64(I64),
    ;

    private final ValueType base;
    private final int bitlength;
    private final int bytelength;
    private final int alignment;

    private ValueType() {
        this(null);
    }

    private ValueType(ValueType base) {
        this.base = base == null? this: base;
        this.bitlength = Integer.parseInt(name().substring(1, 3));
        this.bytelength = (bitlength + 7)/8;
        this.alignment = bytelength == 0? 0: 32 - Integer.numberOfLeadingZeros(bytelength - 1);
    }

    public ValueType getBase() {
        return base;
    }

    public int getStackSize() {
        return (bytelength + 3)/4;
    }

    public boolean isFixed() {
        return name().charAt(0) != 'F';
    }

    public boolean isUnsigned() {
        return name().charAt(0) == 'U';
    }

    public int bitlength() {
        return bitlength;
    }

    public int bytelength() {
        return bytelength;
    }

    public int alignment() {
        return alignment;
    }
    
    public static ValueType getInstance(String token) {
        try {
            return valueOf(token);
        } catch (IllegalArgumentException iaex) {
            return null;
        }
    }

    public ValueType getUnsigned() {
        return getInstance("U" + name().substring(1));
    }
    
    public boolean isCompatible(ValueType vt2) {
        return this == vt2 || this.base == vt2.base;
    }

}
