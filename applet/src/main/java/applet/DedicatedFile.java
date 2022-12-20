package applet;

public class DedicatedFile extends ElementaryFile {
    public static byte MAX_CHILDREN = 0x08;

    /**
     * Array of all the child files this file contains
     */
    public ElementaryFile[] _children;

    /**
     * Initialize a new Dedicated File array
     *
     * @param maxObjects Maximum number of objects this DF can contain
     * @param filename   Filename for this DF
     */
    public DedicatedFile(short maxObjects, byte[] filename) {
        super(maxObjects, (short)0x00, filename);
        _children = new ElementaryFile[MAX_CHILDREN];
    }
}
