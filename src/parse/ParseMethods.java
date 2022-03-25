package parse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import static parse.ValueType.V00;

public class ParseMethods {
    
    public static void parseDylink(WasmModule module, Section section) {
        if (module.getLastid() != 0) {
            throw new IllegalStateException();
        }
        int memsz = section.getU32();
        int memalgn = section.getU32();
        int tabsz = section.getU32();
        int tabalgn = section.getU32();
        Logger.getGlobal().fine(String.format("memsz = %d memalgn = %d tabsz = %d tabalgn = %d",
                memsz,memalgn,tabsz,tabalgn));
        int count = section.vecsz();
        if (count == 0) {
            Logger.getGlobal().info("no dynamic libraries");
            return;
        }
        for (int i = 0; i < count;++i) {
            String libname = section.getName();
            Logger.getGlobal().fine(String.format("libray %d %s", i, libname));
        }
    }
    
    public static FnType[] parseTypes(WasmModule module,Section section) {
        int typect = section.vecsz();
        FnType[] types = new FnType[typect];
        for (int i = 0; i < typect; i++) {
            // spec 5.3.3
            section.expectWasmType(WasmType.Func);
            int parmct = section.vecsz();
            ValueType[] vtarr = new ValueType[parmct];
            for (int j = 0; j < parmct;j++) {
                vtarr[j] = section.getValueType();
            }
            int results = section.vecsz();
            ValueType retvt = results == 0?V00:section.getValueType();
            types[i] = new FnType(retvt,vtarr);
            Logger.getGlobal().fine(String.format("type %d = %s",i,types[i]));
            if (results > 1) {
                throw new RuntimeException("more than one return parameter");
            }
        }
        return types;
    }
    
/*
    #### Import entry
    | Field | Type | Description |
    | ----- | ---- | ----------- |
    | module_len | `varuint32` | module string length |
    | module_str | `bytes` | module string of `module_len` bytes |
    | field_len | `varuint32` | field name length |
    | field_str | `bytes` | field name string of `field_len` bytes |
    | kind | `external_kind` | the kind of definition being imported |
*/

    public static void parseImports(WasmModule module, Section section)  {
        int importct = section.vecsz();
        for (int i = 0; i < importct;i++) {
            String module_str = section.getName();
            String field_str = section.getName();
            KindType kt = KindType.getInstance(section.getUByte());
            Kind kind;
            KindName kn;
            switch (kt) {
                case Function:
                    int index = section.funcidx();
                    kn = new KindName(kt, module_str, field_str, Status.IMPORTED, module.funcidx());
                    ImportedFunction impfn = new ImportedFunction(module.attypeidx(index),kn);
                    module.addFunction(impfn);
                    kind = impfn;
                    break;
                case Table:
                    kn = new KindName(kt, module_str, field_str, Status.IMPORTED, module.tableidx());
                    Table table = Table.parseKind(section,kn);;
                    module.addTable(table);
                    kind = table;
                    break;
                case Memory:
                    kn = new KindName(kt, module_str, field_str, Status.IMPORTED, module.memidx());
                    Memory memory = new Memory(Limits.parse(section),kn);
                    module.addMemory(memory);
                    kind = memory;
                    break;
                case Global:
                    kn = new KindName(kt, module_str, field_str, Status.IMPORTED, module.globidx());
                    Global global = new Global(section,kn);
                    module.addGlobal(global);
                    kind = global;
                    break;
                default:
                    throw new EnumConstantNotPresentException(kt.getClass(), kt.name());
            }
            Logger.getGlobal().fine(String.format("import %s", kind));
        }
    }
    
/*
    ### Function section

The function section _declares_ the signatures of all functions in the
module (their definitions appear in the [code section](#code-section)).

| Field | Type | Description |
| ----- | ---- | ----------- |
| count | `varuint32` | count of signature indices to follow |
| types | `varuint32*` | sequence of indices into the type section |

*/
    
    public static void parseFnSig(WasmModule module, Section section) {
        int count = section.vecsz();
        for (int i = 0; i < count; i++) {
            int index = section.funcidx();
            FnType fntype = module.attypeidx(index);
            KindName kn = new KindName(KindType.Function, module.getName(), null, Status.PRIVATE, module.funcidx());
            WasmFunction fn = new LocalFunction(fntype,kn);
            module.addFunction(fn);
            Logger.getGlobal().fine(String.format("signature %d = %s", i, fntype));
        }
    }

/*    
#### Export entry
| Field | Type | Description |
| ----- | ---- | ----------- |
| field_len | `varuint32` | field name string length |
| field_str | `bytes` | field name string of `field_len` bytes |
| kind | `external_kind` | the kind of definition being exported |
| index | `varuint32` | the index into the corresponding [index space](Modules.md) |

For example, if the "kind" is `Function`, then "index" is a 
[function index](Modules.md#function-index-space). Note that, in the MVP, the
only valid index value for a memory or table export is 0.
*/
    
