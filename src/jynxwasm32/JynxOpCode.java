package jynxwasm32;

import parse.ValueType;

public enum JynxOpCode {
    
    // for initialisation of WASM program
    ENVIRONMENT_NEW,
    
    ENVIRONMENT_IMPORT_TABLE,
    ENVIRONMENT_ADD_TABLE,
    ENVIRONMENT_EXPORT_TABLE,
    
    ENVIRONMENT_IMPORT_STORAGE,
    ENVIRONMENT_ADD_STORAGE,
    ENVIRONMENT_EXPORT_STORAGE,
    
    MEMORY_NEW,
    MEMORY_CHECK,
    STRING_CONST,
    BASE64_STORE,

    TABLE_BUILD,
    ADD_ENTRY,
    TABLE_CREATE,

    I32_LOCAL_INIT,
    I64_LOCAL_INIT,
    F32_LOCAL_INIT,
    F64_LOCAL_INIT,
    
    UNWIND,
    
    LOG,
    ;
    
    static JynxOpCode localInit(ValueType vt) {
        switch(vt) {
            case I32:
                return I32_LOCAL_INIT;
            case I64:
                return I64_LOCAL_INIT;
            case F32:
                return F32_LOCAL_INIT;
            case F64:
                return F64_LOCAL_INIT;
            default:
                throw new EnumConstantNotPresentException(vt.getClass(), vt.name());
        }
    }
    
}
