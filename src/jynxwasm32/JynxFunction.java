package jynxwasm32;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static parse.ValueType.*;
import static wasm.OpCode.MEMORY_COPY;

import parse.BranchTarget;
import parse.FnType;
import parse.Global;
import parse.Local;
import parse.LocalFunction;
import parse.Table;
import parse.ValueType;
import parse.ValueTypeStack;
import parse.WasmFunction;
import parse.WasmModule;
import wasm.BrTableInstruction;
import wasm.BranchInstruction;
import wasm.ControlInstruction;
import wasm.Instruction;
import wasm.MemoryInstruction;
import wasm.ObjectInstruction;
import wasm.OpCode;
import wasm.OpType;
import wasm.SimpleInstruction;
import wasm.UnreachableInstruction;

public class JynxFunction {
    
    public enum JvmOp {
        lookupswitch,
        ;
    }

    private final static Map<OpCode,Integer> OPTS = new HashMap<>();
    
    private static int ct = 0;
    
    public static void printStats() {
        int changect = 0;
        for (Map.Entry<OpCode,Integer> me:OPTS.entrySet()) {
            String msg = String.format("%s optimised = %d",me.getKey(),me.getValue());
            Logger.getGlobal().info(msg);
            changect += me.getValue();
        }
        String msg = String.format("total optimised = %d",changect);
        Logger.getGlobal().info(msg);
        msg = String.format("total instructions = %d",ct);
        Logger.getGlobal().info(msg);
    }
    
    private final PrintWriter pw;
    private final JavaName javaName;
    private final boolean comments;

    public JynxFunction(PrintWriter pw, JavaName javaName, boolean comments) {
        this.pw = pw;
        this.javaName = javaName;
        this.comments = comments;
    }

    public void printStart(WasmFunction start) {
        pw.format("; start function = %s%n",start.getFieldName());
        pw.println(".method public static START()V");
        pw.format("  %s %s()->()%n",OpCode.CALL,javaName.localName(start));
        pw.format("  %s%n",OpCode.RETURN);
        pw.println(".end_method");
    }

    public void printJVMInsts(WasmModule module, LocalFunction fn) {
        String jvmname = javaName.simpleName(fn);
        String from = fn.getFieldName().equals(jvmname)?"":" ; " + fn.getFieldName();
        String access = fn.isPrivate()?"private":"public";
        pw.format(".method %s static %s%s%s%n",access,jvmname,fn.getFnType().wasmString(),from);
        int maxlocal = 0;
        for (Local local:fn.getLocals()) {
            ValueType vt = local.getType();
            ++maxlocal;
            if (vt == I64 || vt == F64) {
                ++maxlocal;
            }
            if (local.isParm()) {
                String name = local.getDebugName();
                if (name != null) {
                    pw.format(".parameter %d final %s%n",local.getNumber(),name);
                }
                continue;
            }
            OpCode opcode;
            switch(vt) {
                case I32:
                    opcode = OpCode.I32_CONST;
                    break;
                case I64:
                    opcode = OpCode.I64_CONST;
                    break;
                case F32:
                    opcode = OpCode.F32_CONST;
                    break;
                case F64:
                    opcode = OpCode.F64_CONST;
                    break;
                default:
                    throw new AssertionError();
            }
           pw.format("  %s 0%n",opcode);
           pw.format("  %s %s%n",OpCode.LOCAL_SET,local.getName());
        }
        int maxstack = printInsts(fn.getInsts(), fn.getFieldName());
            // + 2 to allow use of one temp variable when generating jynx which may be double or long
        maxlocal += 2;
        pw.format(".limit locals %d%n",maxlocal);
        pw.format(".limit stack %d%n",maxstack);
            // + 4 for ExtendedOps
        pw.println(".end_method");
        pw.flush();
    }

