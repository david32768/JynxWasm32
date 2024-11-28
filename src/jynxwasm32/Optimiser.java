package jynxwasm32;

import java.util.ArrayList;
import java.util.List;

import static wasm.OpCode.BR_IF;
import static wasm.OpCode.IF;
import static wasm.OpCode.SELECT;

import parse.BranchTarget;
import wasm.BranchInstruction;
import wasm.ControlInstruction;
import wasm.Instruction;
import wasm.OpCode;
import wasm.OpType;
import wasm.SimpleInstruction;

public class Optimiser {
    
    private static Instruction compareOptimise(Instruction inst, Instruction last) {
        Instruction optlast = null;
        OpCode opcode = inst.getOpCode();
        switch(opcode) {
            case BR_IF:
                BranchInstruction brinst = (BranchInstruction)inst;
                BranchTarget target = brinst.getTarget();
                if (!target.needUnwind()) {
                    optlast = BranchInstruction.combine(last, inst);
                }
                break;
            case SELECT:
                optlast = SimpleInstruction.combine(last,inst);
                break;
            case IF:
                optlast = ControlInstruction.combine(last, inst);
                break;
        }
        return optlast;
    }
    
    public static List<Instruction> optimize(List<Instruction> insts) {
        List<Instruction>  result = new ArrayList<>();
        Instruction last = null;
        for (Instruction inst:insts) {
            if (last != null) {
                OpType lasttype = last.getOpCode().getOpType();
                if (lasttype == OpType.COMPARE) {
                    Instruction optlast = compareOptimise(inst, last);
                    if (optlast != null) {
                        last = optlast;
                        inst = null;
                    }
                }
                result.add(last);
            }
            last = inst;
        }
        if (last != null) {
            result.add(last);
        }
        return result;
    }

}
