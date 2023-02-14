package com.github.david32768.jynxwasm32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Optional;
import java.util.stream.Stream;

import jynxwasm32.JavaName;
import jynxwasm32.JynxModule;
import parse.WasmModule;
import util.BasicFormatter;
import utility.Binary;

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
    
    private static final String VERSION = "0.1.1";
    
    private static void usage() {
        System.err.format("\nUsage: (version %s)\n",VERSION);
        System.err.println("    2jynx {options} wasm-file");
        System.err.println("        convert to a jynx file\n");
        System.err.println("    Options are:\n");
        for (Option opt: Option.values()) {
            System.err.format("        --%s %s%n", opt,opt.msg);
        }
        System.err.println("\n    testsuite [--LEVEL <log-level>] wast-file");
        System.err.println("        run w3c-1.0 testsuite file that contains 'module binary'\n");
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

        if (args[0].equals("testsuite")) {
            args = Arrays.copyOfRange(args, 1, args.length);
            testSuite(args);
            return;
        } else if (args[0].equals("2jynx")) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        
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
    
    private static void testSuite(String[] args)  throws IOException {
        int argct = args.length;
        if (argct == 0) {
            usage();
            System.exit(0);
        }
        Level loglevel = Level.WARNING;
        String file = args[0];
        switch(argct) {
            case 3:
                if (args[0].toUpperCase().equals("--LEVEL")) {
                    try {
                        loglevel = Level.parse(args[1].toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        System.err.println();
                        System.err.println(ex.toString());
                        usage();
                        System.exit(1);
                    }
                    file = args[2];
                } else {
                    usage();
                    System.exit(1);
                }
                break;
            case 1:
                break;
            default:
                usage();
                System.exit(1);
        }
        Logger.getGlobal().setLevel(loglevel);
        Binary.testFile(Paths.get(file));
    }
    
}