    public static String num2string(Number num) {
        String numstr;
        if (num instanceof Long) {
            numstr = ((Long) num).toString() + 'L';
        } else if (num  instanceof Float) {
            float fval = num.floatValue();
            if (Float.isNaN(fval)) {
                int rawf = Float.floatToRawIntBits(fval);
                numstr = rawf < 0?"-":"";
                numstr += "nan:" + Integer.toHexString(rawf & ~0xff800000);
            } else {
                numstr = Float.toHexString(fval) + 'F';
            }
        } else if (num  instanceof Double) {
            double dval = num.doubleValue();
            if (Double.isNaN(dval)) {
                long rawf = Double.doubleToRawLongBits(num.doubleValue());
                numstr = rawf < 0?"-":"";
                numstr += "nan:" + Long.toHexString(rawf & ~0xfff0000000000000L);
            } else {
                numstr = Double.toHexString(dval);
            }
        } else {
            numstr = num.toString();
        }
        return numstr;
    }

    private Instruction compareOptimise(Instruction inst, Instruction last, Deque<Instruction>  result) {
        OpCode opcode = inst.getOpCode();
        switch(opcode) {
            case BR_IF:
                BranchInstruction brinst = (BranchInstruction)inst;
                BranchTarget target = brinst.getTarget();
                FnType unwind = target.getUnwind();
                if (!needUnwind(unwind)) {
                    result.removeLast();
                    inst = BranchInstruction.combine(last, inst);
                    OPTS.compute(OpCode.BR_IF, (k,v)->v == null?0:++v);
                }
                break;
            case SELECT:
                result.removeLast();
                inst = SimpleInstruction.combine(last,inst);
                OPTS.compute(OpCode.SELECT, (k,v)->v == null?0:++v);
                break;
            case IF:
                result.removeLast();
                inst = ControlInstruction.combine(last, inst);
                OPTS.compute(OpCode.IF, (k,v)->v == null?0:++v);
                break;
        }
        return inst;
    }
    
    private Deque<Instruction> optimize(List<Instruction> insts) {
        Deque<Instruction>  result = new ArrayDeque<>();
        for (Instruction inst:insts) {
            Instruction last = result.peekLast();
            OpType lasttype = last == null? null : last.getOpCode().getOpType();
            if (lasttype == OpType.COMPARE) {
                inst = compareOptimise(inst, last, result);
            }
            result.addLast(inst);
            ++ct;
        }
        return result;
    }
    
    public int printInsts(List<Instruction> instlist, String field_name) {
        Deque<Instruction>  insts = optimize(instlist);
        pw.println("  BLOCK");
        ValueTypeStack vts = new ValueTypeStack();
        for (Instruction inst : insts) {
            char[] spaces = new char[inst.getLevel() * 2];
            Arrays.fill(spaces, ' ');
            String spacer = String.valueOf(spaces);
            String before = vts.toString();
            FnType fntypex = inst.getFnType();
            try {
                vts.adjustStack(fntypex);
            } catch (Exception ex) {
                pw.format("%s; %s // %s%n", spacer, inst, before);
                pw.flush();
                Logger.getGlobal().severe(String.format("%s: inst = %s",field_name, inst));
                throw ex;
            }
            String after = vts.toString();
            String stackchange = String.format("%s -> %s",before,after);
            printInst(inst, spacer, stackchange);
        }
        pw.flush();
        if (!vts.addedEmpty()) {
            String msg = "stack not empty at end";
            throw new IllegalStateException(msg);
        }
            // + 2 to allow use of one temp variable when generating jynx which may be double or long
        return vts.getMaxsz() + 4;
    }
    
    public void printInst(Instruction inst, String spacer,String stackchange) {
        OpCode opcode = inst.getOpCode();
        OpType optype = opcode.getOpType();
        String compound = optype.isCompound()?"(*)":"";
        String comment = comments?String.format(" ; %s%s ; %s",compound, inst,stackchange):"";
        switch(optype) {
            case VARIABLE:
                variable(spacer, inst, comment);
                break;
            case MEMLOAD:
            case MEMSTORE:
                memory(spacer, inst, comment);
                break;
            case COMPAREIF:
            case CONTROL:
                control(spacer, inst, comment);
                break;
            case COMPAREBRIF:
            case BRANCH:
                branch(spacer, inst, comment);
                break;
            case BRANCH_TABLE:
                brtable(spacer, inst, comment);
                break;
            case MEMFN:
                long mems = inst.getImm1().longValue();
                if (opcode == MEMORY_COPY) {
                    int mem1 = (int)(mems >> 32);
                    int mem2 = (int)mems;
                    pw.format("%s  %s %d %d%s%n", spacer,opcode,mem1,mem2,comment);
                } else {
                    int mem = (int)mems;
                    pw.format("%s  %s %d%s%n", spacer,opcode,mem,comment);
                }
                break;
            case CONST:
                pw.format("%s  %s %s%s%n", spacer,opcode,num2string(inst.getImm1()),comment);
                break;
            case PARAMETRIC:
            default:
                pw.format("%s  %s%s%n",spacer,opcode,comment);
                break;
        }
    }
    
