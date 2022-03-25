package parse;

public enum WasmType {
    I32(0x7f,ValueType.I32),
    I64(0x7e,ValueType.I64),
    F32(0x7d,ValueType.F32),
    F64(0x7c,ValueType.F64),
    FuncRef(0x70),
    Func(0x60),
    Empty(0x40),
    ;
        
    private final int encoding;
    private final ValueType valueType;

    private WasmType(int encoding, ValueType valueType) {
        this.encoding = encoding;
        this.valueType = valueType;
    }

    private WasmType(int encoding) {
        this.encoding = encoding;
        this.valueType = null;
    }

    public ValueType getValueType() {
        return valueType;
    }
        
    public ValueType getBlockType() {
        if (this == Empty) return ValueType.V00;
        if (valueType == null) {
            throw new IllegalArgumentException("invalid block type");
        }
        return valueType;
    }
        
    public static WasmType getInstance(int encoding) {
        for (WasmType lt:values()) {
            if (encoding == lt.encoding) return lt;
        }
        throw new IllegalArgumentException("unknown encoding for WASM Type - 0x" + Integer.toHexString(encoding));
    }
    
}
