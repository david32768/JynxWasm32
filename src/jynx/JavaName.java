package jynx;

import java.util.stream.Collectors;
import parse.Kind;

public class JavaName {

    private final String env;

    private int envChenges = 0;
    
    public JavaName(String env) {
        this.env = env;
    }

    private static String changeInvalidChar(Integer I) {
        int i = I;
        if (Character.isJavaIdentifierPart(i)) {
            return String.valueOf((char)i);
        } else if (i == '-') {
            return "_";
        } else if (i == '.') {
            return "$DOT$";
        } else {
            return "$" + Integer.toHexString(i) + "$";
        }
    }
    
    public String simpleName(String name) {
        if (!Character.isJavaIdentifierStart(name.codePointAt(0))) {
            name = "_"+ name;
        }
        return name
                .codePoints()
                .boxed()
                .map(JavaName::changeInvalidChar)
                .collect(Collectors.joining());
    }

    public String compoundName(String name) {
        String[] parts = name.split("/");
        StringBuilder sb = new StringBuilder(name.length());
        boolean first = true;
        for (String part:parts) {
            if (first) {
                first = false;
            } else {
                sb.append("/");
            }
            sb.append(simpleName(part));
        }
        return sb.toString();
    }
    private String ownerName(Kind kind) {
        String name = kind.getModuleName();
        if(env != null && name.equals("env")) {
            name = env;
            ++envChenges;
        }
        return compoundName(name);
    }
    
    public String of(Kind kind) {
        String module = ownerName(kind);
        String field = simpleName(kind.getFieldName());
        String name = module + "/" + field;
        return name;
    }

    public String simpleName(Kind kind) {
        return JavaName.this.simpleName(kind.getFieldName());
    }
    
    public String localName(Kind kind) {
        if (kind.isImported()) {
            return of(kind);
        } else {
            return simpleName(kind);
        }
    }
    
    public static boolean is(String name) {
        JavaName javaname = new JavaName(null);
        return javaname.compoundName(name).equals(name);
    }
}
