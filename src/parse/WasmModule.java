package parse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.logging.Logger;

import static parse.Reason.M100;
import static parse.Reason.M102;
import static parse.Reason.M103;
import static parse.Reason.M104;
import static parse.Reason.M106;
import static parse.Reason.M108;

public class WasmModule {

    private final String name;
    private SectionType lastSection = SectionType.st_custom;
    private FnType[] types = new FnType[0];
    private String modname; // set by section 0 name subsection 0

    private final ArrayList<WasmFunction> functions = new ArrayList<>();
    private final ArrayList<Table> tables = new ArrayList<>();
    private final ArrayList<Memory> memories = new ArrayList<>();
    private final ArrayList<Global> globals = new ArrayList<>();

    private final ArrayList<String> producers = new ArrayList<>();
    
    private WasmFunction startfn;
    private int impfns = 0;
    private int imptabs = 0;
    private int impmems = 0;
    private int impglobs = 0;

    private Integer datacount;
    
    public final static Logger LOG = Logger.getGlobal();
    
    private WasmModule(String name) {
        this.name = name;
    }

    public String getName() {
        return modname == null?name:modname;
    }

    public SectionType getLastSection() {
        return lastSection;
    }

    public ArrayList<WasmFunction> getFunctions() {
        return functions;
    }

    public ArrayList<Global> getGlobals() {
        return globals;
    }

    public ArrayList<Memory> getMemories() {
        return memories;
    }

    public ArrayList<Table> getTables() {
        return tables;
    }

    public WasmFunction getStart() {
        return startfn;
    }

    public ArrayList<String> getProducers() {
        return producers;
    }    
    
    public void setModname(String modname) {
        this.modname = modname;
    }

    public void setTypes(Section section) {
        this.types = ParseMethods.parseTypes(this, section);
    }

    public void setImports(Section section) {
        ParseMethods.parseImports(this,section);
        impfns = functions.size();
        impglobs = globals.size();
        impmems = memories.size();
        imptabs = tables.size();
    }

    public void setStart(Section section) {
        assert startfn == null;
        int start = ParseMethods.parseStart(section);
        if (start >= 0) {
            startfn = atfuncidx(start);
            FnType type = startfn.getFnType();
            if (!type.isRunable()) {
                Logger.getGlobal().severe(String.format("start function %s is not RUNABLE", startfn));
            }
        }
    }

    public int getLocalFnIndex(int i) {
        return i + impfns;
    }
    
    public int funcidx() {
        return functions.size();
    }
    
    public int localfuns() {
        return functions.size() - impfns;
    }
    
    public int globidx() {
        return globals.size();
    }
    
    public int memidx() {
        return memories.size();
    }
    
    public int tableidx() {
        return tables.size();
    }

    public WasmFunction atfuncidx(int funcidx) {
        return functions.get(funcidx);
    }
    
    public Global atglobidx(int index) {
        return globals.get(index);
    }
    
    public Memory atmemidx(int memidx) {
        return memories.get(memidx);
    }
    
    public Table attableidx(int tableidx) {
        if (tables.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return tables.get(tableidx);
    }

    public FnType attypeidx(int typeidx) {
        return types[typeidx];
    }

    public WasmFunction getFunction(int index) {
        return functions.get(index);
    }
    
    public void addFunction(WasmFunction function) {
        functions.add(function);
    }
    
    public void addGlobal(Global global) {
        globals.add(global);
    }
    
    public void addMemory(Memory memory) {
        if (!memories.isEmpty()) {
            throw new IllegalArgumentException("more than one memory");
        }
        memories.add(memory);
    }
    
    public void addTable(Table table) {
        if (!tables.isEmpty()) {
            throw new IllegalArgumentException("more than one table");
        }
        tables.add(table);
    }

    public void setDataCount(Integer datacount) {
        this.datacount = datacount;
    }

    public Integer getDataCount() {
        return datacount;
    }


    public void addProducer(String producer) {
        producers.add(producer);
    }
    
    public void setLastSection(SectionType type) {
        if (type != SectionType.st_custom) {
            if (type.compareTo(lastSection) <= 0) {
                // "junk after last section"
                throw new ParseException(M102,"sections in wrong order - last was %s(%d), current is %s(%d)%n",
                        lastSection,lastSection.getId(),type,type.getId());
            }
            lastSection = type;
        }
    }
    
    // spec 5.5.15
    public static WasmModule getModule(String name, ByteBuffer stream) throws IOException {
        stream.order(ByteOrder.LITTLE_ENDIAN);
        if (stream.remaining() < 4) {
            //  // "unexpected end"
            throw new ParseException(M106,"length %d is less than required for magic header",stream.remaining());
        }
        int magic = stream.getInt();
        if (magic != 0x6d736100) {
            stream.order(ByteOrder.BIG_ENDIAN);
            magic = stream.getInt(0);
            // "magic header not detected"
            throw new ParseException(M103,"'/0asm' not present - found %08x", magic);
        }
        if (stream.remaining() < 4) {
            // "unexpected end"
            throw new ParseException(M106,"remaining length %d is less than required for version",
                    stream.remaining());
        }
        int version = stream.getInt();
        if (version != 1) {
            // "unknown binary version"
            throw new ParseException(M108,"version = %d",version);
        }
        Logger.getGlobal().fine(String.format("Version number = %d", version));
        WasmModule module = new WasmModule(name);
        while(stream.hasRemaining()) {
            int id = stream.get() & 0xff;
            SectionType type = SectionType.getInstance(id);
            Section section = Section.getInstance(type,stream);
            Logger.getGlobal().fine(String.format("%s(%d) section payload = %d",
                    type,id,section.getPayload_len()));
            module.setLastSection(type);
            type.parse(module, section);
            if (section.hasRemaining() && id != 0) {
                // "section size mismatch"
                throw new ParseException(M104,"%d bytes remaining in section %s(%d)",section.remaining(),type,id);
            }
        }
        int localfnct = module.localfuns();
        if (localfnct > 0 && !module.atfuncidx(module.getLocalFnIndex(0)).hasCode()) {
            // "function and code section have inconsistent lengths"
            throw new ParseException(M100,"local func count = %d code count = 0",localfnct);
        }
        return module;
    }
    
}
