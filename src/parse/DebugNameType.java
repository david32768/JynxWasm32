package parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import static parse.DebugNameLayout.*;
import util.HexDump;

public enum DebugNameType {

    // https://github.com/WebAssembly/extended-name-section/blob/main/proposals/extended-name-section/Overview.md

    MODULE(0,NAME),
    FUNCTION(1,NAMEMAP),
    LOCAL(2,INDIRECT),
    LABEL(3,INDIRECT),
    TYPE(4,NAMEMAP),
    TABLE(5,NAMEMAP),
    MEMORY(6,NAMEMAP),
    GLOBAL(7,NAMEMAP),
    ELEMENT(8,NAMEMAP),
    DATA(9,NAMEMAP),
    ;

    private final int id;
    private final DebugNameLayout layout;
    
    private DebugNameType(int id, DebugNameLayout layout) {
        assert id == ordinal();
        this.id = id;
        this.layout = layout;
    }
    
    private static Optional<DebugNameType> getInstance(int id) {
        return Stream.of(values())
                .filter(dnt -> dnt.id == id)
                .findAny();
    }
    
    private static void setDebugNames(Section namesect, String nametype,
            Function<Integer,CanHaveDebugName> setter) {
        Map<String,Integer> names = new HashMap<>();
        Map<Integer,String> named = new HashMap<>();
        int count = namesect.vecsz();
        // (idx,name)*
        for (int i = 0; i < count;++i) {
            int idx = namesect.funcidx();
            String nameidx = namesect.getName();
            CanHaveDebugName chdn = setter.apply(idx);
            String namedesc = nameidx + chdn.getDesc();
            String usedidx = named.putIfAbsent(idx, nameidx);
            if (usedidx != null) {
                Logger.getGlobal().warning(String.format("%s num = %d name = %s:  already named %s",
                        nametype,idx,nameidx,usedidx));
            }
            Integer usedname = names.putIfAbsent(namedesc,idx);
            if (usedname != null) {
                Logger.getGlobal().warning(String.format("%s num = %d name %s: name already used by %d",
                        nametype,idx, nameidx,usedname));
            }
            if (usedname == null && usedidx == null) {
                chdn.setDebugName(nameidx);;
                Logger.getGlobal().finest(String.format("; %s %d named %s",nametype,idx,nameidx));
            }
        }
    }
    
    public static void parseNames(WasmModule module, Section section) {
        module.setLastId(Integer.MAX_VALUE);
        while (section.hasRemaining()) {
            int id = section.getUByte();
            Section namesect = Section.getSubSection(section);
            Optional<DebugNameType> dntopt = DebugNameType.getInstance(id);
            if (!dntopt.isPresent()) {
                int len = namesect.getPayload_len();
                String hexstr = HexDump.printHex(namesect.getByteBuffer(), len);
                Logger.getGlobal().warning(String.format("unknown name subsection id - %d: (length %d) ignored%n%s",
                        id, len,hexstr));
                continue;
            }
            DebugNameType dnt = dntopt.get();
            switch(dnt) {
                case MODULE:
                    String modname = namesect.getName();
                    module.setModname(modname);
                    Logger.getGlobal().warning(String.format("module name = %s", modname));
                    assert !namesect.hasRemaining();
                    break;
                case FUNCTION:
                    setDebugNames(namesect, "function",module::atfuncidx);
                    assert !namesect.hasRemaining();
                    break;
                case LOCAL:
                    int fncount = namesect.vecsz();
                    for (int i = 0; i < fncount;++i) {
                        int fnidx = namesect.funcidx();
                        WasmFunction fn = module.atfuncidx(fnidx);
                        setDebugNames(namesect,"Local " + fn.getName(),fn::getLocal);
                    }
                    Logger.getGlobal().info(String.format("local names present and used"));
                    assert !namesect.hasRemaining();
                    break;
                // https://github.com/WebAssembly/extended-name-section/blob/main/proposals/extended-name-section/Overview.md
                case LABEL:
                case TYPE:
                case TABLE:
                case MEMORY:
                case GLOBAL:
                case ELEMENT:
                case DATA:
                    int len = namesect.getPayload_len();
                    namesect.discardRemaining();
                    Logger.getGlobal().info(String.format("%s names not used: subsection id - %d: length = %d",
                            dnt,id,len));
                    break;
                default:
                    throw new EnumConstantNotPresentException(dnt.getClass(), dnt.name());
            }
        }
    }
}
