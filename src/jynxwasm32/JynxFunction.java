package jynxwasm32;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;

import static parse.ValueType.V00;
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
import wasm.ConstantInstruction;
import wasm.Instruction;
import wasm.InvokeInstruction;
import wasm.MemoryFunctionInstruction;
import wasm.MemoryInstruction;
import wasm.OpCode;
import wasm.OpType;
import wasm.UnreachableInstruction;
import wasm.VariableInstruction;

public class JynxFunction {
    
    private final PrintWriter pw;
    private final JavaName javaName;
    private final boolean comments;
    private final FunctionStats stats;
    
    public JynxFunction(PrintWriter pw, JavaName javaName, boolean comments, FunctionStats stats) {
        this.pw = pw;
        this.javaName = javaName;
        this.comments = comments;
        this.stats = stats;
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
        int maxlocal = printInit(fn);

        List<Instruction>  insts = Optimiser.optimize(fn.getInsts());

        stats.addStats(fn.getFieldName(), insts);

        int maxstack = printInsts(insts, fn.getFieldName(), fn.getFnType().getRtype());

        pw.format("; locals %d stack %d; + macro instruction requirements%n", maxlocal, maxstack);
        pw.println(".end_method");
        pw.flush();
    }

    private int printInit(LocalFunction fn) {
        Local[] locals = fn.getLocals();
        BitSet initvars = fn.getVarsToInit();
        List<Instruction> insts = fn.getInsts();
        int maxlocal = 0;
        for (int i = 0; i < locals.length ; ++i) {
            Local local = locals[i];
            ValueType vt = local.getType();
            maxlocal += vt.getStackSize();
            if (local.isParm()) {
                String name = local.getName();
                int num = local.getNumber();
                pw.format(".parameter %d %s%n", num, name);
            }
        }

        try {
            initvars = NeedInit.uninitialisedVar(initvars, insts);
        } catch (AssertionError ex) {
            System.err.format("fn %s%n%s%n", javaName.simpleName(fn), ex);
        }
        for (int i = 0; i < locals.length ; ++i) {
            Local local = locals[i];
            ValueType vt = local.getType();
            if (initvars.get(i)) {
                JynxOpCode init = JynxOpCode.localInit(vt);
                pw.format("  %s %s%n", init, local.getName());
            }
        }
        return maxlocal;
    }

    private static String num2string(Number num) {
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

    private int printInsts(List<Instruction>  insts, String field_name, ValueType rt) {
        pw.format("  %s%n", OpCode.BLOCK);
        int level = 1;
        ValueTypeStack vts = new ValueTypeStack();
        for (Instruction inst : insts) {
            OpCode op = inst.getOpCode();
            int mylevel = level + op.myLevelChange();
            String spacer = "  ".repeat(mylevel);
            level += op.levelChange();
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

        ifReachable("", OpCode.RETURN, "");
        vts.adjustStack(FnType.consume(rt));

        pw.flush();
        if (!vts.addedEmpty()) {
            String msg = "stack not empty at end";
            throw new IllegalStateException(msg);
        }
        return vts.getMaxsz();
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
            case INVOKE:
                invoke(spacer, inst, comment);
                break;
            case MEMLOAD:
            case MEMSTORE:
                memory(spacer, inst, comment);
                break;
            case COMPARE_IF:
            case CONTROL:
                control(spacer, inst, comment);
                break;
            case COMPARE_BRIF:
            case BRANCH:
                branch(spacer, inst, comment);
                break;
            case BRANCH_TABLE:
                brtablex(spacer, inst, comment);
                break;
            case MEMFN:
                MemoryFunctionInstruction memfninst = (MemoryFunctionInstruction)inst;
                int memidx1 = memfninst.memidx1();
                if (opcode == MEMORY_COPY) {
                    int memidx2 = memfninst.memidx2();
                    pw.format("%s  %s %d %d%s%n", spacer,opcode,memidx1,memidx2,comment);
                } else {
                    pw.format("%s  %s %d%s%n", spacer,opcode,memidx1,comment);
                }
                break;
            case CONST:
                ConstantInstruction constinst = (ConstantInstruction)inst;
                pw.format("%s  %s %s%s%n", spacer,opcode,num2string(constinst.getConstant()),comment);
                break;
            case PARAMETRIC:
            default:
                pw.format("%s  %s%s%n",spacer,opcode,comment);
                break;
        }
    }
    
    private void memory(String spacer,Instruction inst, String comment) {
        MemoryInstruction meminst = (MemoryInstruction)inst;
        int offset = meminst.getOffset();
        int alignment = meminst.getAlignment(); // alignment is a hint not semantic
        int memnum = meminst.getMemoryNumber();
        String plus = offset >= 0? "+": "";
        pw.format("%s  %s %d %s%d%s%n",spacer,inst.getOpCode(),memnum,plus,offset,comment);
    }
    
    private void ifReachable(String spacer, OpCode opcode, String comment) {
        pw.println(spacer + "  .if reachable");
        pw.format("%s  %s%s%n", spacer, opcode, comment);
        pw.println(spacer + "  .end_if");
    }
    
    private void unreachable(String spacer,Instruction inst, String comment) {
        if (inst instanceof UnreachableInstruction) {
            pw.format("%s  ; %s%n", spacer,inst);
        } else {
            ifReachable(spacer, inst.getOpCode(), comment);
        }
    }
    
    private void control(String spacer,Instruction inst, String comment) {
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
            case RETURN:
                pw.format("%s  %s%s%n", spacer,opcode,comment);
                break;
            default:
                if (opcode.getOpType() != OpType.COMPARE_IF) {
                    throw new AssertionError();
                }
                pw.format("%s  %s%s%n", spacer,opcode,comment);
                break;
        }
    }

