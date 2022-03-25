package rust;

import java.util.ArrayList;
import java.util.List;

public class Demangle {

    private static String unsanitize(String name) {
        name = name.replace("$SP$", "@");
        name = name.replace("$BP$", "*");
        name = name.replace("$RF$", "&");
        name = name.replace("$LT$", "<");
        name = name.replace("$GT$", ">");
        name = name.replace("$LP$", "(");
        name = name.replace("$RP$", ")");
        name = name.replace("$C$", ",");
        name = name.replace("..", "::");
        name = name.replace(".", "-");
        int index = name.indexOf("$u");
        while (index >= 0 && index < name.length() - 4) {
            if (name.charAt(index + 4) == '$') {
                char unicode = (char)Integer.parseInt(name.substring(index + 2,index + 4),16);
                if (unicode < 0x20 || unicode > 0x7f) {
                    unicode = '_';
                }
                name = name.substring(0, index) + unicode
                         + name.substring(index + 5);
            }
            index = name.indexOf("$u");
        }
//        name = name.replace("$u20$", "\u0020"); // TODO all two digit unicode escapes 
        return name;
    }
    
    private static List<String> split(String mangled) {
        List<String> names = new ArrayList<>();
        if (mangled.startsWith("_ZN") && mangled.endsWith("E")) {
            mangled = mangled.substring(3, mangled.length() - 1);
            while(!mangled.isEmpty()) {
                int length = 0;
                for (int i = 0; i < mangled.length();++i) {
                    char c = mangled.charAt(i); 
                    if (!Character.isDigit(c)) {
                        length = Integer.valueOf(mangled.substring(0,i));
                        String name = mangled.substring(i, i + length);
                        name = unsanitize(name);
                        names.add(name);
                        mangled = mangled.substring(i + length);
                        break;
                    }
                }
            }
            String hash = names.get(names.size() - 1); // remove hash code but needed to avoid duplicates
            if (hash.charAt(0) != 'h') {
                System.err.format("hash begins with %c%n", hash.charAt(0));
                System.exit(1);
            }
        }
        return names;
    }
    
    private static final String V0PREFIX = "_R";
    
    private static String demangleV0(String mangled) {
        assert mangled.startsWith(V0PREFIX);
        return mangled;
    }
    
    public static String demangle(String mangled) {
        if (mangled.startsWith(V0PREFIX)) {
            return demangleV0(mangled);
        }
        List<String> names = split(mangled);
        if (names.isEmpty()) return mangled;
        StringBuilder sb = new StringBuilder();
        for (String name:names) {
            sb.append(' ');
            sb.append(name);
        }
        String result = sb.toString();
        return result.substring(1);
    }
    
    public static String getMethodName(String mangled) {
        if (mangled.startsWith(V0PREFIX)) {
            return demangleV0(mangled);
        }
        List<String> names = split(mangled);
        if (names.size() >= 2) {
            return names.get(names.size() - 2) + "_" + names.get(names.size() - 1);
        } else {
            return mangled;
        }
    }
}