    public static String pushLocal(ValueType vt, int index) {
        return String.format("%s %d",OpCode.LOCAL_GET,index);
    }
    
    private void memory(String spacer,Instruction inst, String comment) {
        MemoryInstruction meminst = (MemoryInstruction)inst;
        int offset = meminst.getOffset();
        int alignment = meminst.getAlignment(); // alignment is a hint not semantic
        int memnum = meminst.getMemoryNumber();
        String plus = offset >= 0? "+": "";
        pw.format("%s  %s %d %s%d%s%n",spacer,inst.getOpCode(),memnum,plus,offset,comment);
    }
    
    private void unreachable(String spacer,Instruction inst, String comment) {
        if (inst instanceof UnreachableInstruction) {
            pw.format("%s  ; %s%n", spacer,inst);
        } else {
            pw.println(spacer + "  .if reachable");
            pw.format("%s  %s%s%n", spacer,inst.getOpCode(),comment);
            pw.println(spacer + "  .end_if");
        }
    }
    
    private void control(String spacer,Instruction inst, String comment) {
        FnType fntype = inst.getFnType();
        ValueType vt1 = fntype.getType(1);
        OpCode opcode = inst.getOpCode();
        switch(opcode) {
            case UNREACHABLE:
                unreachable(spacer, inst, comment);
                break;
            case BLOCK:
            case LOOP:
            case IF:
            case ELSE:
            case END:
                pw.format("%s  %s%s%n", spacer,opcode,comment);
                break;
            case RETURN:
                boolean end = inst.getLevel() == 0;
                if (end) {
                    pw.println(spacer + "  .if reachable");
                }
                pw.format("%s  %s%s%n",spacer,opcode,comment);
                if (end) {
                    pw.println(spacer + "  .end_if");
                }
                break;
            default:
                if (opcode.getOpType() != OpType.COMPAREIF) {
                    throw new AssertionError();
                }
                pw.format("%s  %s%s%n", spacer,opcode,comment);
                break;
        }
    }

    private static boolean needUnwind(FnType unwind) {
        int numparms = unwind.numParms();
        ValueType rvt = unwind.getRtype();
        return rvt.getStackSize() !=  numparms; 
    }
    
    private void brpop(String spacer, FnType unwind) {
        if (!needUnwind(unwind)) {
            return;
        }
        pw.format("%s  %s %s%n", spacer,"UNWIND",unwind.wasmString());
    }

    private void branch(String spacer,Instruction inst, String comment) {
       BranchInstruction brinst = (BranchInstruction)inst;
        BranchTarget target = brinst.getTarget();
        FnType unwind = target.getUnwind();
        int level = target.getBr2level();
        switch(inst.getOpCode()) {
            case BR_IF:
                if (!needUnwind(unwind)) {
                    pw.format("%s  %s %d%s%n",spacer,OpCode.BR_IF, level,comment);
                    return;
                }
                pw.format("%s; %s%n",spacer,comment);
                pw.format("%s  %s%n",spacer,OpCode.IF);
                String indent = spacer + "  ";
                brpop(indent,unwind);
                pw.format("%s  %s %d%n",indent,OpCode.BR,level+1);
                pw.format("%s  %s%n",spacer,OpCode.END);
                break;
            case BR:
                if (needUnwind(unwind)) {
                    pw.format("%s; %s%n",spacer,comment);
                    brpop(spacer,unwind);
                    pw.format("%s  %s %d%n",spacer,OpCode.BR,level);
                } else {
                    pw.format("%s  %s %d%s%n",spacer,OpCode.BR,level,comment);
                }
                break;
            default:
                if (inst.getOpCode().getOpType() != OpType.COMPAREBRIF){
                    throw new AssertionError();
                }
                pw.format("%s  %s %d%s%n",spacer,inst.getOpCode(), level,comment);
                break;
        }
    }

