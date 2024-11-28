package jynxwasm32;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

import static wasm.OpCode.*;

import parse.BranchTarget;
import parse.Local;
import util.LIFOStack;
import wasm.BrTableInstruction;
import wasm.BranchInstruction;
import wasm.ControlInstruction;
import wasm.Instruction;
import wasm.OpCode;
import wasm.VariableInstruction;

public class NeedInit {
    
    public static BitSet uninitialisedVar(BitSet vars, List<Instruction> insts) {
        int numvar = vars.size();
        BitSet uninit = new BitSet(numvar);
        LIFOStack<Block> stack = new LIFOStack<>();
        BitSet current = new BitSet(numvar);
        stack.push(new Block(null, current, vars));
        boolean reachable = true;
        for (Instruction inst : insts) {
            OpCode op = inst.getOpCode();
            if (!reachable) {
                assert EnumSet.of(END, UNREACHABLE, ELSE).contains(op): op;
                current = new BitSet();
            }
            if (inst instanceof VariableInstruction objinst) {
                Object obj = objinst.getObject();
                int local;
                switch(op) {
                    case LOCAL_GET:
                        local = ((Local)obj).getNumber();
                        if (!current.get(local)) {
                            uninit.set(local);
                        }
                        break;
                    case LOCAL_SET:
                    case LOCAL_TEE:
                        local = ((Local)obj).getNumber();
                        current.set(local);
                        break;
                }
            } else if (inst instanceof ControlInstruction) {
                Block block;
                switch(op) {
                    case LOOP:
                    case BLOCK:
                    case IF:
                        stack.push(new Block(op, current, vars));
                        break;
                    case ELSE:
                        block = stack.peek();
                        assert block.op() == IF;
                        if (reachable) {
                            block.updateVarsForBranch(current);
                        }
                        current = block.startVars();
                        reachable = true;
                        break;
                    case END:
                        block = stack.pop();
                        if (reachable) {
                            block.updateVarsIfReached(current);
                        }
                        reachable = block.isReached();
                        current = block.endVars();
                        break;
                    case RETURN:
                    case UNREACHABLE:
                        reachable = false;
                        break;
                }
            } else if (inst instanceof BranchInstruction brinst) {
                BranchTarget target = brinst.getTarget();
                int br2 = target.getBr2level();
                stack.peek(br2)
                    .updateVarsForBranch(current);
                reachable = op != BR;
            } else if (inst instanceof BrTableInstruction tabinst) {
                BranchTarget[] targets = tabinst.getTargets();
                for (BranchTarget target : targets) {
                    int br2 = target.getBr2level();
                    stack.peek(br2)
                        .updateVarsForBranch(current);
                }
                reachable = false;
            }            
        }
        uninit.and(vars);
        return uninit;
    }
}
