package jynxwasm32;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import static parse.ValueType.V00;

import parse.Data_segment;
import parse.FnType;
import parse.Global;
import parse.Limits;
import parse.LocalFunction;
import parse.Memory;
import parse.Table;
import parse.TableElement;
import parse.ValueType;
import parse.WasmFunction;
import parse.WasmModule;
import wasm.Instruction;
import wasm.OpCode;

public class JynxModule {
    
    private final WasmModule module;
    private final PrintWriter pw;
    private final String className;
    private final String fileName;
    private final JavaName javaName;
    private final boolean comments;
    private final String startMethod;

    private JynxModule(WasmModule module, PrintWriter pw,
            String filename, String classname,  JavaName javaname,
            String startmethod, boolean comments) {
        this.module = module;
        this.pw = pw;
        this.javaName = javaname;
        this.className = classname;
        this.fileName = filename;
        this.comments = comments;
        this.startMethod = startmethod;
    }
    
    public static void output(WasmModule module, String file, 
            String classname, JavaName javaname, String startmethod, boolean comments) throws IOException {
        try (PrintWriter pw = new PrintWriter(System.out)) {
            JynxModule jm = new JynxModule(module, pw,file,classname,javaname,startmethod, comments);
            jm.print();
        }
    }

    private void print() {
        WasmFunction start = module.getStart();
        if (start == null) {
            String startmethod = startMethod == null?"_start":startMethod;
            for (WasmFunction function:module.getFunctions()) {
                if (function instanceof LocalFunction) {
                    LocalFunction localfn = (LocalFunction)function;
                    String name = javaName.simpleName(localfn);
                    if (name.equals(startmethod)) {
                        start = localfn;
                        break;
                    }
                }
            }
            if (startMethod != null && start == null) {
                String msg = String.format("start method %s not found", startMethod);
                Logger.getGlobal().warning(msg);
            }
        }
        
        pw.format(".version V1_8 SYMBOLIC_LOCAL GENERATE_LINE_NUMBERS%n");
        pw.println(".macrolib wasm32MVP");
        Path srcpath = Paths.get(fileName);
        pw.format(".source %s%n",srcpath.getFileName());
        pw.format(".class public %s%n",className);
        pw.format(".super java/lang/Object%n");
        pw.println();
        if (module.getTables().size() > 1) {
            throw new UnsupportedOperationException();  // method name for table should include table number
        }
        for (Table table: module.getTables()) {
            pw.format(".field private final static %s Lwasmrun/Table;%n",
                table.getDefaultName());
            if (table.isExported()) {
                pw.format(".field public final static %s Lwasmrun/Table;%n",
                    javaName.simpleName(table));
            }
        }
        if (module.getMemories().size() > 1) {
            throw new UnsupportedOperationException();  // method name for memory should include table number
        }
        String storage = null;
        for (Memory memory: module.getMemories()) {
            if (memory.getMemoryNum() == 0 && !memory.getKindName().getFieldName().equals("memory")) {
                Logger.getGlobal().warning("memory 0 is not exported as 'memory' so cannot use wasi methods");
            }
            pw.format(".field private final static %s Lwasmrun/Storage;%n",
                memory.getDefaultName());
            if (memory.isExported()) {
                if (memory.getMemoryNum() == 0) {
                    storage = javaName.simpleName(memory);
                }
                pw.format(".field public final static %s Lwasmrun/Storage;%n",
                    javaName.simpleName(memory));
            }
        }
        for (Global global:module.getGlobals()) {
            defineGlobalField(global);
        }
        pw.println();

        pw.println(".method static <clinit>()V");
        pw.println("; initialise own globals");
        String spacer = "  ";
        pw.print(spacer);
        pw.format("%s __initGlobals()V%n",OpCode.CALL);
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
        outputGlobalFields(module.getGlobals());
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
                JynxFunction jynx = new JynxFunction(pw,javaName,comments);
                jynx.printJVMInsts(module, localfn);
            }
            pw.flush();
        }
        if (start != null) {
            JynxFunction jynx = new JynxFunction(pw,javaName,comments);
            jynx.printStart(start);
        }
        pw.flush();
        JynxFunction.printStats();
    }

    private void defineGlobalField(Global global) {
        if (!global.isImported()) {
            String name = javaName.simpleName(global);
            String access = global.isPrivate()?"private":"public";
            String mutable = global.isFinal()?" final":"";
            String init = global.isFinal() && !global.usesInitGlobal()?"":" ; ";
            pw.format(".field %s%s static %s %c %s= %s%n",
                    access,mutable,name,global.getType().getJvmtype(),init,global.getValue());
        }
    }

    private void outputGlobalFields(List<Global> globals) {
        JynxFunction jynx = new JynxFunction(pw,javaName,comments);
        pw.println();
        pw.format(".method private static __initGlobals()V%n");
        String spacer = "  ";
        for (Global global:globals) {
            if (global.isImported()) {
                continue;
            }
            if (global.usesInitGlobal() || !global.isFinal()) {
                jynx.printInst(global.getConstInst(), "", "");
                String name = javaName.simpleName(global);
                String prefix = getPrefix(global.getType());
                pw.format("  %s%s %s%n",prefix,OpCode.GLOBAL_SET,name);
            }
        }
        pw.print(spacer);
        pw.format("%s%n",OpCode.RETURN);
        pw.println(".end_method");
    }

    private static int MAX_SEGMENT = 720; // NOTEPAD will wrap long lines into 1024 character chunks
    
    private void outputMemory(Memory memory, int num) {
        JynxFunction jynx = new JynxFunction(pw,javaName,comments);
        pw.println();
        pw.format(".method private static __init%s()V%n",memory.getDefaultName());
        String spacer = "  ";
        Limits limits = memory.getLimits();
        int maximum = limits.hasMaximum()?limits.getMaximum():0;
        String type = "MEMORY";
        if (memory.isImported()) {
            pw.print(spacer);
            String name = javaName.of(memory);
            pw.format("%s_%s %s%n",type,OpCode.GLOBAL_GET,name);
            pw.print(spacer);
            pw.format("%s_%s %s%n",type,OpCode.GLOBAL_SET,memory.getDefaultName());
            pw.print(spacer);
            pw.format("%s_CHECK %d %d %d%n",type,limits.getInitial(),maximum,num);
        } else {
            pw.print(spacer);
            pw.format("%s_NEW %d %d%n",type,limits.getInitial(),maximum);
            pw.print(spacer);
            pw.format("%s_%s %s%n",type,OpCode.GLOBAL_SET,memory.getDefaultName());
            if (memory.isExported()) {
                pw.print(spacer);
                pw.format("%s_%s %s%n",type,OpCode.GLOBAL_GET,memory.getDefaultName());
                pw.print(spacer);
                pw.format("%s_%s %s%n",type,OpCode.GLOBAL_SET,javaName.simpleName(memory));
            }
        }
        for (Data_segment ds:memory.getData()) {
            byte[] data = ds.getData();
            Instruction constinst = ds.getOffsetInstruction();
            int dataoffset = 0;
            int remaining = data.length;
            byte[] part = new byte[MAX_SEGMENT];
            assert MAX_SEGMENT > 0;
            while (remaining > MAX_SEGMENT) {
                System.arraycopy(data, dataoffset, part, 0, MAX_SEGMENT);
                printDataSegment(jynx,num,constinst,part,dataoffset);
                remaining -= MAX_SEGMENT;
                dataoffset += MAX_SEGMENT;
            }
            part = new byte[remaining];
            System.arraycopy(data, dataoffset, part, 0, remaining);
            printDataSegment(jynx,num,constinst,part,dataoffset);
        }
        pw.print(spacer);
        pw.format("%s%n",OpCode.RETURN);
        pw.println(".end_method");
    }

    private void printDataSegment(JynxFunction jynx, int num, Instruction constinst, byte[] data, int dataoffset) {
        Base64.Encoder encoder = Base64.getEncoder();
        String datastr = encoder.encodeToString(data);
        String spacer = "  ";
        jynx.printInst(constinst, "", "");
        pw.print(spacer);
        pw.format("STRING_CONST \"%s\"%n", datastr);
        pw.print(spacer);
        pw.format("BASE64_STORE %d +%d%n",num,dataoffset);
    }
    
    private static final int MAXPARM = 32;
    
    private void outputTableMethod(Table table, int num) {
        JynxFunction jynx = new JynxFunction(pw,javaName,comments);
        pw.println();
        String type = "TABLE";
        String spacer = "  ";
        pw.format(".method private static __init%s()V%n",table.getDefaultName());
        if (table.isImported()) {
            pw.print(spacer);
            pw.format("%s_%s %s%n",type,OpCode.GLOBAL_GET,javaName.of(table));
            pw.print(spacer);
            pw.format("%s_%s %s%n",type,OpCode.GLOBAL_SET,table.getDefaultName());
            pw.print(spacer);
            pw.format("%s%n",OpCode.RETURN);
            pw.println(".end_method");
            return;
        }
        pw.print(spacer);
        pw.format("%s_NEW%n",type);
        pw.print(spacer);
        pw.format("%s_%s %s%n",type,OpCode.GLOBAL_SET,table.getDefaultName());
        pw.print(spacer);
        pw.format("%s_%s %s%n",type,OpCode.GLOBAL_GET,table.getDefaultName());

        for (TableElement element:table.getElements()) {
            printTableElement(jynx,element,spacer);
        }

        pw.print(spacer);
        pw.format("%s%n",OpCode.DROP);
        if (table.isExported()) {
            pw.print(spacer);
            pw.format("COPY_%s %s %s %n",type,table.getDefaultName(),javaName.simpleName(table));
        }
        pw.print(spacer);
        pw.format("%s%n",OpCode.RETURN);
        pw.println(".end_method");
    }

    private void printTableElement(JynxFunction jynx,TableElement element, String spacer) {
        boolean started = false;
        int added = 0;
        Instruction offsetinst = element.getConstExpr().getConstInst();
        int i = 0;
        for (WasmFunction fn:element.getFunctions()) {
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
                    jynx.printInst(offsetinst, "", "");
                    pw.print(spacer);
                    pw.format("ADD_ENTRY %d .array ; start_index%n",i);
                }
                FnType fntype = fn.getFnType();
                String name = javaName.localName(fn);
                String method = String.format("ST:%s%s",name,fntype.wasmString() );
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
    }
    
    public static String getPrefix(ValueType vt) {
        if (vt == V00) {
            return "";
        }
        return String.format("%s%d_",vt.isFixed()?'I':'F',vt.getStackSize()*32);
    }
    
}