    private void brtable(String spacer,Instruction inst, String comment) {
        BrTableInstruction brinst = (BrTableInstruction)inst;
        BranchTarget[] targets = brinst.getTargets();
        boolean unwindsreq = false;
        for (BranchTarget target:targets) {
            if (needUnwind(target.getUnwind())) {
                unwindsreq = true;
                break;
            }
        }
        if (unwindsreq) {
            brtablex(spacer, inst, comment);
            return;
        }
        BranchTarget deftarget = targets[targets.length - 1]; 
        pw.format("%s  %s 0 default %d [",spacer,inst.getOpCode(),deftarget.getBr2level());
        for (int i = 0; i < targets.length - 1;++i) {
            BranchTarget target = targets[i];
            if (i == 0){
                pw.format(" %d",target.getBr2level());
            } else {
                pw.format(" , %d",target.getBr2level());
            }
        }
        pw.println(" ]");
    }

    private static int labnum = 150;
    
    private void brtablex(String spacer,Instruction inst, String comment) {
        BrTableInstruction brinst = (BrTableInstruction)inst;
        pw.format("%s%s%n",spacer,comment);
        BranchTarget[] targets = brinst.getTargets();
        BranchTarget deftarget = targets[targets.length - 1];
        int label = labnum;
        labnum += targets.length;
        FnType defunwind = deftarget.getUnwind();
        pw.format("%s  %s 0 default",spacer,OpCode.BR_TABLE);
        if (needUnwind(defunwind)) {
            int deflab = label + targets.length - 1;
            pw.format(" L%d .array%n",deflab);
        } else {
            pw.format(" %d .array%n",deftarget.getBr2level());
        }
        for (int i = 0; i < targets.length -1;++i) {
            int labi = label + i;
            BranchTarget target = targets[i];
            FnType unwind = target.getUnwind();
            if (needUnwind(unwind)) {
                pw.format("%s  L%d ; %s%n",spacer,labi,unwind);
            } else {
                pw.format("%s  %d%n",spacer, target.getBr2level());
            }
        }
        pw.format("%s.end_array%n",spacer);
        for (int i = 0; i < targets.length;++i) {
            BranchTarget target = targets[i];
            FnType unwind = target.getUnwind();
            if (needUnwind(unwind)) {
                pw.format("%s  L%d:%n", spacer,(label + i));
                brpop(spacer,unwind);
                pw.format("%s  %s %d%n", spacer,OpCode.BR,target.getBr2level());
            }
        }
    }

    private void variable(String spacer,Instruction inst, String comment) {
        FnType fntype = inst.getFnType();
        ValueType vtr = fntype.getRtype();
        ValueType vt1 = fntype.getType(1);
        ValueType varvt = vtr == V00?vt1:vtr;
        ObjectInstruction objinst  = (ObjectInstruction)inst;
        Object obj = objinst.getObject();
        String name;
        Local local;
        OpCode opcode = inst.getOpCode();
        switch(opcode) {
            case LOCAL_GET:
            case LOCAL_SET:
            case LOCAL_TEE:
                local = (Local)obj;
                pw.format("%s  %s %s%s%n",
                        spacer,opcode,local.getName(),comment);
                break;
            case GLOBAL_GET:
            case GLOBAL_SET:
                name = javaName.localName(((Global)obj));
                String prefix = JynxModule.getPrefix(varvt);
                pw.format("%s  %s%s %s%s%n",spacer,prefix,opcode,name,comment);
                break;
            case CALL:
                WasmFunction called = (WasmFunction)obj;
                name = javaName.localName(called);
                pw.format("%s  %s %s%s%s%n",spacer,opcode, name, fntype.wasmString(),comment);
                break;
            case CALL_INDIRECT:
                int tablenum = ((Table)obj).getTableNum();
                pw.format("%s  %s %d %s%s%n",
                        spacer,opcode,tablenum,fntype.wasmString(),comment);
                break;
            default:
                throw new AssertionError();
        }
    }

}
