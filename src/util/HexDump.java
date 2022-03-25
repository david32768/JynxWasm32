package util;

import java.nio.ByteBuffer;

public class HexDump {

    private static final int WIDTH = 16;
    
    public static String printHex(ByteBuffer bb,long start) {
        StringBuilder sb = new StringBuilder();
        while(bb.hasRemaining()) {
            int pos = bb.position();
            int width = Math.min(WIDTH, bb.remaining());
            sb.append(String.format("%08x   ",bb.position() + start));
            for (int j = 0; j < width;j++) {
                if (j != 0 && j % 4 == 0) sb.append(" ");
                byte bj = bb.get();
                String bs = String.format("%02x", Byte.toUnsignedInt(bj));
                sb.append(bs);
            }
            sb.append(" ");
            bb.position(pos);
            for (int j = 0; j < width;j++) {
                byte bj = bb.get();
                if (bj < 0x20 || bj > 0x7f) bj = '.';
                sb.append((char)bj);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
}
