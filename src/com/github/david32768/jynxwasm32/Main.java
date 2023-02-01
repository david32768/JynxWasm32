package com.github.david32768.jynxwasm32;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import jynxwasm32.JavaName;
import jynxwasm32.JynxModule;
import parse.WasmModule;
import util.BasicFormatter;

public class Main {

    private enum Option {
        DEBUG("changes log-level to FINEST"),
        CLASS_NAME_AS_IS("stops changing first character of class name to upper case"),
        COMMENT("add wasm ops as comments to Jynx output"),
        NAME("class_name ; default name is module-name else filename without the .wasm extension"),
        PACKAGE("add package name"),
        ;
        
        private final String msg;

        private Option(String msg) {
            this.msg = msg;
        }

        private static Optional<Option> getInstance(String arg) {
            return Stream.of(values())
                    .filter(opt -> arg.equals("--" + opt.name()))
                    .findAny();
        }
    }
    
    private static void usage() {
        System.err.println("\nUsage: {options} wasm-file\n");
        System.err.println("Options are:\n");
        for (Option opt: Option.values()) {
            System.err.format("--%s %s%n", opt,opt.msg);
        }
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception {
        Logger root = Logger.getGlobal();
        root.setUseParentHandlers(false);
        Handler ha = new ConsoleHandler();
        ha.setFormatter(new BasicFormatter());
        root.addHandler(ha);

        if (args.length < 1) {
            usage();
            return;
        }

        String file = args[args.length - 1];
        if (!file.endsWith(".wasm")) {
            System.err.format("file suffix is not .wasm - '%s'%n", file);
            System.exit(1);
            return;
        }

        Path path = Paths.get(file);
        String fname = path.getFileName().toString();
        fname = fname.substring(0, fname.length() - 5);

        JavaName javaname = new JavaName(true);
        boolean comments = false;
        Optional<String> name = Optional.empty();
        String pkg = null;
        for(int i = 0; i < args.length - 1; ++i) {
            String argi = args[i].toUpperCase().replace('_', '-');
            Optional<Option> opt = Option.getInstance(argi);
            if (!opt.isPresent()) {
                usage();
                System.exit(1);
            }
            switch(opt.get()) {
                case DEBUG:
                    ha.setLevel(Level.FINEST);
                    ha.setFormatter(new SimpleFormatter());
                    root.setLevel(Level.FINEST);
                    break;
                case CLASS_NAME_AS_IS:
                    javaname = new JavaName(false);
                    break;
                case COMMENT:
                    comments = true;
                    break;
                case NAME:
                    if (i < args.length - 2) {
                        ++i;
                        name = Optional.of(args[i]);
                    }
                    break;
                case PACKAGE:
                    if (i < args.length - 2) {
                        ++i;
                        pkg = args[i];
                    }
                    break;
                default:
                    System.err.format("unknown option '%s'%n",argi);
                    usage();
                    return;
            }
        }

        if (name.isPresent() && !javaname.isClassName(name.get())) {
            System.err.format("%s is not a valid Java class name%n", name.get());
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
        String classname = name.orElse(javaname.ownerName(module.getName()));
        if (pkg != null) {
            classname = pkg + '/' + classname;
        }
        JynxModule.output(module,file,classname,javaname,comments);

    }
    
}
