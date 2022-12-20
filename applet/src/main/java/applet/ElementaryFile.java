package applet;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;

import static applet.IsoscelesApplet.OFFSET_NONE;

public class ElementaryFile {
    /**
     * Used to indicate a null/unknown slot
     */
    public static final short SLOT_NONE = -1;

    /**
     * Array of all the data objects contained in a dedicated file
     */
    public DataObject[] _dataObjects;

    /**
     * Filename for this dedicated file
     */
    private byte[] _fileName;

    /**
     * Initialize a new Dedicated File array
     * @param maxObjects Maximum number of objects this EF can contain
     * @param maxChildren Maximum number of children this EF can have.  Must be zero.
     * @param filename Filename for this DF
     */
    public ElementaryFile (short maxObjects, short maxChildren, byte[] filename) {
        if (maxChildren != 0) {
            ISOException.throwIt(ISO7816.SW_UNKNOWN);
        }
        _dataObjects = new DataObject[maxObjects];
        _fileName = filename;
    }

    /**
     * Get the value of a data object from within a file
     * @param tag Byte array representing the tag for a data object
     * @return
     */
    public byte[] getData(byte[] tag) {
        return getData(tag, OFFSET_NONE, (short)tag.length);
    }

    /**
     * Get the value of a data object from within a file
     * @param tagBuffer Tag data buffer
     * @param tagOffset Tag data offset
     * @param tagLength Tag data length
     * @return Byte array containing the tag data
     */
    public byte[] getData (byte[] tagBuffer, short tagOffset, short tagLength) {
        for (short i = 0; i < _dataObjects.length; i++) {
            if (_dataObjects[i] == null) {
                continue;
            } else if (_dataObjects[i].tagEquals(tagBuffer, tagOffset, tagLength)) {
                return _dataObjects[i].getData();
            }
        }

        return null;
    }

    /**
     * Place a data object within a files
     * @param dataObject
     */
    public void putData(DataObject dataObject) {
        byte[] tag = dataObject.getTag();
        putData (tag, OFFSET_NONE, (short)tag.length, dataObject.getData());
    }


    /**
     * Put data into a data object
     * @param buffer Source Buffer for the tag
     * @param tagOffset Source Buffer offset for the tag
     * @param tagLength Length of the tag
     * @param data Data for the data object
     */
    public void putData(byte[] buffer, short tagOffset, short tagLength, byte[] data) {
        short openSlot = SLOT_NONE;

        // Look for the tag, and notate the first open slot (if any)
        for (short i = 0; i < _dataObjects.length; i++) {
            // If the tag exists, update it
            if (_dataObjects[i] == null) {
                openSlot = i;
                break;
            } else if (_dataObjects[i].tagEquals(buffer, tagOffset, tagLength)) {
                _dataObjects[i].setData(data);
                JCSystem.requestObjectDeletion();
                return;
            }
        }

        // Since the tag was not found, create it.
        if (openSlot == SLOT_NONE) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        DataObject newObject = new DataObject(buffer, tagOffset, tagLength);
        newObject.setData(data);
        _dataObjects[openSlot] = newObject;
    }
}
