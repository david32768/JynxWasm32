package parse;

import java.util.logging.Logger;
import wasm.Instruction;

public class Global implements Kind {
    private final ValueType type;
    private final boolean mutable;
    private final Instruction constinst;

    private final Number value;
    private KindName kindName;
    
    public Global(ValueType type, boolean mutable, KindName kindName)  {
        this(type, mutable, null, 0, kindName);
    }

    public Global(ValueType type, boolean mutable, Instruction constinst, Number value, KindName kindName) {
        this.type = type;
        this.mutable = mutable;
        this.constinst = constinst;
        this.value = value;
        this.kindName = kindName;
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
    

    public static void parse(WasmModule module, Section section)  {
        /*
        ### Global section

        The encoding of the [Global section](Modules.md#global-section):

        | Field   | Type               | Description                          |
        | ------- | ------------------ | ------------------------------------ |
        | count   | `varuint32`        | count of global variable entries     |
        | globals | `global_variable*` | global variables, as described below |

        */
        int count = section.vecsz();
        for (int i = 0;i < count;i++) {
            /*
            #### Global Entry

            Each `global_variable` declares a single global variable of a given type, mutability
            and with the given initializer.

            | Field      | Type         | Description                      |
            | ---------- | ------------ | -------------------------------- |
            | type       | `value_type` | type of the variables            |
            | mutability | `varuint1`   | `0` if immutable, `1` if mutable |
            | init       | `init_expr`  | the initial value of the global  |

            Note that, in the MVP, only immutable global variables can be exported.

            */
            ValueType type = section.getValueType();
            boolean mutable = section.getMutability();
            Instruction constinst = Expression.parseInstruction(module, section);
            KindName kn = new KindName(KindType.Global,module.getName(),null,Status.PRIVATE,module.globidx());
            Global global = new Global(type, mutable,constinst,Expression.evalConstant(constinst),kn);
            Logger.getGlobal().fine(String.format("global %d = %s",i,global));
            module.addGlobal(global);
        }
    }
}
