package parse;

public enum KindType {
    Function(0),
    Table(1),
    Memory(2),
    Global(3),
    ;
    
    private final int id;

    private KindType(int id) {
        this.id = id;
    }
    
    /*
    ### `external_kind`
    A single-byte unsigned integer indicating the kind of definition being imported or defined:
    * `0` indicating a `Function` [import](Modules.md#imports) or [definition](Modules.md#function-and-code-sections)
    * `1` indicating a `Table` [import](Modules.md#imports) or [definition](Modules.md#table-section)
    * `2` indicating a `Memory` [import](Modules.md#imports) or [definition](Modules.md#linear-memory-section)
    * `3` indicating a `Global` [import](Modules.md#imports) or [definition](Modules.md#global-section)
    */
    public static KindType getInstance(int idx) {
        for (KindType kt:values()) {
            if (kt.id == idx) return kt;
        }
        throw new UnsupportedOperationException("unknown kind - " + idx);
    }
    
}
