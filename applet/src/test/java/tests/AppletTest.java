package tests;

import cardTools.CardManager;
import cardTools.CardType;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author xsvenda, Dusan Klinec (ph4r05)
 */
public class AppletTest extends BaseTest {

    public AppletTest() {
        setCardType(CardType.JCARDSIMLOCAL);
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUpMethod() throws Exception {
        setSimulateStateful(true);
    }

    @AfterEach
    public void tearDownMethod() throws Exception {
    }

    // Test ATS
    @Test
    public void testAts() throws Exception {
        CardManager manager = connect();

        // The first time the answer to select is provided, it should be empty.
        ResponseAPDU atsResponse = manager.selectApplet();
        Assertions.assertNotNull(atsResponse);
        Assertions.assertEquals(0x9000, atsResponse.getSW());
        Assertions.assertNotNull(atsResponse.getBytes());
        byte[] expectedResponse = new byte[]{(byte) 0x90, (byte) 0x00};
        Assertions.assertArrayEquals(expectedResponse, atsResponse.getBytes());

        // Put data new ATS (and a throwaway tag)
        final byte TAG_DO_ATS = (byte)0xDE;
        final byte[] TAG_DO_TEST = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0x01};
        byte[] testPutATS = new byte[]{TAG_DO_TEST[0], TAG_DO_TEST[1], TAG_DO_TEST[2], (byte) 0x02, (byte) 0xFF, (byte) 0xFF, TAG_DO_ATS, 0x08, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37};
        final CommandAPDU putDataCommand = new CommandAPDU(0x00, 0xDB, 0x3F, 0x00, testPutATS);
        final ResponseAPDU putDataResponse = manager.transmit(putDataCommand);
        Assertions.assertNotNull(putDataResponse);
        Assertions.assertEquals(0x9000, putDataResponse.getSW());
        Assertions.assertNotNull(putDataResponse.getBytes());

        // Verify new ATS
        manager.disconnect(false);
        ResponseAPDU atsResponse2 = manager.selectApplet();
        Assertions.assertNotNull(atsResponse2);
        Assertions.assertEquals(0x9000, atsResponse2.getSW());
        Assertions.assertNotNull(atsResponse2.getBytes());
        expectedResponse = new byte[]{(byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x90, (byte) 0x00};
        Assertions.assertArrayEquals(expectedResponse, atsResponse2.getBytes());
    }
}
