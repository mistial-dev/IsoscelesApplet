package applet;

public class DedicatedFile extends ElementaryFile {
    public static byte MAX_CHILDREN = 0x08;

    /**
     * Array of all the child files this file contains
     */
    public ElementaryFile[] _children;

    /**
     * Application Name
     */
    private byte[] _applicationName;

    /**
     * Initialize a new Dedicated File array
     *
     * @param maxObjects Maximum number of objects this DF can contain
     * @param fileIdentifier   File identifier for this DF
     * @param applicationName Application Name [AID] for this DF
     */
    public DedicatedFile(short maxObjects, byte[] fileIdentifier, byte[] applicationName) {
        super(maxObjects, (short)0x00, fileIdentifier);
        if (applicationName != null) {
            _applicationName = applicationName;
        }
        _children = new ElementaryFile[MAX_CHILDREN];
    }

    /**
     * Get the application identifier (AID) for a dedicated file
     * @return Application Identifier
     */
    public byte[] GetName() {
        return this._applicationName;
    }
}
