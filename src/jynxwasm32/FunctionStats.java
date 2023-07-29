package jynxwasm32;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import wasm.Feature;
import wasm.Instruction;
import wasm.OpCode;

public class FunctionStats {
    
    private final EnumMap<Feature,Integer> features;
    
    private int fnct;
    
    public FunctionStats() {
        this.features = new EnumMap<>(Feature.class);
        this.fnct = 0;
    }
    
    public void printStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("features used:\n");
        int ct = 0;
        String tabs = "      ";
        for (Map.Entry<Feature,Integer> me : features.entrySet()) {
            Feature feature = me.getKey();
            Integer count = me.getValue();
            sb.append(tabs).append(feature).append(" ").append(count).append('\n');
            ct += count;
        }
        Logger.getGlobal().info(sb.toString());
        String msg = String.format("number of functions = %d total instructions = %s%n", fnct, ct);
        Logger.getGlobal().info(msg);
    }
    
    public void addStats(String methodname,List<Instruction> insts) {
        ++fnct;
        for (Instruction inst:insts) {
            OpCode opcode = inst.getOpCode();
            features.compute(opcode.getFeature(), (k,v) -> v == null?1:v + 1);
        }
    }
    
}
