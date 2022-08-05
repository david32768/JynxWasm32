package wasm;

import java.util.function.BiFunction;

import parse.BrTableOp;
import parse.BranchOp;
import parse.ControlOp;
import parse.ImmediateOp;
import parse.MemoryOp;
import parse.ObjectOp;
import parse.Op;
import parse.Section;
import parse.SimpleOp;
import parse.TypeStack;

public enum OpType {
    BINARY(SimpleInstruction::binary,SimpleOp::getInstance),
    UNARY(SimpleInstruction::unary,SimpleOp::getInstance),
    TRANSFORM(SimpleInstruction::transform,SimpleOp::getInstance),

    COMPARE(SimpleInstruction::compare,SimpleOp::getInstance),

    PARAMETRIC(SimpleInstruction::parametric,SimpleOp::getInstance),
    VARIABLE(ObjectInstruction::variable,ObjectOp::getInstance),
    CONST(ImmediateInstruction::constant,ImmediateOp::getConstant),
    MEMFN(ImmediateInstruction::memfn,ImmediateOp::getMemfn),
    MEMLOAD(MemoryInstruction::memload,MemoryOp::getInstance),
    MEMSTORE(MemoryInstruction::memstore,MemoryOp::getInstance),
    CONTROL(ControlInstruction::control,ControlOp::getInstance),
    COMPAREIF(null,null),
    COMPAREBRIF(null,null),
    COMPARESELECT(null,null),
    BRANCH_TABLE(BrTableInstruction::brtable,BrTableOp::getInstance),
    BRANCH(BranchInstruction::branch,BranchOp::getInstance),
    ;
    
    private final BiFunction<Op,TypeStack, Instruction> getInst;
    private final BiFunction<OpCode,Section, Op> getOp;

    private OpType(BiFunction<Op, TypeStack, Instruction> getInst, BiFunction<OpCode, Section, Op> getOp) {
        this.getInst = getInst;
        this.getOp = getOp;
    }

    public Instruction getInstruction(Op op, TypeStack typestack) {
        return getInst.apply(op, typestack);
    }

    public Op getOp(OpCode opcode,Section section) {
        return getOp.apply(opcode, section);
    }
}
