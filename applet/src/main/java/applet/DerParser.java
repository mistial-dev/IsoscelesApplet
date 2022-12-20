package applet;

import javacard.framework.Util;

public class DerParser {
    private static final byte MASK_TAG_MULTIBYTE = (byte)0x1F;
    private static final byte MASK_TAG_THREE_BYTE = (byte)0x80;

    private static final byte MASK_LENGTH_TWO_BYTE = (byte)0x80;

    private static final byte LEN_SINGLE_BYTE = (byte)0x01;
    private static final byte LEN_TWO_BYTE = (byte)0x02;

    private static final byte LEN_THREE_BYTE = (byte)0x03;

    /**
     * Parses a variable-length tag ASN.1 DER tag, returning the length in bytes
     * @param srcBuffer Source buffer to read
     * @param srcOffset Source buffer offset
     * @return Size of the tag
     */
    public static short ReadTagSize (byte[] srcBuffer, short srcOffset) {
        // Short tags will not have all 5 bytes set
        if ((srcBuffer[srcOffset] & MASK_TAG_MULTIBYTE) != MASK_TAG_MULTIBYTE) {
            return LEN_SINGLE_BYTE;
        } else if ((srcBuffer[(short)(srcOffset + 1)] & MASK_TAG_THREE_BYTE) == MASK_TAG_THREE_BYTE) {
            return LEN_THREE_BYTE;
        }
        return LEN_TWO_BYTE;
    }

    /**
     * Read the size of a variable-length ASN.1 DER field
     * @param srcBuffer Source buffer to read
     * @param srcOffset Source buffer offset
     * @return Size of the length
     */
    public static short ReadLengthSize (byte[] srcBuffer, short srcOffset) {
        if (MASK_LENGTH_TWO_BYTE == (srcBuffer[srcOffset] & MASK_LENGTH_TWO_BYTE)) {
            return LEN_TWO_BYTE;
        }
        return LEN_SINGLE_BYTE;
    }

    /**
     * Read the length of an ASN.1 DER length tag (up to two bytes)
     * @param srcBuffer Source buffer to read
     * @param srcOffset Source buffer offset
     * @return Length of the ASN.1 DER tag
     */
    public static short ReadTagLength(byte[] srcBuffer, short srcOffset) {
        short size = ReadLengthSize(srcBuffer, srcOffset);

        if (size == LEN_SINGLE_BYTE) {
            return Util.makeShort((byte)0x00, srcBuffer[srcOffset]);
        }
        return Util.makeShort(srcBuffer[srcOffset], srcBuffer[(short)(srcOffset + 1)]);
    }
}
