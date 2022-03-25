package parse;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

public enum SectionType {

    st_custom(0, SectionType::parseCustom),
    st_type(1, WasmModule::setTypes),
    st_import(2, WasmModule::setImports),
    st_function(3, ParseMethods::parseFnSig),
    st_table(4, Table::parse),
    st_memory(5, Memory::parse),
    st_global(6, Global::parse),
    st_export(7, ParseMethods::parseExports),
    st_start(8, WasmModule::setStart),
    st_element(9, ParseMethods::parseElements),
    st_code(10, LocalFunction::parse),
    st_data(11, Data_segment::parse),
    ;

    private final int id;
    private final BiConsumer<WasmModule, Section> parsefn;

    private SectionType(int id, BiConsumer<WasmModule, Section> parsefn) {
        this.id = id;
        this.parsefn = parsefn;
    }

    public int getId() {
        return id;
    }

    public void parse(WasmModule module, Section section) {
        parsefn.accept(module, section);
    }

    @Override
    public String toString() {
        return name().substring(3);
    }

    public static SectionType getInstance(int idx) {
        for (SectionType st : values()) {
            if (st.id == idx) {
                return st;
            }
        }
        throw new IllegalArgumentException("unknown section type");
    }

    private static void parseCustom(WasmModule module, Section section) {
        String name = section.getName();
        if (name.equals("name")) {
            Logger.getGlobal().info(String.format("name = %s", name));
            ParseMethods.parseNames(module, section);
            return;
        }
        if (name.equals("dylink")) {
            Logger.getGlobal().info(String.format("name = %s", name));
            ParseMethods.parseDylink(module, section);
            return;
        }
        Logger.getGlobal().info(String.format("name = %s; ignored", name));
    }
}
