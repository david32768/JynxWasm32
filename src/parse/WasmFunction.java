package parse;

import java.util.logging.Logger;

public interface WasmFunction extends Kind, CanHaveDebugName {
    
    public FnType getFnType();

    default void setName(String name) {
        throw new UnsupportedOperationException();
    }

    default Local getLocal(int index) {
        throw new UnsupportedOperationException();
    }
    
    default boolean hasCode() {
        return false;
    }

    @Override
    default String getDesc() {
        return getFnType().wasmString();
    }
    
    @Override
    default void setDebugName(String name) {
        String curname = getName();
        int index = curname.indexOf('/');
        if (index >= 0) {
            curname = curname.substring(index + 1);
        }
        if (isImported()) {
            if (!curname.equals(name)) {
                Logger.getGlobal().fine(String.format("ignoring renaming of imported function from %s to %s",
                        curname, name));
            }
        } else {
            if (!curname.equals(name)) {
                String fnaccess = isPrivate()?"":"non-private ";
                String msg = String.format("renaming local %s function %s from %s to %s",
                        fnaccess, getFnType().wasmString(),curname, name);
                setName(name);
                if (isPrivate()) {
                    Logger.getGlobal().fine(msg);
                } else {
                    Logger.getGlobal().warning(msg);
                }
            }
        }
    }
}
