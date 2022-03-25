package parse;

import java.util.ArrayList;
import java.util.logging.Logger;

public class Memory implements Kind {
/*or, if the `kind` is `Memory`:

| Field | Type | Description |
| ----- | ---- | ----------- |
| | `resizable_limits` | see [above](#resizable_limits) |
*/

    private final static int PAGE_SIZE = 1 << 16;   // 64K
    private final static int MAX_PAGES = 1 << 10;   // 64M
    
    private final Limits limits;
    private final ArrayList<Data_segment> data = new ArrayList<>();
    private KindName kindName;
    
    public Memory(Limits limits, KindName kindName) {
        this.limits = limits;
        this.kindName = kindName;
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    public int getMemoryNum() {
        return kindName.getNumber();
    }

    public Limits getLimits() {
        return limits;
    }

    @Override
    public void exportNames(String module_name,String field_name) {
        kindName = kindName.exportNames(module_name, field_name);
    }

    public ArrayList<Data_segment> getData() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("Memory %s %s initial = %d maximum = %d",
                kindName.getModuleName(),kindName.getFieldName()
                ,limits.getInitial()*PAGE_SIZE,limits.getMaximum()*PAGE_SIZE);
    }

    public void add(Data_segment ds) {
        data.add(ds);
    }
    

/*
    ### Memory section

ID: `memory`

The encoding of a [Memory section](Modules.md#linear-memory-section):

| Field | Type | Description |
| ----- |  ----- | ----- |
| count | `varuint32` | indicating the number of memories defined by the module |
| entries | `memory_type*` | repeated `memory_type` entries as described below |

| Field | Type | Description |
| ----- | ---- | ----------- |
| | `resizable_limits` | see [above](#resizable_limits) |

Note that the initial/maximum fields are specified in units of 
[WebAssembly pages](Semantics.md#linear-memory).

In the MVP, the number of memories must be no more than 1.


    */    
    public static void parse(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0;i < count;i++) {
            Limits limits = Limits.parse(section);
            KindName kn = new KindName(KindType.Memory, module.getName(), null, Status.PRIVATE, module.memidx());
            Memory memory = new Memory(limits,kn);
            module.addMemory(memory);
            Logger.getGlobal().fine(String.format("memory %d = %s", i, memory));
        }        
    }
}
