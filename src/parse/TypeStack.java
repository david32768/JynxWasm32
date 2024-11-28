package parse;

import static parse.ValueType.I32;
import static parse.ValueType.V00;

import util.LIFOStack;
import wasm.Instruction;
import wasm.OpCode;

public class TypeStack {

    private final ValueTypeStack vtstack = new ValueTypeStack();
    private final LIFOStack<BlockEntry> blocks = new LIFOStack<>();
    private final FnType fntype;
    private final ValueType vtfnr;
    private final Local[] locals;
    private final Section code;
    private final WasmModule module;

    private BlockEntry currentBlock;

    public TypeStack(FnType fntype, Local[] locals, Section code, WasmModule module) {
        this.fntype = fntype;
        this.vtfnr = fntype.getRtype();
        this.locals = locals;
        this.code = code;
        this.module = module;
        this.currentBlock = new BlockEntry(OpCode.BLOCK, 0, vtfnr);
        vtstack.setFloor();
        blocks.push(currentBlock);
        // parameters are in locals 0 ->
    }

    public Section getCode() {
        return code;
    }

    public boolean isEmpty() {
        return vtstack.addedEmpty() && blocks.isEmpty();
    }

    public Local getLocal(int index) {
        return locals[index];
    }

    public FnType FnType() {
        return fntype;
    }

    public ValueType peek() {
        return vtstack.peek();
    }
    
    public ValueType peekNext() {
        return vtstack.peekNext();
    }

    public ValueType returnType() {
        return vtfnr;
    }

    public WasmModule getModule() {
        return module;
    }

    public void updateFallThroughToEnd(boolean reachable) {
        if (reachable) {
            currentBlock.setFallThroughToEnd();
        }
    }
    
    private void checkStackState(BlockEntry match) {
        ValueType vt = match.getVt();
        int testsz = vt == V00 ? 0 : 1;
        ValueType svt = vtstack.peekIf(vt);
        if (vtstack.addedSize() != testsz || !svt.isCompatible(vt)) {
            String msg = String.format("%nstacksize = %d expected = %d svt = %s vt = %s%nstack = %s%nblockentry = {%s}%n",
                    vtstack.addedSize(), testsz, svt, vt, vtstack,match);
            throw new IllegalStateException(msg);
        }
    }

    private BlockEntry getUnwindBlock(int brlevel) {
        try {
            return blocks.peek(brlevel);
        } catch (IndexOutOfBoundsException iobex) {
            String msg = String.format("branch to level %d but number of outstanding ends = %d",
                    brlevel,blocks.size());
            throw new IllegalArgumentException(msg);
        }
    }

    public BlockEntry getCurrentBlock() {
        return currentBlock;
    }
    
    public FnType getUnwind() {
        if (blocks.isEmpty()) {
            return vtstack.getUnwind(V00, 0,true);
        }
        ValueType vt = currentBlock.getVt();
        return vtstack.getUnwind(vt, currentBlock.getStackptr(),true);
    }

    public BranchTarget getTarget(int br2level, boolean tos) {
        BlockEntry blockentryx = getUnwindBlock(br2level);
        blockentryx.setBranchToEnd();
        ValueType vtr = blockentryx.getVt();
        if (blockentryx.getOpCode() == OpCode.LOOP) vtr = V00;
        FnType unwindft = vtstack.getUnwind(vtr, blockentryx.getStackptr(),tos);
        return new BranchTarget(br2level, unwindft);
    }
    
    public boolean changeControl(final Instruction inst) {
        OpCode opcode = inst.getOpCode();
        FnType optype = inst.getFnType();
        ValueType vtr;
        boolean unreachable = false;
        switch (opcode) {
            case IF:
                vtstack.adjustStack(optype);
            case BLOCK:
            case LOOP:
                ValueType vt = inst.getBlockType();
                vtstack.setFloor();
                currentBlock = new BlockEntry(opcode, vtstack.size(),vt);
                blocks.push(currentBlock);
                break;
            case ELSE:
                if (currentBlock.getOpCode() != OpCode.IF) {
                    String msg = String.format("IF expected as last control op but is %s%n", currentBlock.getOpCode());
                    throw new IllegalStateException(msg);
                }
                vt = currentBlock.getVt();
                checkStackState(currentBlock);
                if (vt != V00) {
                    vtstack.pop();
                }
                currentBlock = currentBlock.toElse();
                blocks.pop();
                blocks.push(currentBlock);
                break;
            case END:
                if (currentBlock.getOpCode() == OpCode.IF) {
                    currentBlock.setBranchToEnd();
                }
                if (currentBlock.isFallThroughToEnd()) {
                    checkStackState(currentBlock);
                } else {
                    vtstack.adjustStack(optype);
                }
                unreachable = !currentBlock.isEndReachable();
                blocks.pop();
                currentBlock = blocks.peek();
                int oldfloor = currentBlock == null?0:currentBlock.getStackptr();
                vtstack.resetFloor(oldfloor);
                break;
            case BR_TABLE:
                if (!optype.lastParm().isCompatible(I32)) {
                    throw new StackStateException();
                }
                vtr = inst.getBlockType();
                if (vtr != V00) {
                    ValueType vta = vtstack.peekNext();
                    if (!vtr.isCompatible(vta)) {
                        String msg = String.format("%nstack value = %s parm value = %s%n", vta, vtr);
                        throw new IllegalStateException(msg);
                    }
                }
                vtstack.adjustStack(optype);
                break;
            case BR:
                vtr = inst.getBlockType();
                if (vtr != V00) {
                    ValueType vta = vtstack.peek();
                    if (!vtr.isCompatible(vta)) {
                        String msg = String.format("%nstack value = %s parm value = %s%n", vta, vtr);
                        throw new IllegalStateException(msg);
                    }
                }
                vtstack.adjustStack(optype);
                break;
            default:
                vtstack.adjustStack(optype);
                break;
        }
        return opcode.isTransfer() || unreachable;
    }

    @Override
    public String toString() {
        return vtstack.toString();
    }

}
