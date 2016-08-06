/*
 */
package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;

import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import javax.microedition.io.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * This application is the 'on Desktop' portion of the SendDataDemo. This host
 * application collects sensor samples sent by the 'on SPOT' portion running on
 * neighboring SPOTs and graphs them in a window.
 *
 */
public class SendDataDemoGuiHostApplication {
    // Broadcast port on which we listen for sensor samples

    private static final int HOST_PORT = 67;
    private JTextArea status;
    private long[] addresses = new long[8];
    private DataWindow[] plots = new DataWindow[8];
    private static final int SAMPLE_PERIOD = 1 * 1000;  // in milliseconds
    long inst=0;
    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        status = new JTextArea();
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = 0;
            plots[i] = null;
        }
    }

    private DataWindow findPlot(long addr) {
        for (int i = 0; i < addresses.length; i++) {
            if (addresses[i] == 0) {
                String ieee = IEEEAddress.toDottedHex(addr);
                status.append("Received packet from SPOT: " + ieee + "\n");
                addresses[i] = addr;
                plots[i] = new DataWindow(ieee);
                final int ii = i;
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        plots[ii].setVisible(true);
                    }
                });
                return plots[i];
            }
            if (addresses[i] == addr) {
                return plots[i];
            }
        }
        return plots[0];
    }

    private void run() throws Exception {
        RadiogramConnection rCon;
        Radiogram dg;

        try {
            // Open up a server-side broadcast radiogram connection
            // to listen for sensor readings being sent by different SPOTs
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);

            dg = (Radiogram) rCon.newDatagram(rCon.getMaximumLength());
           Thread_base base = new Thread_base();
               base.start();

        } catch (Exception e) {
            System.err.println("setUp caught " + e.getMessage());
            throw e;
        }

        status.append("Sending...\n");
        int num = 41038;
        int num_ant = 0;
       
        int seq = 0;
        // Main data collection loop
        while (true) {
            try {
                inst = System.currentTimeMillis();
             
                if (seq > 2) {
                    num = 41038;
                    seq = 0;
                }
            num = num + 1;
             
                dg.reset();

                dg.writeLong(inst);
               
                dg.writeInt(num);

                dg.writeInt(seq);

                rCon.send(dg);

                seq++;

            
              Utils.sleep(SAMPLE_PERIOD);

            } catch (Exception e) {
                System.err.println("Caught " + e + " while reading sensor samples.");
                throw e;
            }
        }
    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        OTACommandServer.start("SendDataDemo-GUI");

        SendDataDemoGuiHostApplication app = new SendDataDemoGuiHostApplication();
        app.setup();
        app.run();
    }

    public class Thread_base extends Thread {

        public void run() {
            RadiogramConnection rCon = null;
            Radiogram dg = null;


            try {
                // Open up a broadcast connection to the host port
                // where the 'on Desktop' portion of this demo is listening
                rCon = (RadiogramConnection) Connector.open("radiogram://:52");
                dg = (Radiogram) rCon.newDatagram(50);  // only sending 12 bytes of data
            } catch (Exception e) {
                System.err.println("Caught " + e + " in connection initialization.");

            }

            while (true) {
                try {
                    rCon.receive(dg);
                    dg.getAddressAsLong();

                   
                    DataWindow dw= findPlot(dg.getAddressAsLong());
                    long graph_time= dg.readLong();
                     int media = dg.readInt();
                     dw.addData(graph_time,media);
                     System.out.println("media: "+media);

                } catch (Exception e) {
                    System.err.println("Caught " + e + " while collecting/sending Base sample.");
                }
            }

        }
    }
}
