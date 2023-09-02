package parse;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import static parse.Reason.M201;

import util.HexDump;
import wasm.Instruction;

public class Data_segment {
    
    private final ConstantExpression constexpr;
    private final byte[] data;

    public Data_segment(ConstantExpression constexpr, byte[] data) {
        this.constexpr = constexpr;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
    
    public Instruction getOffsetInstruction() {
        return constexpr.getConstInst();
    }
    
    public static void parse(WasmModule module, Section section)  {
        ParseMethods.parseSectionVector(section, i->checkCount(module,i), Data_segment::parseDataSegment);
    }

    private static WasmModule checkCount(WasmModule module, Integer count) {
        Integer sec12ct = module.getDataCount();
        if (sec12ct != null && !count.equals(sec12ct)) {
            // "data count and data section have inconsistent lengths"
            throw new ParseException(M201,"number of data segments = %d, section 12 count = %d",
                count,sec12ct);            
        }
        return module;
    }

    /*
    a `data_segment` is:

    | Field  | Type        | Description                                                                         |
    | ------ | ----------- | ----------------------------------------------------------------------------------- |
    | index  | `varuint32` | the [linear memory index](Modules.md#linear-memory-index-space) (0 in the MVP)      |
    | offset | `init_expr` | an `i32` initializer expression that computes the offset at which to place the data |
    | size   | `varuint32` | size of `data` (in bytes)                                                           |
    | data   | `bytes`     | sequence of `size` bytes                                                            |

    */    
    public static void parseDataSegment(WasmModule module, Section section, int i)  {
        int index = section.memidx();
        ConstantExpression constexpr =  ConstantExpression.parseConstantExpression(module,section);
        Memory memory = module.atmemidx(index);
        byte[] data = section.byteVector();
        Data_segment ds = new Data_segment(constexpr, data);
        Logger.getGlobal().fine(String.format("data segment %d size = %d offset = %s",
                i,data.length, constexpr.getConstantString()));
        Logger.getGlobal().finest(()->HexDump.printHex(ByteBuffer.wrap(data), 0));
        memory.add(ds);
    }
}
