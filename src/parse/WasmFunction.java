package parse;

public interface WasmFunction extends Kind {
    
    public FnType getFnType();

    default void setName(String name) {
        throw new UnsupportedOperationException();
    }

    default boolean hasCode() {
        return false;
    }
}
