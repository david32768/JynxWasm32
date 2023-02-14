package parse;

import java.util.logging.Logger;
import java.util.Optional;
import static parse.Reason.M101;

import static parse.Reason.M208;
import static parse.Reason.M209;
import static parse.ValueType.V00;

public class ParseMethods {
    
    public static void parseDylink(WasmModule module, Section section) {
        if (module.getLastSection() != SectionType.st_custom) {
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
    
    public static void parseImports(WasmModule module, Section section)  {
        int importct = section.vecsz();
        for (int i = 0; i < importct;i++) {
            /*
            #### Import entry
            | Field      | Type            | Description                            |
            | ---------- | --------------- | ----------- -------------------------  |
            | module_len | `varuint32`     | module string length                   |
            | module_str | `bytes`         | module string of `module_len` bytes    |
            | field_len  | `varuint32`     | field name length                      |
            | field_str  | `bytes`         | field name string of `field_len` bytes |
            | kind       | `external_kind` | the kind of definition being imported  |
            */

            String module_str = section.getName();
            String field_str = section.getName();
            int idx = section.getUByte();
            Optional<KindType> optkt = KindType.getInstance(idx);
            if (!optkt.isPresent()) {
                // "malformed import kind"
                throw new ParseException(M209,"kind = %d",idx);
            }
            KindType kt = optkt.get();
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
                    /* or, if the `kind` is `Memory`:

                    | Field | Type               | Description                    |
                    | ----- | ------------------ | ------------------------------ |
                    |       | `resizable_limits` | see (#resizable_limits) |
                    
                    */
                    kn = new KindName(kt, module_str, field_str, Status.IMPORTED, module.memidx());
                    Memory memory = new Memory(Limits.parse(section),kn);
                    module.addMemory(memory);
                    kind = memory;
                    break;
                case Global:
                    /* or, if the `kind` is `Global`:

                    | Field      | Type         | Description                                              |
                    | ---------- | ------------ | -------------------------------------------------------- |
                    | type       | `value_type` | type of the imported global                              |
                    | mutability | `varuint1`   | `0` if immutable, `1` if mutable; must be `0` in the MVP |
                    
                    */
                    ValueType type = section.getValueType();
                    boolean mutable = section.getMutability();
                    kn = new KindName(kt, module_str, field_str, Status.IMPORTED, module.globidx());
                    Global global = new Global(type,mutable,kn);
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

| Field | Type        | Description                               |
| ----- | ----------- | ----------------------------------------- |
| count | `varuint32` | count of signature indices to follow      |
| types | `varuint32*`| sequence of indices into the type section |

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
| Field     | Type            | Description                                                |
| --------- | --------------- | ---------------------------------------------------------- |
| field_len | `varuint32`     | field name string length                                   |
| field_str | `bytes`         | field name string of `field_len` bytes                     |
| kind      | `external_kind` | the kind of definition being exported                      |
| index     | `varuint32`     | the index into the corresponding [index space](Modules.md) |

For example, if the "kind" is `Function`, then "index" is a 
[function index](Modules.md#function-index-space). Note that, in the MVP, the
only valid index value for a memory or table export is 0.
*/
    
    public static void parseExports(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0; i < count;i++) {
            String field_str = section.getName();
            int idx = section.getUByte();
            Optional<KindType> optkt = KindType.getInstance(idx);
            if (!optkt.isPresent()) {
                // "malformed export kind"
                throw new ParseException(M208,"kind = %d",idx);
            }
            KindType kt = optkt.get();
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
            String msg = String.format("export %s as %s index = %d", field_str, kt,index);
            Logger.getGlobal().fine(msg);
        }
    }

/*
    ### Start section

The start section declares the [start function](Modules.md#module-start-function).

| Field | Type        | Description          |
| ----- | ----------- | -------------------- |
| index | `varuint32` | start function index |
*/
    
    public static int parseStart(Section section) {
        if (section == null) {
            return -1;
        }
        int index = section.funcidx();
        return index;
    }

/*
    ### Element section

The encoding of the [Elements section](Modules.md#elements-section):

| Field   | Type            | Description                                  |
| ------- | --------------- | -------------------------------------------- |
| count   | `varuint32`     | count of element segments to follow          |
| entries | `elem_segment*` | repeated element segments as described below |

a `elem_segment` is:

| Field    | Type         | Description                                                                             |
| ----- ---| ------------ | --------------------------------------------------------------------------------------- |
| index    | `varuint32`  | the [table index](Modules.md#table-index-space) (0 in the MVP)                          |
| offset   | `init_expr`  | an `i32` initializer expression that computes the offset at which to place the elements |
| num_elem | `varuint32`  | number of elements to follow                                                            |
| elems    | `varuint32*` | sequence of [function indices](Modules.md#function-index-space)                         |


    */    
    public static void parseElements(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0; i < count ; i++) {
            if (!section.hasRemaining()) {
                throw new ParseException(M101, "%d %ss present but %d expected", i, section.getType(),count);
            }
            int index = section.tableidx();
            Table table = module.attableidx(index);
            ConstantExpression constexpr = ConstantExpression.parseConstantExpression(module, section);
            int num_elem = section.vecsz();
            WasmFunction[] functions = new WasmFunction[num_elem];
            for (int j = 0; j < num_elem;j++) {
                int function_index = section.funcidx();
                functions[j] = module.atfuncidx(function_index);
            }
            TableElement element = new TableElement(constexpr, functions);
            table.addElement(element);
            Logger.getGlobal().fine(String.format("%s element offset = %s length = %d",
                    table,constexpr.getConstantString(),functions.length));
        }
    }

    public static void parseDataCount(WasmModule module, Section section) {
        int count = section.getU32();
        module.setDataCount(count);
    }
}
