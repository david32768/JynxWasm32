package parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static parse.Reason.M100;

import static parse.Reason.M200;

import wasm.Instruction;
import wasm.OpCode;

public class LocalFunction implements WasmFunction {

    private List<Instruction> insts;
    private final FnType fntype;

    private Local[] locals;
    private KindName kindName;
    private boolean found;
    private BitSet initvars;

    public LocalFunction(FnType fntype, KindName kindName) {        // PlaceHolder
        this.fntype = fntype;
        this.kindName = kindName;
        this.found = false;
    }

    public List<Instruction> getInsts() {
        return insts;
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    public Local[] getLocals() {
        return locals.clone();
    }

    @Override
    public boolean hasCode() {
        return found;
    }

    private static BitSet setVarsToInit(Local[] locals) {
        BitSet result = new BitSet(locals.length);
        result.set(0, locals.length);
        for (int i = 0; i < locals.length ; ++i) {
            Local local = locals[i];
            if (local.isParm()) {
                result.clear(i);
            } else break;
        }
        return result;
    }
    
    public BitSet getVarsToInit() {
        return (BitSet)initvars.clone();
    }
    
    private void setLocalFunction(int fnnum, Local[] locals, BitSet initvars,
            List<Instruction> insts, FnType fntype) {
        this.locals = locals;
        this.initvars = initvars;
        this.insts = Collections.unmodifiableList(insts);
        if (fnnum != kindName.getNumber()) {
            String msg = String.format("set fnnum %d is different from original %d",fnnum,kindName.getNumber());
            throw new IllegalStateException(msg);
        }
        if (!this.fntype.equals(fntype)) {
            String msg = String.format("set fntype %s is different from original %s",fntype,this.fntype);
            throw new IllegalStateException(msg);
        }
        this.found = true;
    }

    @Override
    public Local getLocal(int index) {
        return locals[index];
    }

    @Override
    public void exportNames(String module_name,String field_name) {
        kindName = kindName.exportNames(module_name, field_name);
    }

    @Override
    public FnType getFnType() {
        return fntype;
    }

    @Override
    public void setName(String name) {
        kindName = kindName.changeNames(kindName.getModuleName(), name);
    }

    private static ArrayList<Instruction> getInsts(TypeStack ts, String fnname) {
        Section code = ts.getCode();
        ArrayList<Instruction> insts = new ArrayList<>();
        InstructionChecker checker = new InstructionChecker(ts, fnname);
        while (code.hasRemaining()) {
            Op op = code.getop();
            try {
                Instruction inst = checker.from(op);
                insts.add(inst);
            } catch (Exception ex) {
                Logger.getGlobal().info(ex.toString());
                Logger.getGlobal().info(printInsts(insts, fnname, ts.FnType()));
                Logger.getGlobal().log(Level.INFO, String.format("failed inst = %s%n",op.getOpCode()), ex);
                throw ex;
            }
        }
        OpCode lastop = insts.get(insts.size() - 1).getOpCode();
        if (lastop != OpCode.END) {
            // "END opcode expected"
            throw new ParseException(M200,"lastop = %s",lastop);
        }
        return insts;
    }

    @Override
    public String toString() {
        return fntype.wasmString();
    }

    private static String printInsts(ArrayList<Instruction> insts, String fnname, FnType fnsig) {
        StringBuilder sb = new StringBuilder();
        ValueTypeStack vts = new ValueTypeStack();
        sb.append(String.format("// function %s %s%n",fnname,fnsig));
        for (Instruction inst : insts) {
            char[] spaces = new char[inst.getLevel() * 2];
            Arrays.fill(spaces, ' ');
            String spacer = String.valueOf(spaces);
            sb.append(String.format("%s%s // %s", spacer, inst, vts));
            FnType optype = inst.getFnType();
            vts.adjustStack(optype);
            sb.append(String.format("-> %s%n", vts));
        }
        return sb.toString();
    }
  
  /*
    ### Code section

    ID: `code`

    The code section contains a body for every function in the module.
    The count of function declared in the [function section](#function-section)
    and function bodies defined in this section must be the same and the `i`th
    declaration corresponds to the `i`th function body.

    | Field  | Type             | Description                                     |
    | ----- -| ---------------- | ----------------------------------------------- |
    | count  | `varuint32`      | count of function bodies to follow              |
    | bodies | `function_body*` | sequence of [Function Bodies](#function-bodies) |

     */
    public static void parse(WasmModule module, Section section) {
        ParseMethods.parseSectionVector(section, i->checkCount(module,i),LocalFunction::parseLocalFunction);
    }
    

    public static WasmModule checkCount(WasmModule module, Integer count) {
        int localfns = module.localfuns();
        if (!count.equals(localfns)) {
            // "function and code section have inconsistent lengths"
            throw new ParseException(M100,"local func count = %d code count = %d",localfns,count);
        }
        return module;
    }
    
    /*
    # Function Bodies

    Function bodies consist of a sequence of local variable declarations followed by 
    [bytecode instructions](Semantics.md). Each function body must end with the `end` opcode.

    | Field       | Type           | Description                               |
    | ----------- | -------------- | ----------------------------------------- |
    | body_size   | `varuint32`    | size of function body to follow, in bytes |
    | local_count | `varuint32`    | number of local entries                   |
    | locals      | `local_entry*` | local variables                           |
    | code        | `byte*`        | bytecode of the function                  |
    | end         | `byte`         | `0x0f`, indicating the end of the body    |

     */
    public static void parseLocalFunction(WasmModule module, Section section, int i) {
        int fnnum = module.getLocalFnIndex(i);
        LocalFunction localfn = (LocalFunction)module.atfuncidx(fnnum);
        Logger.getGlobal().fine(String.format("Function %d %s", fnnum,localfn.getName()));
        Section code = Section.getSubSection(section);
        FnType fnsig = localfn.getFnType();
        Local[] locals = Local.parse(code, localfn);
        BitSet initvars = setVarsToInit(locals);
        TypeStack ts = new TypeStack(fnsig, locals , code, module);
        ArrayList<Instruction> insts = getInsts(ts, localfn.getName());
        localfn.setLocalFunction(fnnum, locals, initvars, insts, fnsig);
        Logger.getGlobal().fine(String.format("function body %d has %d locals and %d insts",
                i, locals.length,insts.size()));
    }
    
}
