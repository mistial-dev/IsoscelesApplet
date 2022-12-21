package applet;

import javacard.framework.*;
import javacard.security.RandomData;
import javacardx.apdu.ExtendedLength;

public class IsoscelesApplet extends Applet implements ExtendedLength
{
	/**
	 * Used to indicate that there is no offset (in other words, the offset is zero)
	 */
	public static final short OFFSET_NONE = (short)0x00;

	/**
	 * Used to get a data object from an elementary file or dedicated file
	 */
	private static final byte INS_GET_DATA = (byte)0xCB;

	/**
	 * Used to store a data object within an elementary file or dedicated file
	 */
	private static final byte INS_PUT_DATA = (byte)0xDB;

	/**
	 * Certain values starting with 0x3F are reserved in ISO7816-4.
	 */
	protected static final byte ISO7816_FILE_RESERVED_P1 = (byte)0x3F;

	/**
	 * Used with the reserved P1 value to indicate the current dedicated file
	 */
	private static final byte ISO7816_CURRENT_DEDICATED_FILE_P2 = (byte)0xFF;

	/**
	 * Used with the reserved P1 value to indicate the master file
	 */
	private static final byte ISO7816_MASTER_FILE_P2 = (byte)0x00;

	/**
	 * This Data Object Tag indicates a Private (non-ISO7816), primitive (doesn't contain other tags) object.
	 */
	private static final byte[] TAG_DO_ATS = new byte[] {(byte)0xDE};

	/**
	 * File control template.  See ISO/IEC 7816-4:2020(E) 7.4.2
	 */
	private static final byte[] TAG_FCI_TEMPLATE = {(byte)0x6F};

	/**
	 * File identifier tag.  See ISO/IEC 7816-4:2020(E) 7.4.3
	 */
	private static final byte[] TAG_FCI_FILE_IDENTIFIER = {(byte)0x83};

	/**
	 * Dedicated File Name tag.  See ISO/IEC 7816-4:2020(E) 7.4.3
	 */
	private static final byte[] TAG_FCI_DF_NAME = {(byte)0x84};

	/**
	 * Defines the maximum number of Data Objects the Master File can hold.
	 */
	private static final short MF_MAX_DATA_OBJECTS = 8;

	/**
	 * Transitory buffer size
	 */
	private static final short FIXED_TEMPORARY_BUFFER_SIZE = 64;

	/**
	 * Transitory buffer, used for writing small amounts of data back and forth.
	 */
	private byte[] fixedTemporaryBuffer = JCSystem.makeTransientByteArray(FIXED_TEMPORARY_BUFFER_SIZE, JCSystem.CLEAR_ON_DESELECT);

	/**
	 * Contains the Application Identifier (AID) the applet was installed with
	 */
	private byte[] applicationId;

	/**
	 * RNG used by the applet
	 */
	private final RandomData random;

	/**
	 * Master file, as defined by ISO7816-4.  Automatically selected when the applet is selected.
	 */
	private final DedicatedFile masterFile;

	/**
	 * Currently selected Dedicated File (DF)
	 */
	private DedicatedFile currentDedicatedFile;

	/**
	 * Currently selected Application Dedicated File (ADF)
	 */
	private DedicatedFile currentApplicationDedicatedFile;

