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

    private ValueType(char jvmtype, char jvminst, Class primitive, Class boxed) {
        this.jvmtype = jvmtype;
        this.jvminst = jvminst;
        this.primitive = primitive;
        this.boxed = boxed;
        this.unsigned = name().startsWith("U");
    }

    public char getJvmtype() {
        return jvmtype;
    }

    public char getJvminst() {
        return jvminst;
    }

    public int getStackSize() {
        int bitsz = Integer.valueOf(name().substring(1));
        return bitsz / 32;
    }

    public String getSizeSuffix() {
        return getStackSize() == 2?"2":"";
    }

    public String getPrefix() {
        if (this == V00) {
            return "";
        }
        return String.format("%s%d_",isFixed()?'I':'F',getStackSize()*32);
    }
    
    public String primitive() {
        return primitive.getSimpleName();
    }

    public Class<?> primitiveClass() {
        return primitive;
    }

    public String boxed() {
        return boxed.getSimpleName();
    }

    public Class<?> boxedClass() {
        return boxed;
    }

    public boolean isFixed() {
        return name().charAt(0) != 'F';
    }

    public boolean isUnsigned() {
        return name().charAt(0) == 'U';
    }

    public String promoted() {
        if (bitlength() > 0 && bitlength() < 32) {
            return valueOf(name().substring(0, 1) + "32").primitive();
        }
        return primitive();
    }

    public String nameInMethod() {
        String prefix = unsigned ? "U" : "";
        String name = primitive();
        String head = name.substring(0, 1);
        String tail = name.substring(1);
        return prefix + head.toUpperCase() + tail;
    }

    public int bitlength() {
        return Integer.parseInt(name().substring(1, 3));
    }

    public int bytelength() {
        return (bitlength() + 7) / 8;
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
