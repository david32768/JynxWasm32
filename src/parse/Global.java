package parse;

import java.util.logging.Logger;
import wasm.Instruction;

public class Global implements Kind {
    private final ValueType type;
    private final boolean mutable;
    private final ConstantExpression constexpr;

    private KindName kindName;
    
    public Global(ValueType type, boolean mutable, KindName kindName)  {
        this(type, mutable, null, kindName);
    }

    public Global(ValueType type, boolean mutable, ConstantExpression constexpr, KindName kindName) {
        this.type = type;
        this.mutable = mutable;
        this.constexpr = constexpr;
        this.kindName = kindName;
    }

    public ValueType getType() {
        return type;
    }

    public boolean usesInitGlobal() {
        return !isImported() && constexpr.usesGlobal();
    }
    
    public Number getValue() {
        return constexpr.evalConstant();
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    public boolean isMutable() {
        return mutable;
    }

    public boolean isFinal() {
        return !mutable && constexpr != null;
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

    public Instruction getConstInst() {
        return constexpr.getConstInst();
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

            */
            ValueType type = section.getValueType();
            boolean mutable = section.getMutability();
            ConstantExpression constexpr = ConstantExpression.parseConstantExpression(module, section);
            KindName kn = new KindName(KindType.Global,module.getName(),null,Status.PRIVATE,module.globidx());
            Global global = new Global(type, mutable,constexpr,kn);
            Logger.getGlobal().fine(String.format("global %d = %s",i,global));
            module.addGlobal(global);
        }
    }
}
