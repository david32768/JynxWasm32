package parse;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;


public class Table implements Kind {

    private final Limits limits;
    private final WasmFunction[] elements;
    private final Set<FnType> fntypes;

    private KindName kindName;
    
    public Table(Limits limits,KindName kindName) {
        this.limits = limits;
        this.elements = new WasmFunction[limits.getInitial()];
        this.fntypes = new LinkedHashSet<>();
        this.kindName = kindName;
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    @Override
    public void exportNames(String module_name,String field_name) {
        kindName = kindName.exportNames(module_name, field_name);
    }

    public int getTableNum() {
        return kindName.getNumber();
    }

    public WasmFunction[] getElements() {
        return elements;
    }
    
    public void addElement(int offset, WasmFunction[] functions) {
        for (int i = 0; i < functions.length;++i) {
            int elementnum = i + offset;
            WasmFunction shouldBeNull = elements[elementnum];
            WasmFunction toBeAdded = functions[i];
            if (shouldBeNull != null && shouldBeNull != toBeAdded) {
                String message = String.format("Table element %d already defined%n %s would be replaced by %s%n",
                        elementnum,shouldBeNull.getName(),toBeAdded.getName());
                throw new IllegalStateException(message);
            }
            elements[elementnum] = toBeAdded;
            fntypes.add(toBeAdded.getFnType());
        }
        Logger.getGlobal().fine(String.format("fntypes size = %d", fntypes.size()));
    }

    @Override
    public String toString() {
        return String.format("Table %s %s %s", 
                kindName.getModuleName(),kindName.getFieldName(),limits);
    }

    /*
    ### Table section

The encoding of a [Table section](Modules.md#table-section):

| Field   | Type          | Description                                           |
| ------- |  ------------ | ----------------------------------------------------- |
| count   | `varuint32`   | indicating the number of tables defined by the module |
| entries | `table_type*` | repeated `table_type` entries as described below      |

| Field        | Type               | Description                                        |
| ------------ | ------------------ | -------------------------------------------------- |
| element_type | `varuint7`         | `0x20`, indicating [`anyfunc`](Semantics.md#table) |
|              | `resizable_limits` | see [above](#resizable_limits)                     |

In the MVP, the number of tables must be no more than 1.
    */

    public static void parse(WasmModule module, Section section)  {
        int count = section.vecsz();
        if (count > 1) throw new IllegalArgumentException("number of tables > 1; found " + count);
        for (int i = 0;i < count;i++) {
            KindName kn = new KindName(KindType.Table, module.getName(), null, Status.PRIVATE, module.tableidx());
            Table table = parseKind(section,kn);
            module.addTable(table);
        }
        Logger.getGlobal().fine(String.format("number of tables = %d", count));
    }

    
    public static Table parseKind(Section section, KindName kn)  {
        /*
            | Field        | Type               | Description                                        |
            | ------------ | ------------------ | -------------------------------------------------- |
            | element_type | `varuint7`         | `0x20`, indicating [`anyfunc`](Semantics.md#table) |
            |              | `resizable_limits` | see (#resizable_limits)                     |
        */
        section.expectWasmType(WasmType.FuncRef);
        return new Table(Limits.parse(section),kn);
    }

}
