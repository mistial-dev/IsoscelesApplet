package main;

import applet.IsoscelesApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javax.smartcardio.*;

public class Run {
    public static void main(String[] args){
        // 1. create simulator
        CardSimulator simulator = new CardSimulator();

        // 2. install applet
        AID appletAID = AIDUtil.create("F000000001");
        simulator.installApplet(appletAID, IsoscelesApplet.class);

        // 3. select applet
        simulator.selectApplet(appletAID);

        // 4. send APDU
        CommandAPDU commandAPDU = new CommandAPDU(0x00, 0x90, 0x00, 0x00);
        ResponseAPDU response = simulator.transmitCommand(commandAPDU);

        System.out.println(new String(response.getData()));
    }

}