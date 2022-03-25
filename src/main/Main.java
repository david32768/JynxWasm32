package main;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import jynx.JynxModule;
import parse.WasmModule;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: Parse wasm-file");
            System.exit(0);
        }
        String file = args[0];
        if (!file.endsWith(".wasm")) {
            System.err.format("file suffix is not .wasm - %s%n", file);
            System.exit(1);
        }

        Path path = Paths.get(file);
        String name = path.getFileName().toString();
        name = name.substring(0, name.length() - 5);

        ByteBuffer stream = ByteBuffer.wrap(Files.readAllBytes(path));
        Logger.getGlobal().setLevel(Level.WARNING);
        WasmModule module = WasmModule.getModule(name,stream);

        JynxModule.output(module,file);

    }
    
}
