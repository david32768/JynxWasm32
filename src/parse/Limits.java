package parse;

public class Limits {
    
    /*
    ### `resizable_limits`
A packed tuple that describes the limits of a
[table](Semantics.md#table) or [memory](Semantics.md#resizing):

| Field   | Type         | Description                                               |
| ------- |  ----------- | --------------------------------------------------------- |
| flags   | `varuint32`  | bit `0x1` is set if the maximum field is present          |
| initial | `varuint32`  | initial length (in units of table elements or wasm pages) |
| maximum | `varuint32`? | only present if specified by `flags`                      |

The "flags" field may later be extended to include a flag for sharing (between
threads).

*/
    
    private final int[] limits;

    private Limits(int initial) {
        this.limits = new int[]{initial};
    }
    
    private Limits(int initial, int maximum) {
        this.limits = new int[]{initial, maximum};
    }

    @Override
    public String toString() {
        String init = "initial " + limits[0];
        String maximum = hasMaximum()?" maximum " + limits[1]:"";
        return init + maximum;
    }

    public int getInitial() {
        return limits[0];
    }

    public boolean hasMaximum() {
        return limits.length == 2;
    }
    
    public int getMaximum() { 
        return hasMaximum()?limits[1]:limits[0];
    }
    
    public  static Limits parse(Section section) {
        boolean maxflag = section.getFlag();
        int initial =  section.getU32();
        if (!maxflag) return new Limits(initial);
        int maximum = section.getU32();
        return new Limits(initial,maximum);
    }
}
