package parse;

import java.util.Optional;
import java.util.stream.Stream;

public enum Reason {

    M0("OK"),
    M999("Exception"),
    
    // w3c-1.0
    M100("function and code section have inconsistent lengths"),
    M101("invalid value type"), //NOT USED
    M102("junk after last section","unexpected content after last section"),
    M103("magic header not detected"),
    M104("section size mismatch"),
    M105("too many locals"),
    M106("unexpected end"),
    M107("unexpected end of section or function"),
    M108("unknown binary version"),
    M109("zero flag expected","zero byte expected"),

    // draft v2 messages used
    M200("END opcode expected"),
    M201("data count and data section have inconsistent lengths"),
    M204("illegal opcode"),
    M205("integer representation too long"),
    M206("integer too large"),
    M207("length out of bounds"),
    M208("malformed export kind"),
    M209("malformed import kind"),
    M211("malformed section id"),

    // draft V2 not supported
    M202("data count section required"),

    M210("malformed reference type"),

    ;
    private final String reason;
    private final String reason2;
    
    private Reason(String reason) {
        this(reason,reason);
    }

    private Reason(String reason, String reason2) {
        this.reason = reason;
        this.reason2 = reason2;
    }
    
    public String getMessage(String format, Object... objects) {
        String why = String.format(format,objects);
        return reason + "\n    " + why;
    }

    public static Optional<Reason> getInstance(String str) {
       return  Stream.of(values())
                .filter(r->str.equals(r.reason) || str.equals(r.reason2))
                .findAny();
    }

    public String reason() {
        return reason;
    }
    
    
}
