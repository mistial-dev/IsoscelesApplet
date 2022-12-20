package applet;

import javacard.framework.Util;

import static applet.IsoscelesApplet.OFFSET_NONE;

public class DataObject {
    private byte[] _tag;

    private byte[] _data;

    /**
     * Instantiates a new Data Object
     * @param tagBuffer Tag input buffer
     * @param tagOffset Tag offset
     * @param tagLength Tag Length
     */
    public DataObject(byte[] tagBuffer, short tagOffset, short tagLength) {
        _tag = new byte[tagLength];
        Util.arrayCopy(tagBuffer, tagOffset, _tag, OFFSET_NONE, tagLength);
    }

    public byte[] getTag() {
        return _tag;
    }

    public byte[] getData() {
        return _data;
    }

    public void setData(byte[] data) {
        this._data = data;
    }

    public boolean tagEquals(byte[] buffer, short tagOffset, short tagLength) {
        // Check the length first
        if (tagLength != _tag.length) {
            return false;
        }
        return (Util.arrayCompare(buffer, tagOffset, _tag, OFFSET_NONE, tagLength) == 0) ? true : false;
    }
}
