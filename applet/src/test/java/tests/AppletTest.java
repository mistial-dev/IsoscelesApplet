package tests;

import cardTools.CardManager;
import cardTools.CardType;
import org.junit.jupiter.api.*;

import javax.smartcardio.ResponseAPDU;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author xsvenda, Dusan Klinec (ph4r05)
 */
public class AppletTest extends BaseTest {
    /**
     * Initialize the test
     */
    public AppletTest() {
        setCardType(CardType.JCARDSIMLOCAL);
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUpMethod() {
        // Don't reset every time
        setSimulateStateful(true);
    }

    @AfterEach
    public void tearDownMethod() {
    }

    /**
     * Ensure that the answer to select is compliant with ISO7816-4, table 63.
     */
    @Test
    public void testAppletAts() throws Exception {
        // [Len] [AID] [Len] [Info] [Len] [Data]
        byte[] installParams = new byte[] {(byte)0x0B, (byte)0x0F,(byte)0x49,(byte)0x53,(byte)0x4F,(byte)0x53,(byte)0x43,(byte)0x45,(byte)0x4C,(byte)0x45,(byte)0x53,(byte)0x01, (byte)0x00, (byte)0x00};

        // 6F [11]
        //    83 [02] 3F 00
        //    84 [0B] 0F 49 53 4F 53 43 45 4C 45 53 01
        byte[] validAnswerToSelect = new byte[] {
                (byte)0x6f, (byte)0x11, (byte)0x83, (byte)0x02, (byte)0x3f, (byte)0x00,
                (byte)0x84, (byte)0x0b, (byte)0x0f, (byte)0x49, (byte)0x53, (byte)0x4f,
                (byte)0x53, (byte)0x43, (byte)0x45 ,(byte)0x4c, (byte)0x45, (byte)0x53,
                (byte)0x01, (byte)0x90, (byte)0x00
        };

        // Pass install parameters to card manager
        CardManager manager = connect(installParams);

        // Answer to select should be an FCI structure
        ResponseAPDU atsResponse = manager.selectApplet();
        Assertions.assertNotNull(atsResponse);
        Assertions.assertEquals(0x9000, atsResponse.getSW());
        Assertions.assertNotNull(atsResponse.getBytes());
        Assertions.assertArrayEquals(validAnswerToSelect, atsResponse.getBytes());
    }
}
