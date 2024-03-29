package parse;

import java.util.function.BiConsumer;

import static parse.Reason.M211;

public enum SectionType {

    // sections must be in this order apart from section 0
    st_custom(0, Custom::parseCustom),
    st_type(1, WasmModule::setTypes),
    st_import(2, WasmModule::setImports),
    st_function(3, ParseMethods::parseFnSig),
    st_table(4, Table::parse),
    st_memory(5, Memory::parse),
    st_global(6, Global::parse),
    st_export(7, ParseMethods::parseExports),
    st_start(8, WasmModule::setStart),
    st_element(9, ParseMethods::parseElements),
    st_datacount(12, ParseMethods::parseDataCount),
    st_code(10, LocalFunction::parse),
    st_data(11, Data_segment::parse),
    st_end(Integer.MAX_VALUE,null),
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
        // "malformed section id"
        throw new ParseException(M211,"section id = %d",idx);
    }

}
