package parse;

public enum ValueType {
    I08('I', 'i', byte.class, Byte.class),
    I16('I', 'i', short.class, Short.class),
    I32('I', 'i', int.class, Integer.class),
    I64('J', 'l', long.class, Long.class),
    U08('I', 'i', byte.class, Byte.class),
    U16('I', 'i', short.class, Short.class),
    U32('I', 'i', int.class, Integer.class),
    U64('J', 'l', long.class, Long.class),
    F32('F', 'f', float.class, Float.class),
    F64('D', 'd', double.class, Double.class),
    B32('Z', 'i', boolean.class, Boolean.class),
    V00('V', ' ', void.class, Void.class),
    U01('I', 'i', byte.class, Byte.class),
    X32('X', '?', int.class, Integer.class),
    ;

    private final char jvmtype;
    private final char jvminst;
    private final Class primitive;
    private final Class boxed;
    private final boolean unsigned;
    private final int bitlength;
    private final int bytelength;
    private final int alignment;

    private ValueType(char jvmtype, char jvminst, Class primitive, Class boxed) {
        this.jvmtype = jvmtype;
        this.jvminst = jvminst;
        this.primitive = primitive;
        this.boxed = boxed;
        this.unsigned = name().startsWith("U");
        this.bitlength = Integer.parseInt(name().substring(1, 3));
        this.bytelength = (bitlength +7)/8;
        this.alignment = bytelength == 0?0: 32 - Integer.numberOfLeadingZeros(bytelength - 1);
    }

    public char getJvmtype() {
        return jvmtype;
    }

    private char getJvminst() {
        return jvminst;
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

    public Number getZero() {
        return 0;
    }

    public boolean isInstance(Object obj) {
        return boxed.isInstance(obj);
    }

    public ValueType getUnsigned() {
        return getInstance("U" + name().substring(1));
    }
    
    public boolean isCompatible(ValueType vt2) {
        if (this == vt2) {
            return true;
        }
        return this.getJvminst() == vt2.getJvminst();
    }

}
