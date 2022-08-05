package parse;

import java.util.logging.Logger;
import wasm.Instruction;

public class Global implements Kind {
/*or, if the `kind` is `Global`:

| Field | Type | Description |
| ----- | ---- | ----------- |
| type | `value_type` | type of the imported global |
| mutability | `varuint1` | `0` if immutable, `1` if mutable; must be `0` in the MVP |
*/
    
    private final ValueType type;
    private final boolean mutable;
    private final Instruction constinst;

    private final Number value;
    private KindName kindName;
    
    public Global(Section section, KindName kindName)  {
        this.type = section.getValueType();
        this.mutable = getMutability(section);
        this.constinst = null;
        this.kindName = kindName;
        this.value = 0;
    }

    public Global(ValueType type, boolean mutable, Instruction constinst, KindName kindName) {
        this.type = type;
        this.mutable = mutable;
        this.constinst = constinst;
        this.value = Expression.evalConstant(constinst);
        this.kindName = kindName;
    }

    private static boolean getMutability(Section section) {
        try {
            return section.getFlag();
        } catch (IllegalArgumentException ex) {
            String msg = String.format("invalid mutability%n%s",ex.getMessage());
            throw new IllegalArgumentException(msg,ex);
        }
    }
    
    public ValueType getType() {
        return type;
    }

    public Number getValue() {
        return value;
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    public boolean isMutable() {
        return mutable;
    }

    @Override
    public void exportNames(String module_name,String field_name) {
        kindName = kindName.exportNames(module_name, field_name);
    }

    @Override
    public String getModuleName() {
        return kindName.getModuleName();
    }

    @Override
    public String getFieldName() {
        return kindName.getFieldName();
    }

    public Instruction getConstinst() {
        return constinst;
    }

    @Override
    public String toString() {
        return String.format("Global %s %s type = %s",kindName.getModuleName(),kindName.getFieldName(),type);
    }
    

    /*
    ### Global section

The encoding of the [Global section](Modules.md#global-section):

| Field | Type | Description |
| ----- | ---- | ----------- |
| count | `varuint32` | count of global variable entries |
| globals | `global_variable*` | global variables, as described below |

#### Global Entry

Each `global_variable` declares a single global variable of a given type, mutability
and with the given initializer.

| Field | Type | Description |
| ----- | ---- | ----------- |
| type | `value_type` | type of the variables |
| mutability | `varuint1` | `0` if immutable, `1` if mutable |
| init | `init_expr` | the initial value of the global |

Note that, in the MVP, only immutable global variables can be exported.

    */
    public static void parse(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0;i < count;i++) {
            ValueType type = section.getValueType();
            boolean mutable = getMutability(section);
            Instruction constinst = Expression.parseInstruction(module, section);
            KindName kn = new KindName(KindType.Global,module.getName(),null,Status.PRIVATE,module.globidx());
            Global global = new Global(type, mutable,constinst,kn);
            Logger.getGlobal().fine(String.format("global %d = %s",i,global));
            module.addGlobal(global);
        }
    }
}
