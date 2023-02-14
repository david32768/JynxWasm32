package parse;

public class ParseException extends IllegalArgumentException {

    private final Reason reason;

    public ParseException(Reason reason, String format, Object... objects) {
        super(reason.getMessage(format, objects));
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
    
}
