package parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import static parse.CustomNameLayout.*;
import util.HexDump;

public enum CustomName {

    // Standard
    
    MODULE(0,NAME),
    FUNCTION(1,NAMEMAP),
    LOCAL(2,INDIRECT),

    // https://github.com/WebAssembly/extended-name-section/blob/main/proposals/extended-name-section/Overview.md

    LABEL(3,INDIRECT),
    TYPE(4,NAMEMAP),
    TABLE(5,NAMEMAP),
    MEMORY(6,NAMEMAP),
    GLOBAL(7,NAMEMAP),
    ELEMENT(8,NAMEMAP),
    DATA(9,NAMEMAP),
    ;

    private final int id;
    private final CustomNameLayout layout;
    
    private CustomName(int id, CustomNameLayout layout) {
        assert id == ordinal();
        this.id = id;
        this.layout = layout;
    }
    
    private static Optional<CustomName> getInstance(int id) {
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
        module.setLastSection(SectionType.st_end);
        while (section.hasRemaining()) {
            int id = section.getUByte();
            Section namesect = Section.getSubSection(section);
            Optional<CustomName> dntopt = CustomName.getInstance(id);
            if (!dntopt.isPresent()) {
                int len = namesect.getPayload_len();
                String hexstr = HexDump.printHex(namesect.getByteBuffer(), len);
                Logger.getGlobal().warning(String.format("unknown name subsection id - %d: (length %d) ignored%n%s",
                        id, len,hexstr));
                continue;
            }
            CustomName dnt = dntopt.get();
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

