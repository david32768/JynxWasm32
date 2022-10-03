package com.github.david32768.jynxwasm32;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import jynx.JavaName;

import jynx.JynxModule;
import parse.WasmModule;

public class Main {

    private static void usage() {
        System.err.println("Usage: [level=log-level] [env=main-java-class] [name=class-name] wasm-file");
                String file;
        System.err.println("  default log-level is INFO");
        System.err.println("  default env is 'Env'");
        System.err.println("  default class name is filename without the .wasm extension");
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length <1 || args.length > 3) {
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
        String name = path.getFileName().toString();
        name = name.substring(0, name.length() - 5);

        String env = "Env";
        for(int i = 0; i < args.length - 1; ++i) {
            String arg = args[i];
            int index = arg.indexOf('=');
            if (index < 0) {
                System.err.format("'%s' does not contain '='%n", arg);
                usage();
                return;
            }
            String option = arg.substring(0,index);
            String parm = arg.substring(index +1);
            switch(option) {
                case "level":
                    try {
                        Level level = Level.parse(parm.toUpperCase());
                        Logger.getGlobal().setLevel(level);
                    } catch (IllegalArgumentException ex) {
                        System.err.println(ex.getMessage());
                        usage();
                        return;
                    }
                    break;
                case "env":
                    env = parm;
                    break;
                case "name":
                    if (!JavaName.is(parm)) {
                        System.err.format("%s is not a valid Java class name%n", parm);
                        usage();
                        return;
                    }
                    name = parm;
                    break;
                default:
                    System.err.format("unknown option '%s'%n",option);
                    usage();
                    return;
            }
        }

        ByteBuffer stream = ByteBuffer.wrap(Files.readAllBytes(path));
        WasmModule module = WasmModule.getModule(name,stream);

        JynxModule.output(module,file,env);

    }
    
}
