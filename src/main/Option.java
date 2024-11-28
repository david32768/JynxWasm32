package main;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static main.Action.*;

public enum Option {
    
    LEVEL("changes log-level", _2JYNX, _TESTPARSE, _PARSE),
    CLASS_NAME_AS_IS(false,"stops changing first character of class name to upper case", _2JYNX),
    COMMENT(false,"add wasm ops as comments to Jynx output",_2JYNX),
    NAME("class_name ; default name is module-name else filename without the .wasm extension", _2JYNX),
    PACKAGE("package name ; default is 'wasirun'", _2JYNX),
    START("set start method if wasm start not set. default is '_start' if it exists", _2JYNX),
    ;

    private final boolean hasString;
    private final String msg;
    private final EnumSet<Action> actions;

    private Option(String msg, Action action, Action... actions) {
        this(true, msg, action, actions);
    }

    private Option(boolean has, String msg, Action action, Action... actions) {
        this.hasString = has;
        this.msg = msg;
        this.actions = EnumSet.of(action,actions);
    }

    private static Optional<Option> getInstance(String arg) {
        return Stream.of(values())
                .filter(opt -> arg.equalsIgnoreCase("--" + opt.name()))
                .findAny();
    }

    
    public static Map<Option,String> getOptions(Action action, String[] args) {
        Map<Option,String> result = new HashMap<>();
        int first = 1;
        int last = args.length - 1;
        for (int i = first; i < last; ++i) {
            String argi = args[i];
            Optional<Option> opt =  getInstance(argi);
            if (!opt.isPresent()) {
                String msg = String.format("unknown option: %s",argi);
                throw new IllegalArgumentException(msg);
            }
            Option option = opt.get();
            if (!option.actions.contains(action)) {
                String msg = String.format("unknown option %s for %s",argi,action);
                throw new IllegalArgumentException(msg);
            }
            String value = "";
            if (option.hasString) {
                ++i;
                value = args[i];
                if (i == last) {
                    String msg = String.format("no value for option %s",argi);
                    throw new IllegalArgumentException(msg);
                }
            }
            result.put(option, value);
        }
        return result;
    }
    
    public static void print(Action action) {
        Stream.of(values())
                .filter(opt->opt.actions.contains(action))
                .forEach(opt->System.err.format("    --%-16s %s%n", opt, opt.msg));
    }
    
    private static final int VERSION = 0;
    private static final int RELEASE = 2;
    private static final String RUST = "82";
    private static final int BUILD = 0;
    
    public static String version() {
        return String.format("%d.%d.%s.%d", VERSION, RELEASE, RUST, BUILD);
    }
    
}
