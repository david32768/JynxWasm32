package parse;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import util.HexDump;

public class Data_segment {
    
    private final int offset;
    private final byte[] data;

    public Data_segment(int offset, byte[] data) {
        this.offset = offset;
        this.data = data;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public int getOffset() {
        return offset;
    }
    
/*
    ### Data section

The data section declares the initialized data that is loaded
into the linear memory.

| Field | Type | Description |
| ----- | ---- | ----------- |
| count | `varuint32` | count of data segments to follow |
| entries | `data_segment*` | repeated data segments as described below |

a `data_segment` is:

| Field | Type | Description |
| ----- | ---- | ----------- |
| index | `varuint32` | the [linear memory index](Modules.md#linear-memory-index-space) (0 in the MVP) |
| offset | `init_expr` | an `i32` initializer expression that computes the offset at which to place the data |
| size | `varuint32` | size of `data` (in bytes) |
| data | `bytes` | sequence of `size` bytes |

    */    
    public static void parse(WasmModule module, Section section)  {
        int count = section.vecsz();
        for (int i = 0; i < count;i++) {
            int index = section.memidx();
            int offset =  Expression.parseConstant(module,section).intValue();
            Memory memory = module.atmemidx(index);
            int length = section.vecsz();
            byte[] data = section.byteArray(length);
            Data_segment ds = new Data_segment(offset, data);
            Logger.getGlobal().fine(String.format("data %d size = %d offset = %d",i, data.length, offset));
            Logger.getGlobal().finest(()->HexDump.printHex(ByteBuffer.wrap(data), 0));
            memory.add(ds);
        }
        
    }
}
