package jynxwasm32;

public enum JynxOpCode {
    
    // for initialisation of WASM program
    MEMORY_NEW,
    MEMORY_GLOBAL_GET,
    MEMORY_GLOBAL_SET,
    MEMORY_CHECK,
    STRING_CONST,
    BASE64_STORE,

    TABLE_NEW,
    TABLE_GLOBAL_GET,
    TABLE_GLOBAL_SET,
    ADD_ENTRY,
    
    LOG,
    ;
}
