package main;

import java.util.Optional;
import java.util.stream.Stream;

public enum Action {

    _2JYNX("wasm"),
    _TESTPARSE("wast"),
    _PARSE("wasm"),
    ;
    
    private final String extension;

    private Action(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }
    
    
    public static Optional<Action> getInstance(String str) {
        return Stream.of(values())
                .filter(x->x.toString().equalsIgnoreCase(str))
                .findAny();
    }

    @Override
    public String toString() {
        return name().substring(1);
    }

}