    private void brpop(String spacer, BranchTarget target) {
        if (target.needUnwind()) {
            pw.format("%s  %s %s%n", spacer,JynxOpCode.UNWIND, target.getUnwind().wasmString());
        }
    }

    private void branch(String spacer,Instruction inst, String comment) {
        BranchInstruction brinst = (BranchInstruction)inst;
        BranchTarget target = brinst.getTarget();
        int level = target.getBr2level();
        switch(inst.getOpCode()) {
            case BR_IF:
                if (!target.needUnwind()) {
                    pw.format("%s  %s %d%s%n",spacer,OpCode.BR_IF, level,comment);
                    return;
                }
                pw.format("%s; %s%n",spacer,comment);
                pw.format("%s  %s%n",spacer,OpCode.IF);
                String indent = spacer + "  ";
                brpop(indent, target);
                pw.format("%s  %s %d%n",indent,OpCode.BR,level+1);
                pw.format("%s  %s%n",spacer,OpCode.END);
                break;
            case BR:
                if (target.needUnwind()) {
                    pw.format("%s; %s%n",spacer,comment);
                    brpop(spacer, target);
                    pw.format("%s  %s %d%n",spacer,OpCode.BR,level);
                } else {
                    pw.format("%s  %s %d%s%n",spacer,OpCode.BR,level,comment);
                }
                break;
            default:
                if (inst.getOpCode().getOpType() != OpType.COMPARE_BRIF){
                    throw new AssertionError();
                }
                pw.format("%s  %s %d%s%n",spacer,inst.getOpCode(), level,comment);
                break;
        }
    }

    private static int labnum = 150;
    
    private void brtablex(String spacer,Instruction inst, String comment) {
        BrTableInstruction brinst = (BrTableInstruction)inst;
        pw.format("%s%s%n",spacer,comment);
        BranchTarget[] targets = brinst.getTargets();
        BranchTarget deftarget = targets[targets.length - 1];
        int label = labnum;
        labnum += targets.length;
        pw.format("%s  %s default",spacer,OpCode.BR_TABLE);
        if (deftarget.needUnwind()) {
            int deflab = label + targets.length - 1;
            pw.format(" L%d .array%n",deflab);
        } else {
            pw.format(" %d .array%n",deftarget.getBr2level());
        }
        for (int i = 0; i < targets.length -1;++i) {
            int labi = label + i;
            BranchTarget target = targets[i];
            if (target.getBr2level() != deftarget.getBr2level()) {
                FnType unwind = target.getUnwind();
                if (target.needUnwind()) {
                    pw.format("%s    %d -> L%d ; %s%n", spacer, i, labi, unwind);
                } else {
                    pw.format("%s    %d -> %d%n", spacer, i, target.getBr2level());
                }
            }
        }
        pw.format("%s  .end_array%n",spacer);
        for (int i = 0; i < targets.length;++i) {
            BranchTarget target = targets[i];
            if (target.getBr2level() != deftarget.getBr2level()) {
                if (target.needUnwind()) {
                    pw.format("%s  L%d:%n", spacer,(label + i));
                    brpop(spacer, target);
                    pw.format("%s  %s %d%n", spacer,OpCode.BR,target.getBr2level());
                }
            }
        }
    }

    private void variable(String spacer,Instruction inst, String comment) {
        FnType fntype = inst.getFnType();
        ValueType vtr = fntype.getRtype();
        ValueType vt1 = fntype.getType(1);
        ValueType varvt = vtr == V00?vt1:vtr;
        VariableInstruction objinst  = (VariableInstruction)inst;
        Object obj = objinst.getObject();
        String name;
        Local local;
        OpCode opcode = inst.getOpCode();
        switch(opcode) {
            case LOCAL_GET:
            case LOCAL_SET:
            case LOCAL_TEE:
                local = (Local)obj;
                pw.format("%s  %s_%s %s%s%n",
                        spacer, varvt, opcode, local.getName(), comment);
                break;
            case GLOBAL_GET:
            case GLOBAL_SET:
                name = javaName.localName(((Global)obj));
                pw.format("%s  %s_%s %s%s%n",
                        spacer, varvt, opcode, name, comment);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void invoke(String spacer,Instruction inst, String comment) {
        FnType fntype = inst.getFnType();
        InvokeInstruction objinst  = (InvokeInstruction)inst;
        Object obj = objinst.getObject();
        String name;
        OpCode opcode = inst.getOpCode();
        switch(opcode) {
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
