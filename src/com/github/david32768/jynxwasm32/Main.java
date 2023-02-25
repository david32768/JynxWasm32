package com.github.david32768.jynxwasm32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

import jynxwasm32.JavaName;
import jynxwasm32.JynxModule;
import main.Action;
import main.Option;
import parse.WasmModule;
import util.BasicFormatter;
import utility.Binary;

public class Main {

    private static final String VERSION = "0.1.4";
    
    private static void usage() {
        System.err.format("\nUsage: (version %s)\n",VERSION);
        for (Action action:Action.values()) {
            System.err.format("\n%s [options] %s-file\n\n",action,action.extension());
            System.err.println("  Options are:\n");
            Option.print(action);
        }
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception {
        Logger root = Logger.getGlobal();
        root.setUseParentHandlers(false);
        Handler ha = new ConsoleHandler();
        ha.setFormatter(new BasicFormatter());
        root.addHandler(ha);

        if (args.length < 2) {
            usage();
            return;
        }
        Optional<Action> optaction = Action.getInstance(args[0]);
        if (!optaction.isPresent()) {
            System.err.format("unknown action %s%n",args[0]);
            usage();
            return;
        }
        Action action = optaction.get();
        Map<Option,String> options = Option.getOptions(action, args);
        String lastarg = args[args.length - 1];
        if (!lastarg.endsWith("." + action.extension())) {
            System.err.format("file %s has invalid extension - expected .%s%n",lastarg,action.extension());
            usage();
            return;
        }
        Level loglevel = Level.WARNING;
        String level = options.get(Option.LEVEL);
        if (level != null) {
            try {
                loglevel = Level.parse(level.toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.err.println();
                System.err.println(ex.toString());
                usage();
                System.exit(1);
            }
        }
        root.setLevel(loglevel);
        ha.setLevel(loglevel);
        switch(action) {
            case _2JYNX:
                toJynx(options, lastarg);
                break;
            case _PARSE:
                parse(options, lastarg);
                break;
            case _TESTPARSE:
                Binary.testFile(Paths.get(lastarg));
                break;
            default:
                throw new EnumConstantNotPresentException(action.getClass(), action.name());
        }
    }
    
    private static void parse(Map<Option,String> options, String file)  throws IOException {
        Path path = Paths.get(file);
        String fname = path.getFileName().toString();
        fname = fname.substring(0, fname.length() - 5);
        ByteBuffer stream = ByteBuffer.wrap(Files.readAllBytes(path));
        WasmModule module = WasmModule.getModule(fname,stream);
    }
    
    private static void toJynx(Map<Option,String> options, String file)  throws IOException {
        Path path = Paths.get(file);
        String fname = path.getFileName().toString();
        fname = fname.substring(0, fname.length() - 5);

        JavaName javaname = new JavaName(!options.containsKey(Option.CLASS_NAME_AS_IS));
        boolean comments = options.get(Option.COMMENT) != null;
        String name = options.get(Option.NAME);
        String pkg = options.get(Option.PACKAGE);

        if (name != null  && !javaname.isClassName(name)) {
            System.err.format("%s is not a valid Java class name%n", name);
            usage();
            return;
        }

        if (pkg != null && !javaname.isPackageName(pkg)) {
            System.err.format("%s is not a valid Java package name%n", pkg);
            usage();
            return;
        }

        ByteBuffer stream = ByteBuffer.wrap(Files.readAllBytes(path));
        WasmModule module = WasmModule.getModule(fname,stream);
        if (name == null) {
            name = javaname.ownerName(module.getName());
        }
        if (pkg != null) {
            name = pkg + '/' + name;
        }
        JynxModule.output(module,file,name,javaname,comments);

    }
    
}
