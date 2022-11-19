package util;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class BasicFormatter extends SimpleFormatter {

    public BasicFormatter() {}

    @Override
    public String format(LogRecord record) {
        String s = super.format(record);
        int index = s.indexOf('\n');
        return s.substring(index + 1);
    }

}
