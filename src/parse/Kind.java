package parse;

public interface Kind {
    
    public void exportNames(String module_name,String field_name);

    public KindName getKindName();

    public default String getName() {
        KindName kn = getKindName();
        return kn.getModuleName() + "/" + kn.getFieldName();
    }

    public default String getDefaultName() {
        return getKindName().getDefaultName();
    }
    
    public default String getModuleName() {
        return getKindName().getModuleName();
    }
    
    public default String getFieldName() {
        return getKindName().getFieldName();
    }
    
    public default boolean isImported() {
        return getKindName().getStatus() == Status.IMPORTED;
    }

    public default boolean isExported() {
        return getKindName().getStatus() == Status.EXPORTED;
    }

    public default boolean isPrivate() {
        return getKindName().getStatus() == Status.PRIVATE;
    }
}
