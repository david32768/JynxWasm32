package utility;

import java.util.Optional;
import java.util.stream.Stream;

enum TestSection {

    ASSERT_RETURN,
    ASSERT_TRAP,
    ASSERT_EXHAUSTION,
    INVOKE,
    ASSERT_MALFORMED,
    ASSERT_INVALID,

    MODULE_BINARY,
    ASSERT_UNLINKABLE,

    MODULE, // after any MODULE_x
    ;

    boolean starts(String str) {
        String uc_str = str.toUpperCase().replace(' ','_');
        return uc_str.startsWith(name());
    }
    
    public static Optional<TestSection> getInstance(String section) {
        try {
            return Optional.of(valueOf(section));
        } catch ( IllegalArgumentException iaex) {
            return Optional.empty();
        }
    }

    public static Optional<TestSection> getStartInstance(String section) {
        return Stream.of(values())
                .filter(sec-> sec.starts(section)) 
                .findAny();
    }

}
