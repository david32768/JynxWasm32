package parse;

import java.util.ArrayList;
import java.util.logging.Logger;

import static parse.Reason.M105;

public class Local implements CanHaveDebugName {

    private final ValueType type;
    private final int relnum;
    private final boolean parm;
    
    private String name;
    
    private Local(ValueType type, int relnum, boolean parm) {
        this.type = type;
        this.relnum = relnum;
        this.parm = parm;
    }

    public ValueType getType() {
        return type;
    }

    @Override
    public void setDebugName(String name) {
        if (this.name == null) {
            this.name = name;
        } else {
            String msg = String.format("cannot set name to %s as already set to %s",name,this.name);
            Logger.getGlobal().warning(msg);
        }
    }
    
    public String getName() {
        return name == null? "$" + relnum: name;
    }
    
    public boolean isParm() {
        return parm;
    }

    public int getNumber() {
        return relnum;
    }

    @Override
    public String toString() {
        return String.format("%s %d %s",type,relnum,parm?"parameter":"");
    }

    
/*
    #### Local Entry

Each local entry declares a number of local variables of a given type.
It is legal to have several entries with the same type.

| Field | Type         | Description                                     |
| ----- | ------------ | ----------------------------------------------- |
| count | `varuint32`  | number of local variables of the following type |
| type  | `value_type` | type of the variables                           |

    */    
    
    private static final int MAXLOCALS = 2*Short.MAX_VALUE + 1;
    
    public static Local[] parse(Section section, LocalFunction fn) {
        // parameters are parms 0 ->
        FnType fntype = fn.getFnType();
        ValueType[] parms = fntype.getParm();
        ArrayList<Local> result = new ArrayList<>();
        int relnum = 0;
        for (int i = 1; i < parms.length;++i) {
            ValueType vt = parms[i];
            Local local = new Local(vt,relnum,true);
            result.add(local);
            ++relnum;
        }

        LocalsVT[] vtlocals = ParseMethods.parseArray(section, LocalsVT[] ::new,
                sect->new LocalsVT(sect.vecsz(),sect.getValueType()));
        Logger.getGlobal().fine(String.format("locals has %d sections", vtlocals.length));

        long total = 0;
        for (LocalsVT lvt:vtlocals) {
            int count = lvt.count();
            ValueType type = lvt.vt();
            Logger.getGlobal().fine(String.format("%d locals of type %s", count,type));
            total += Integer.toUnsignedLong(lvt.count());
        }
        if (total > MAXLOCALS) {
            // "too many locals"
            throw new ParseException(M105,"total locals is %d; implementation limit is %d", total, MAXLOCALS);
        }
        for (LocalsVT lvt:vtlocals) {
            int count = lvt.count();
            ValueType type = lvt.vt();
            for (int j = 0; j < count;j++) {
                Local local = new Local(type,relnum,false);
                result.add(local);
                ++relnum;
            }            
        }
        return result.toArray(new Local[0]);
    }

}
