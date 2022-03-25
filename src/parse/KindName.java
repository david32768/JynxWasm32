package parse;

public class KindName {
    
    private final KindType type;
    private final String module_name;
    private final String field_name;
    private final Status status;
    private final int number;

    public KindName(KindType type, String module_name, String field_name, Status status, int number) {
        this.type = type;
        this.module_name = module_name;
        if (field_name == null) {
            this.field_name = defaultName(type,number);
        } else {
            this.field_name = field_name;
        }
        this.status = status;
        this.number = number;
    }

    private static String defaultName(KindType type, int number) {
        return String.format("__%s__%d",type.name(),number);
    }
    
    public String getDefaultName() {
        return defaultName(type, number);
    }
    
    public String getFieldName() {
        return field_name;
    }

    public String getModuleName() {
        return module_name;
    }

    public int getNumber() {
        return number;
    }
    
    public Status getStatus() {
        return status;
    }

    public KindName changeNames(String module_namex,String field_namex) {
        return new KindName(type, module_namex, field_namex, status, number);
    }
    
    public KindName exportNames(String module_namex,String field_namex) {
        assert status == Status.PRIVATE;
        return new KindName(type, module_namex, field_namex, Status.EXPORTED, number);
    }

    @Override
    public String toString() {
        return String.format("kind = %s module = %s field = %s status = %s num = %d",
                type,module_name,field_name,status,number);
    }
}
