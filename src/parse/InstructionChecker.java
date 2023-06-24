package parse;

import java.util.logging.Logger;
import wasm.ControlInstruction;
import wasm.Instruction;
import wasm.OpCode;
import wasm.UnreachableInstruction;

public class InstructionChecker {

    private final TypeStack ts;
    private final String fnname;

    private boolean unreachable;
    private int unreachablelevel;

    public InstructionChecker(TypeStack ts, String fnname) {
        this.ts = ts;
        this.fnname = fnname;
        this.unreachable = false;
        this.unreachablelevel = 0;
    }

    public Instruction from(Op op) {
        OpCode opcode = op.getOpCode();
        Instruction comment = UnreachableInstruction.unreachable(op, ts);
        switch (opcode) {
            case END:
                if (unreachable) {
                    if (unreachablelevel != 0) {
                        --unreachablelevel;
                        logUnreachable(fnname, opcode);
                        return comment;
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
                    logUnreachable(fnname, opcode);
                    return comment;
                } else {
                    ts.updateFallThroughToEnd();
                }
                break;
            case UNREACHABLE:
                if (unreachable) {
                    logUnreachable(fnname, opcode);
                    return comment;
                }
                break;
            case BLOCK:
            case IF:
            case LOOP:
                if (unreachable) {
                    ++unreachablelevel;
                    logUnreachable(fnname, opcode);
                    return comment;
                }
                break;
            case RETURN:
            default:
                if (unreachable) {
                    logUnreachable(fnname, opcode);
                    return comment;
                }
                break;
        }
        Instruction inst = op.getInstruction(ts);
        inst = ts.changeControl(inst);
        unreachable = opcode.isTransfer()
                || opcode == OpCode.END && !((ControlInstruction) inst).getBlock().isEndReachable();
        unreachablelevel = 0;
        return inst;
    }

    private static void logUnreachable(String fnname, OpCode opcode) {
        if (opcode == OpCode.UNREACHABLE) {
            Logger.getGlobal().fine(String.format("%s: unreachable instruction (%s) dropped",
                    fnname, opcode.name()));
        } else {
            Logger.getGlobal().warning(String.format("%s: unreachable instruction (%s) dropped",
                    fnname, opcode.name()));
        }
    }

}
