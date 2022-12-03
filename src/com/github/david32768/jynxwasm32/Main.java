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
import jynxwasm32.JavaName;
import jynxwasm32.JynxModule;
import parse.WasmModule;
import util.BasicFormatter;

public class Main {

    private static void usage() {
        System.err.println("\nUsage: {options} wasm-file\n");
        System.err.println("Options are:\n");
        System.err.println("  --DEBUG changes log-level to FINEST");
        System.err.println("  --NON_STANDARD stops changing first character of class name to upper case");
        System.err.println("  --COMMENT add wasm ops as comments to Jynx output");
        System.err.println("  --NAME class_name ; default name is module-name else filename without the .wasm extension");
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
        for(int i = 0; i < args.length - 1; ++i) {
            String argi = args[i].toUpperCase().replace('_', '-');
            switch(argi) {
                case "--DEBUG":
                    ha.setLevel(Level.FINEST);
                    ha.setFormatter(new SimpleFormatter());
                    root.setLevel(Level.FINEST);
                    break;
                case "--NON-STANDARD":
                    javaname = new JavaName(false);
                    break;
                case "--COMMENT":
                    comments = true;
                    break;
                case "--NAME":
                    if (i < args.length - 2) {
                        ++i;
                        name = Optional.of(args[i]);
                    }
                    break;
                default:
                    System.err.format("unknown option '%s'%n",argi);
                    usage();
                    return;
            }
        }

        if (name.isPresent() && !javaname.is(name.get())) {
            System.err.format("%s is not a valid Java class name%n", name.get());
            usage();
            return;
        }

        ByteBuffer stream = ByteBuffer.wrap(Files.readAllBytes(path));
        WasmModule module = WasmModule.getModule(fname,stream);
        String classname = name.orElse(javaname.ownerName(module.getName()));

        JynxModule.output(module,file,classname,javaname,comments);

    }
    
}
