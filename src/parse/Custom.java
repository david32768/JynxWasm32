package parse;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public enum Custom {

    NAME(CustomName::parseNames),
    PRODUCERS(Custom::parseProducers),
    TARGET_FEATURES(Custom::parseTargetFeatures),
    ;
    
    private final BiConsumer<WasmModule, Section> parsefn;

    private Custom(BiConsumer<WasmModule, Section> parsefn) {
        this.parsefn = parsefn;
    }

    private static Optional<Custom> getInstance(String str) {
        return Stream.of(values())
                .filter(c -> c.name().equalsIgnoreCase(str))
                .findAny();
    }
    
    public static void parseCustom(WasmModule module, Section section) {
        String name = section.getName();
        Optional<Custom> optcustom = getInstance(name);
        if (optcustom.isPresent()) {
            Logger.getGlobal().fine(String.format("custom name = %s", name));
            optcustom.get().parsefn.accept(module, section);
        } else {
            if (name.startsWith(".debug")) {
                Logger.getGlobal().fine(String.format("custom name = %s; ignored", name));
            } else {
                Logger.getGlobal().info(String.format("custom name = %s; ignored", name));
            }
        }
    }

    private final static String TAB = "      ";
    private final static char NL = '\n';
    
// https://github.com/WebAssembly/tool-conventions/blob/main/ProducersSection.md
   private static void parseProducers(WasmModule module, Section section) {
       StringBuilder sb = new StringBuilder();
       sb.append(PRODUCERS).append(NL);
       int fieldct = section.getU32();
       for (int i = 0 ; i < fieldct; ++i) {
           String fieldname = section.getName();
           int entryct = section.getU32();
           for (int j = 0; j < entryct; ++j) {
               String entryname = section.getName();
               String entryversion = section.getName();
               sb.append(TAB)
                       .append(fieldname)
                       .append(' ')
                       .append(entryname)
                       .append(' ')
                       .append(entryversion)
                       .append(NL);
           }
       }
       Logger.getGlobal().config(sb.toString());
   }
   
   // https://github.com/WebAssembly/tool-conventions/blob/main/Linking.md#target-features-section
   private static void parseTargetFeatures(WasmModule module, Section section) {
       StringBuilder sb = new StringBuilder();
       sb.append(TARGET_FEATURES).append(NL);
       int fieldct = section.getU32();
       for (int i = 0 ; i < fieldct; ++i) {
           char prefix = (char)section.getUByte();
           String feature = section.getName();
           sb.append(TAB)
                   .append(prefix)
                   .append(feature)
                   .append(NL);
       }
       Logger.getGlobal().info(sb.toString());
   }
   
}
