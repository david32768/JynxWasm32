package parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasm.ControlInstruction;
import wasm.Instruction;
import wasm.OpCode;
import wasm.UnreachableInstruction;

public class LocalFunction implements WasmFunction {

    private ArrayList<Local> locals;
    private ArrayList<Instruction> insts;
    private final FnType fntype;
    private KindName kindName;
    
    public LocalFunction(FnType fntype, KindName kindName) {        // PlaceHolder
        this.fntype = fntype;
        this.kindName = kindName;
    }

    public ArrayList<Instruction> getInsts() {
        return insts;
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    public ArrayList<Local> getLocals() {
        return locals;
    }

    private void setLocalFunction(int fnnum, ArrayList<Local> locals,
            ArrayList<Instruction> insts, FnType fntype) { //, int maxstacksz) {
        this.locals = locals;
        this.insts = insts;
        if (fnnum != kindName.getNumber()) {
            String msg = String.format("set fnnum %d is differnet from original %d",fnnum,kindName.getNumber());
            throw new IllegalStateException(msg);
        }
        if (!this.fntype.equals(fntype)) {
            String msg = String.format("set fntype %s is differnet from original %s",fntype,this.fntype);
            throw new IllegalStateException(msg);
        }
    }

    public Local getLocal(int index) {
        return locals.get(index);
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

    private static void logUnreachable(String fnname, OpCode opcode) {
        if (opcode == OpCode.UNREACHABLE) {
            Logger.getGlobal().info(String.format("%s: unreachable instruction (%s) dropped",
                fnname,opcode.name()));
        } else {
            Logger.getGlobal().warning(String.format("%s: unreachable instruction (%s) dropped",
                fnname,opcode.name()));
        }
    }
    
    private static ArrayList<Instruction> getInsts(TypeStack ts, String fnname, FnType fnsig) {
        Section code = ts.getCode();
        ArrayList<Instruction> insts = new ArrayList<>();
        boolean unreachable = false;
        int unreachablelevel = 0;
        while (code.hasRemaining()) {
            Op op = code.getop();
            OpCode opcode = op.getOpCode();
            Instruction comment = UnreachableInstruction.unreachable(op, ts);
            try {
                switch(opcode) {
                    case END :
                        if (unreachable) {
                            if (unreachablelevel != 0) {
                                --unreachablelevel;
                                insts.add(comment);
                                logUnreachable(fnname, opcode);
                                continue;
                            }
                        } else {
                            ts.updateFallThroughToEnd();
                        }
                        break;
                    case ELSE:
                        if (unreachable) {
                            if (unreachablelevel == 0) {
                                break;
                            }
                            insts.add(comment);
                            logUnreachable(fnname, opcode);
                            continue;
                        } else {
                            ts.updateFallThroughToEnd();
                        }
                        break;
                    case UNREACHABLE:
                        if (unreachable) {
                                insts.add(comment);
                                logUnreachable(fnname, opcode);
                            continue;
                        }
                        break;
                    case BLOCK: case IF: case LOOP:
                        if (unreachable) {
                            ++unreachablelevel;
                                insts.add(comment);
                                logUnreachable(fnname, opcode);
                            continue;
                        }
                        break;
                    case RETURN:    
                    default:
                        if (unreachable) {
                                insts.add(comment);
                                logUnreachable(fnname, opcode);
                            continue;
                        }
                        break;
                }
                Instruction inst = op.getInstruction(ts);
                unreachable = opcode.isTransfer();
                unreachablelevel = 0;
                inst = ts.changeControl(inst);
                unreachable |= opcode == OpCode.END && !((ControlInstruction)inst).getBlock().isEndReachable();
                insts.add(inst);
            } catch (Exception ex) {
                Logger.getGlobal().severe(ex.toString());

                Logger.getGlobal().severe(printInsts(insts,fnname,fnsig));

                Logger.getGlobal().log(Level.SEVERE, String.format("failed inst = %s%n",opcode), ex);
                throw ex;
            }
        }
        return insts;
    }

    @Override
    public String toString() {
        return fntype.toString();
    }

    public int getMaxLocals() {
        int localmax = locals.isEmpty()?0
                :locals.get(locals.size() - 1).getNextJvmnum();// .getJvmnum() + 2; // in case last local was double or long;
        return localmax;
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

| Field | Type | Description |
| ----- | ---- | ----------- |
| count | `varuint32` | count of function bodies to follow |
| bodies | `function_body*` | sequence of [Function Bodies](#function-bodies) |

    # Function Bodies

Function bodies consist of a sequence of local variable declarations followed by 
[bytecode instructions](Semantics.md). Each function body must end with the `end` opcode.

| Field | Type | Description |
| ----- | ---- | ----------- |
| body_size | `varuint32` | size of function body to follow, in bytes |
| local_count | `varuint32` | number of local entries |
| locals | `local_entry*` | local variables |
| code | `byte*` | bytecode of the function |
| end | `byte` | `0x0f`, indicating the end of the body |

     */
    public static void parse(WasmModule module, Section section) {
        int bodies = section.vecsz();
        for (int i = 0; i < bodies; i++) {
            int fnnum = module.getLocalFnIndex(i);
            LocalFunction localfn = (LocalFunction)module.atfuncidx(fnnum);
            Logger.getGlobal().fine(String.format("Function %d %s", fnnum,localfn.getName()));
            Section code = Section.getSubSection(section);
            FnType fnsig = localfn.getFnType(); //module.getSignature(i);
            ArrayList<Local> locals = Local.parse(code, fnsig);
            TypeStack ts = new TypeStack(fnsig, locals,code,module);
            ArrayList<Instruction> insts = getInsts(ts,localfn.getName(),fnsig);
            OpCode lastop = insts.get(insts.size() - 1).getOpCode();
            if (lastop != OpCode.END) {
                Logger.getGlobal().severe(String.format("Code does not end with END%n"));
            }
            Op op = ControlOp.getInstance(OpCode.RETURN, null);
            Instruction inst = op.getInstruction(ts);
            inst = ts.changeControl(inst);
            insts.add(inst);
            localfn.setLocalFunction(fnnum, locals, insts, fnsig);//, ts.getMaxsize());
            Logger.getGlobal().fine(String.format("function body %d has %d locals and %d insts", i, locals.size(),insts.size()));
        }
    }
}
