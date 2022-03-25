package jynx;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import parse.Data_segment;
import parse.FnType;
import parse.Global;
import parse.Kind;
import parse.KindName;

import parse.Limits;
import parse.LocalFunction;
import parse.Memory;
import parse.Table;
import parse.WasmFunction;
import parse.WasmModule;
import wasm.OpCode;

public class JynxModule {
    
    private final WasmModule module;
    private final PrintWriter pw;
    private final String moduleName;

    private JynxModule(WasmModule module, PrintWriter pw) {
        this.module = module;
        this.pw = pw;
        this.moduleName = module.getName();
    }
    
    
    public static void output(WasmModule module, String file) throws IOException {
        file = file.substring(0, file.length() - 5) + ".jx";
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(Paths.get(file)))) {
            JynxModule jm = new JynxModule(module, pw);
            jm.print();
        }
    }

    private void print() {
        pw.format(".version V1_8 WARN_INDENT GENERATE_LINE_NUMBERS CHECK_METHOD_REFERENCES PREPEND_CLASSNAME%n");
        pw.format(".class public %s%n",module.getName());
        pw.format(".super java/lang/Object%n");
        WasmFunction start = module.getStart();
        if (start != null) {
            pw.format("; start function = %s%n",start.getName());
        }
        pw.println();
        if (module.getTables().size() > 1) {
            throw new UnsupportedOperationException();  // method name for table should include table number
        }
        for (Table table: module.getTables()) {
            pw.format(".field private final static %s Lwasmrun/Table;%n",
                table.getDefaultName());
            if (table.isExported()) {
                pw.format(".field public final static %s Lwasmrun/Table;%n",
                    javaSimpleName(table));
            }
        }
        if (module.getMemories().size() > 1) {
            throw new UnsupportedOperationException();  // method name for memory should include table number
        }
        for (Memory memory: module.getMemories()) {
            if (memory.getMemoryNum() == 0 && !memory.getKindName().getFieldName().equals("memory")) {
                Logger.getGlobal().warning("memory 0 is not exported as 'memory' so cannot use wasi");
            }
            pw.format(".field private final static %s Lwasmrun/Storage;%n",
                memory.getDefaultName());
            if (memory.isExported()) {
                pw.format(".field public final static %s Lwasmrun/Storage;%n",
                    javaSimpleName(memory));
            }
        }
        for (Global global:module.getGlobals()) {
            defineGlobalField(global);
        }
        pw.println();
        pw.println(".method static <clinit>()V");
        pw.println("; initialise own globals");
        String spacer = "  ";
        for (Global global:module.getGlobals()) {
            initGlobalField(global);
        }
        pw.println("; initialise tables");
        for (Table table:module.getTables()) {
            pw.print(spacer);
            pw.format("%s __init%s()V%n",OpCode.CALL,table.getDefaultName());
        }
        pw.println("; initialise memories");
        for (Memory memory:module.getMemories()) {
            pw.print(spacer);
            pw.format("%s __init%s()V%n",OpCode.CALL,memory.getDefaultName());
        }
        pw.print(spacer);
        pw.println(OpCode.RETURN);
        pw.println(".end_method");
        int tablenum = 0;
        for (Table table:module.getTables()) {
            outputTableMethod(table,tablenum);
            ++tablenum;
        }
        int memnum = 0;
        for (Memory memory:module.getMemories()) {
            outputMemory(memory,memnum);
            ++memnum;
        }
        for (WasmFunction function:module.getFunctions()) {
            if (function instanceof LocalFunction) {
                LocalFunction localfn = (LocalFunction)function;
                pw.println();
                JynxFunction.printJVMInsts(module, localfn, pw);
            }
            pw.flush();
        }
        pw.flush();
    }

    private void defineGlobalField(Global global) {
        if (!global.isImported()) {
            assert moduleName.equals(global.getModuleName());
            String name = javaSimpleName(global);
            String access = global.isPrivate()?"private":"public";
            pw.format(".field %s static %s %c ;  = %s%n",
                    access,name,global.getType().getJvmtype(),global.getValue());
        }
    }

    private void initGlobalField(Global global) {
        if (!global.isImported()) {
            JynxFunction jynx = new JynxFunction(pw);
            jynx.printInst(global.getConstinst(), "", "");
            String name = JynxModule.javaSimpleName(global);
            pw.format("  %s %s %c%n",OpCode.GLOBAL_SET,name,global.getType().getJvmtype());
        }
    }

    private static int MAX_SEGMENT = (2*Short.MAX_VALUE + 1)*3/4 - 2;
    
    private void outputMemory(Memory memory, int num) {
        pw.println();
        pw.format(".method private static __init%s()V%n",memory.getDefaultName());
        String spacer = "  ";
        Limits limits = memory.getLimits();
        int maximum = limits.hasMaximum()?limits.getMaximum():0;
        if (memory.isImported()) {
            pw.print(spacer);
            String name = javaName(memory);
            String type = "Lwasmrun/Storage;";
            pw.format("%s %s %s%n",OpCode.GLOBAL_GET,name,type);
            pw.print(spacer);
            pw.format("%s %s Lwasmrun/Storage;%n",OpCode.GLOBAL_SET,memory.getDefaultName());
            pw.print(spacer);
            pw.format("MEMORY_CHECK %d %d %d%n",limits.getInitial(),maximum,num);
            pw.print(spacer);
            pw.format("%s%n",OpCode.RETURN);
            pw.println(".end_method");
            return;
        } else {
            pw.print(spacer);
            pw.format("MEMORY_NEW %d %d%n",limits.getInitial(),maximum);
        }
        pw.print(spacer);
        pw.format("%s %s Lwasmrun/Storage;%n",OpCode.GLOBAL_SET,memory.getDefaultName());
        for (Data_segment ds:memory.getData()) {
            byte[] data = ds.getData();
            final int memoffset = ds.getOffset();
            int dataoffset = 0;
            int remaining = data.length;
            byte[] part = new byte[MAX_SEGMENT];
            assert MAX_SEGMENT > 0;
            while (remaining > MAX_SEGMENT) {
                System.arraycopy(data, dataoffset, part, 0, MAX_SEGMENT);
                printDataSegment(part, memoffset,dataoffset);
                remaining -= MAX_SEGMENT;
                dataoffset += MAX_SEGMENT;
            }
            part = new byte[remaining];
            System.arraycopy(data, dataoffset, part, 0, remaining);
            printDataSegment(part, memoffset,dataoffset);
        }
        if (memory.isExported()) {
            pw.print(spacer);
            pw.format("COPY_MEMORY %s %s %n",memory.getDefaultName(),javaSimpleName(memory));
        }
        pw.print(spacer);
        pw.format("%s%n",OpCode.RETURN);
        pw.println(".end_method");
    }

    private void printDataSegment(byte[] data, int memoffset, int dataoffset) {
        Base64.Encoder encoder = Base64.getEncoder();
        String datastr = encoder.encodeToString(data);
        String spacer = "  ";
        pw.print(spacer);
        pw.format("ADD_SEGMENT %d %d \"%s\"%n",memoffset,dataoffset,datastr);
    }
    
    private static final int MAXPARM = 32;
    
    private void outputTableMethod(Table table, int num) {
        pw.println();
        String spacer = "  ";
        pw.format(".method private static __init%s()V%n",table.getDefaultName());
        if (table.isImported()) {
            pw.print(spacer);
            pw.format("COPY_TABLE %s %s%n",javaName(table),table.getDefaultName());
            pw.print(spacer);
            pw.format("%s%n",OpCode.RETURN);
            pw.println(".end_method");
            return;
        }
        pw.print(spacer);
        pw.format("TABLE_NEW%n");
        pw.print(spacer);
        pw.format("TABLE_TEE %d%n",table.getTableNum());
        boolean started = false;
        int added = 0;
        int i = 0;
        for (WasmFunction fn:table.getElements()) {
            if (fn == null) {
                if (started) {
                    pw.print(spacer);
                    pw.println(".end_array");
                    started = false;
                    added = 0;
                }
            } else {
                if (!started) {
                    started = true;
                    pw.print(spacer);
                    pw.format("ADD_ENTRY %d .array ; start_index%n",i);
                }
                FnType fntype = fn.getFnType();
                String name = javaLocalName(fn);
                String method = String.format("ST:%s%s",name,fntype.jvmString() );
                pw.print(spacer);
                pw.print(spacer);
                pw.format("%s ; %d%n",method,i);
                ++added;
                if (added%MAXPARM == MAXPARM - 1) {
                    pw.print(spacer);
                    pw.println(".end_array");
                    started = false;
                    added = 0;
                }
            }
            ++i;
        }
        if (started) {
            pw.print(spacer);
            pw.println(".end_array");
        }
        pw.print(spacer);
        pw.format("%s%n",OpCode.DROP);
        if (table.isExported()) {
            pw.print(spacer);
            pw.format("COPY_TABLE %s %s %n",table.getDefaultName(),javaSimpleName(table));
        }
        pw.print(spacer);
        pw.format("%s%n",OpCode.RETURN);
        pw.println(".end_method");
    }
    
    private static String changeInvalidChar(Integer I) {
        int i = I;
        if (Character.isJavaIdentifierPart(i)) {
            return String.valueOf((char)i);
        } else {
            return (i == '.'?"$DOT$":"$" + Integer.toHexString(i) + "$");
        }
    }
    
    private static String javaSimpleName(String name) {
        if (!Character.isJavaIdentifierStart(name.codePointAt(0))) {
            name = "_"+ name;
        }
        return name
                .codePoints()
                .boxed()
                .map(JynxModule::changeInvalidChar)
                .collect(Collectors.joining());
    }

    private static String addPackage(String pkg, String name) {
        assert name.startsWith(pkg);
        return pkg + "/" + name.substring(0,1).toUpperCase() + name.substring(1);
    }
    
    public static String javaName(Kind kind) {
        String module = javaSimpleName(kind.getModuleName());
        String field = javaSimpleName(kind.getFieldName());
        String name = module + "/" + field;
        if (module.startsWith("env")) {
            name = addPackage("env",name);
        } else if (module.startsWith("wasi")) {
            name = addPackage("wasi",name);
        } else if (module.startsWith("spectest")) {
            name = addPackage("spectest/",name);
        }
        return name;
    }

    public static String javaSimpleName(Kind kind) {
        return javaSimpleName(kind.getFieldName());
    }
    
    public static String javaLocalName(Kind kind) {
        if (kind.isImported()) {
            return javaName(kind);
        } else {
            return javaSimpleName(kind);
        }
    }
}