    public static void parseExports(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0; i < count;i++) {
            String field_str = section.getName();
            KindType kt = KindType.getInstance(section.getUByte());
            int index;
            String name = module.getName();
            Kind kind;
            switch(kt) {
                case Function:
                    index = section.funcidx();
                    kind = module.atfuncidx(index);
                    break;
                case Table:
                    index = section.tableidx();
                    kind = module.attableidx(index);
                    break;
                case Memory:
                    index = section.memidx();
                    kind = module.atmemidx(index);
                    break;
                case Global:
                    index = section.globalidx();
                    kind = module.atglobidx(index);
                    break;
                default:
                    throw new EnumConstantNotPresentException(kt.getClass(), kt.name());
            }
            kind.exportNames(name, field_str);
            Logger.getGlobal().fine(String.format("export %s as %s index = %d", field_str, kt,index));
        }
    }

/*
    ### Start section

The start section declares the [start function](Modules.md#module-start-function).

| Field | Type | Description |
| ----- | ---- | ----------- |
| index | `varuint32` | start function index |
*/
    
    public static int parseStart(Section section) {
        if (section == null) {
            return -1;
        }
        int index = section.funcidx();
        return index;
    }

    public static void parseNames(WasmModule module, Section section) {
        module.setLastId(Integer.MAX_VALUE);
        while (section.hasRemaining()) {
            int id = section.getUByte();
            Section namesect = Section.getSubSection(section);
            switch(id) {
                case 1: // function map (idx name)
                    Map<String,Integer> names = new HashMap<>();
                    int count = namesect.vecsz();
                    for (int i = 0; i < count;++i) {
                        int idx = namesect.funcidx();
                        String nameidx = namesect.getName();
                        WasmFunction fn = module.atfuncidx(idx);
                        String parms = fn.getFnType().jvmString();
                        String demangled = rust.Demangle.demangle(nameidx);
                        nameidx = rust.Demangle.getMethodName(nameidx);
                        Integer used = names.put(nameidx + parms,idx);
                        if (used == null) {
                            module.setName(idx, nameidx);
                            Logger.getGlobal().fine("; " + demangled);
                        } else {
                            Logger.getGlobal().fine(String.format("fnnum = %d parms = %s name duplicate of %d %s",
                                    idx, parms,used, nameidx));
                        }
                    }
                    assert !namesect.hasRemaining();
                    break;
                case 0: // module name
                    String modname = namesect.getName();
                    module.setModname(modname);
                    Logger.getGlobal().fine(String.format("module name = %s", modname));
                    assert !namesect.hasRemaining();
                    break;
                case 2:
                    int fncount = namesect.vecsz();
                    for (int i = 0; i < fncount;++i) {
                        int fnidx = namesect.funcidx();
                        int localcount = namesect.vecsz();
                        for (int j = 0; j < localcount;++j) {
                            int localidx = namesect.localidx();
                            String localname = namesect.getName();
                            Logger.getGlobal().fine(
                                    String.format("fnidx = %d localidx = %d name = %s", fnidx, localidx, localname));
                        }
                    }
                    Logger.getGlobal().warning(String.format("local names not supported"));
                    assert !namesect.hasRemaining();
                    break;
                default:
                    int len = namesect.getPayload_len();
                    StringBuilder sb = new StringBuilder(len);
                    while(namesect.hasRemaining()) {
                        int b = namesect.getUByte();
                        if (sb.length() > 60) {
                            continue;
                        }
                        if (b >=0x20 && b < 0x7f) {
                            sb.append((char)b);
                        } else {
                            sb.append('.');
                        }
                    }
                    Logger.getGlobal().warning(String.format("unknown id - %d: subsection (length %d) ignored%n%s",
                            id, namesect.getPayload_len(),sb.toString()));
            }
        }
    }

/*
    ### Element section

The encoding of the [Elements section](Modules.md#elements-section):

| Field | Type | Description |
| ----- | ---- | ----------- |
| count | `varuint32` | count of element segments to follow |
| entries | `elem_segment*` | repeated element segments as described below |

a `elem_segment` is:

| Field | Type | Description |
| ----- | ---- | ----------- |
| index | `varuint32` | the [table index](Modules.md#table-index-space) (0 in the MVP) |
| offset | `init_expr` | an `i32` initializer expression that computes the offset at which to place the elements |
| num_elem | `varuint32` | number of elements to follow |
| elems | `varuint32*` | sequence of [function indices](Modules.md#function-index-space) |


    */    
    public static void parseElements(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0; i < count ; i++) {
            int index = section.tableidx();
            Table table = module.attableidx(index);
            int offset = Expression.parseConstant(module, section).intValue();
            int num_elem = section.vecsz();
            WasmFunction[] functions = new WasmFunction[num_elem];
            for (int j = 0; j < num_elem;j++) {
                int function_index = section.funcidx();
                functions[j] = module.atfuncidx(function_index);
            }
            table.addElement(offset,functions);
            Logger.getGlobal().fine(String.format("%s element offset = %d length = %d",table,offset,functions.length));
        }
    }

}