	/**
	 * Called when the applet is installed
	 * @param bArray Byte array containing the Applet ID, Applet Information and Applet Install Parameters
	 * @param bOffset Offset within the buffer for the Applet information
	 * @param bLength Length of the parameter data in bArray
	 */
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new IsoscelesApplet(bArray, bOffset, bLength);
	}

	/**
	 * Isosceles ISO7816-4 pplet
	 * @param buffer Installation details and parameters
	 * @param offset Offset for installation details and parameters
	 * @param length Length of installation details and parameters
	 */
	public IsoscelesApplet(byte[] buffer, short offset, byte length)
	{
		// Save the Application ID
		byte applicationIdLen = buffer[offset];
		applicationId = new byte[applicationIdLen];
		Util.arrayCopy(buffer, (short)(offset+1), applicationId, OFFSET_NONE, applicationIdLen);

		// Allocate a new master file and provide the Application ID
		masterFile = new DedicatedFile(MF_MAX_DATA_OBJECTS, new byte[]{ISO7816_FILE_RESERVED_P1, ISO7816_MASTER_FILE_P2}, applicationId);

		// Set file target pointers
		currentDedicatedFile = masterFile;
		currentApplicationDedicatedFile = masterFile;

		// Initialize a reference to the RNG
		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		register();
	}

	/**
	 * Process an incoming Application Protocol Data Unit (APDU)
	 * @param apdu APDU to Process
	 */
	public void process(APDU apdu)
	{
		byte[] apduBuffer = apdu.getBuffer();
		byte ins = apduBuffer[ISO7816.OFFSET_INS];

		// We only support inter-industry classes
		if (!apdu.isISOInterindustryCLA()) {
				ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		// If the applet is being selected, send the ATS for the master file
		if (selectingApplet()) {
			SendFci(apdu, masterFile);
			return;
		} else if (ins == INS_PUT_DATA) {
			HandlePutData(apdu);
			return;
		}

		ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
	}

	/**
	 * Put Data command (ISO7816-4).  Puts a data object into a specified data file.
	 * @param apdu APDU to process
	 */
	public void HandlePutData(APDU apdu) {
		// Only support the current dedicated file
		byte[] apduBuffer = apdu.getBuffer();
		byte cla = apduBuffer[ISO7816.OFFSET_CLA];
		byte p1 = apduBuffer[ISO7816.OFFSET_P1];
		byte p2 = apduBuffer[ISO7816.OFFSET_P2];

		// Get the data parameters
		short recvLen = apdu.setIncomingAndReceive();
		short lc = apdu.getIncomingLength();
		short dataOffset = apdu.getOffsetCdata();

		// Check parameters
		if (cla != ISO7816.CLA_ISO7816) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		// Identify the target file
		DedicatedFile target = null;
		if (p1 == ISO7816_FILE_RESERVED_P1) {
			if (p2 == ISO7816_MASTER_FILE_P2) {
				target = masterFile;
			} else if (p2 == ISO7816_CURRENT_DEDICATED_FILE_P2) {
				target = currentDedicatedFile;
			}
		}
		if (target == null) {
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}

		// Read the extended APDU into transient memory
		short dataBufferOffset = 0;
		byte[] dataBuffer = JCSystem.makeTransientByteArray(recvLen, JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT);
		while (recvLen > 0) {
			Util.arrayCopyNonAtomic(apduBuffer, dataOffset, dataBuffer, dataBufferOffset, recvLen);
			dataBufferOffset += recvLen;
			recvLen = apdu.receiveBytes(dataOffset);
		}

		// Reset the data buffer for parsing
		dataBufferOffset = 0;

		// Loop through all the target tags
		while (dataBufferOffset < dataBuffer.length) {
			// Read the tag length
			short tagSize = DerParser.ReadTagSize(dataBuffer, dataBufferOffset);
			short lengthSize = DerParser.ReadLengthSize(dataBuffer, (short)(dataBufferOffset + tagSize));
			short tagLength = DerParser.ReadTagLength(dataBuffer, (short)(dataBufferOffset + tagSize));

			// Allocate the new data object for storage
			byte[] newData = new byte[tagLength];
			Util.arrayCopyNonAtomic(dataBuffer, (short) (dataBufferOffset + tagSize + lengthSize), newData, OFFSET_NONE, tagLength);
			target.putData(dataBuffer, dataBufferOffset, tagSize, newData);

			dataBufferOffset += tagSize + lengthSize + tagLength;
		}


		ISOException.throwIt(ISO7816.SW_NO_ERROR);
	}

	/**
	 * Send the ATS response contained in element 0xDE.
	 * @param apdu
	 */
	public void sendAts(APDU apdu, ElementaryFile elementaryFile) {
		byte[] apduBuffer = apdu.getBuffer();
		byte cla = apduBuffer[ISO7816.OFFSET_CLA];
		short le = apdu.setOutgoing();

		byte[] ats = elementaryFile.getData(TAG_DO_ATS);

		// Ensure the ATS buffer can be output, and that the class is correct
		if (le != (short)0x00 && le < ats.length) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		} else if (cla != ISO7816.CLA_ISO7816) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		apdu.setOutgoingLength((short)ats.length);

		// Put the ATS value into the buffer
		Util.arrayCopyNonAtomic(ats, OFFSET_NONE, apduBuffer, OFFSET_NONE, (short)ats.length);
		apdu.sendBytes(OFFSET_NONE, (short)ats.length);
		ISOException.throwIt(ISO7816.SW_NO_ERROR);
	}

	/**
	 * Sends file control information.  Used when P2 & 0xFC is zero.
	 * See ISO/IEC 7816 4:2020, Table 63.
	 */
	public void SendFci(APDU apdu, DedicatedFile dedicatedFile) {
		byte[] apduBuffer = apdu.getBuffer();
		byte cla = apduBuffer[ISO7816.OFFSET_CLA];
		short le = apdu.setOutgoing();

		// Dynamically generate the FCI template
		byte[] dfName = dedicatedFile.GetName();
		byte[] fileIdentifier = dedicatedFile.GetFilename();

		// Build the template in fixedTemporaryBuffer
		short len = 0x00;
		short fciLenOffset;

		// [FCI Template Tag] [Le]
		Util.arrayCopy(TAG_FCI_TEMPLATE, OFFSET_NONE, fixedTemporaryBuffer, len, (short)TAG_FCI_TEMPLATE.length);
		len += (short)TAG_FCI_TEMPLATE.length;
		fciLenOffset = len;
		len += (short)1;

		// [File Identifier Tag] [Le] [File Identifier]
		if ((fileIdentifier != null) && Util.makeShort(fileIdentifier[0], fileIdentifier[1]) != 0) {
			Util.arrayCopy(TAG_FCI_FILE_IDENTIFIER, OFFSET_NONE, fixedTemporaryBuffer, len, (short)(TAG_FCI_FILE_IDENTIFIER.length));
			len += (short)TAG_FCI_FILE_IDENTIFIER.length;
			fixedTemporaryBuffer[len] = (byte)(fileIdentifier.length);
			len += (short) 1;
			Util.arrayCopy(fileIdentifier, OFFSET_NONE, fixedTemporaryBuffer, len, (short)(fileIdentifier.length));
			len += (short)(fileIdentifier.length);
		}

		// [DF Name Tag] [Le] [DF Name]
		if ((dfName != null) && dfName.length > 0) {
			Util.arrayCopy(TAG_FCI_DF_NAME, OFFSET_NONE, fixedTemporaryBuffer, len, (short)(TAG_FCI_DF_NAME.length));
			len += (short)(TAG_FCI_DF_NAME.length);
			fixedTemporaryBuffer[len] = (byte)(dfName.length);
			len += (short)1;
			Util.arrayCopy(dfName, OFFSET_NONE, fixedTemporaryBuffer, len, (short)(dfName.length));
			len += (short)(dfName.length);
		}

		// Fixup the length byte
		fixedTemporaryBuffer[fciLenOffset] = (byte)(len - fciLenOffset - 1);

		// Verify the parameters
		if (le != (short)0x00 && le < len) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		} else if (cla != ISO7816.CLA_ISO7816) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		// And send it
		Util.arrayCopy(fixedTemporaryBuffer, OFFSET_NONE, apduBuffer, OFFSET_NONE, len);

		apdu.setOutgoingLength(len);
		apdu.sendBytes(OFFSET_NONE, len);
		ISOException.throwIt(ISO7816.SW_NO_ERROR);
	}
}
