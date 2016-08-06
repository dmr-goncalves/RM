/*
 * SensorSampler.java
 *
 * Copyright (c) 2008-2010 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.util.Utils;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This application is the 'on SPOT' portion of the SendDataDemo. It
 * periodically samples a sensor value on the SPOT and transmits it to a desktop
 * application (the 'on Desktop' portion of the SendDataDemo) where the values
 * are displayed.
 *
 * @author: Vipul Gupta modified: Ron Goldman
 */
public class SensorSampler extends MIDlet {

    private static final int HOST_PORT = 67;
    private static final int SAMPLE_PERIOD = 1 * 1000;  // in milliseconds
    public int media = 0;

    protected void startApp() throws MIDletStateChangeException {
        RadiogramConnection rCon = null;
        RadiogramConnection rCon2 = null;
        Radiogram dg, dg2;
        dg = null;
        dg2 = null;
        int pot = 0;
        String ourAddress = System.getProperty("IEEE_ADDRESS");
        ILightSensor lightSensor = (ILightSensor) Resources.lookup(ILightSensor.class);
        ITriColorLED led1 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED1");
        ITriColorLED led2 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED2");
        ITriColorLED led3 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED3");
        ITriColorLED led4 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED4");
        ITriColorLED led5 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED5");
        ITriColorLED led6 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED6");
        ITriColorLED led7 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED7");
        ITriColorLED led8 = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED8");
        System.out.println("Starting sensor sampler application on " + ourAddress + " ...");
        int ant_seq = -1;
        int count = 0;
        int mediaint = 0;

        // Listen for downloads/commands over USB connection
        new com.sun.spot.service.BootloaderListenerService().getInstance().start();

        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
            dg = (Radiogram) rCon.newDatagram(50);  // only sending 12 bytes of data

            rCon2 = (RadiogramConnection) Connector.open("radiogram://broadcast:52");

            dg2 = (Radiogram) rCon2.newDatagram(rCon2.getMaximumLength());
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
            notifyDestroyed();
        }

        while (true) {
            try {
                rCon.receive(dg);
                dg.getAddressAsLong();

                // Get the current time and sensor reading
                long now = System.currentTimeMillis();
                int reading = lightSensor.getValue();

                long inst = dg.readLong();
                int num_ant = dg.readInt();

                int seq = dg.readInt();
                if (seq != ant_seq + 1) {
                    led7.setColor(LEDColor.RED);
                    led6.setColor(LEDColor.RED);
                    led5.setColor(LEDColor.RED);
                    led4.setColor(LEDColor.RED);
                    led8.setColor(LEDColor.RED);

                    led7.setOn();
                    led6.setOn();
                    led5.setOn();
                    led4.setOn();
                    led8.setOn();

                }
                ant_seq = seq;
                if (ant_seq > 1) {
                    ant_seq = -1;
                }

                // Flash an LED to indicate a sampling event
                if (num_ant == 41039) {
                    led1.setColor(LEDColor.WHITE);
                }
                if (num_ant == 41040) {
                    led1.setColor(LEDColor.GREEN);
                }
                if (num_ant == 41041) {
                    led1.setColor(LEDColor.RED);
                }
                led1.setOn();
                Utils.sleep(50);
                led1.setOff();

                int i;

                pot = dg.getRssi();

                //for (pot = 0; pot < 60; pot++) {

                    if (pot <= 60 && pot > 48) {
                        led7.setColor(LEDColor.GREEN);
                        led6.setColor(LEDColor.GREEN);
                        led5.setColor(LEDColor.GREEN);
                        led4.setColor(LEDColor.GREEN);
                        led8.setColor(LEDColor.GREEN);

                        led7.setOn();
                        led6.setOn();
                        led5.setOn();
                        led4.setOn();
                        led8.setOn();
                        Utils.sleep(50);
                    }
                    if (pot <= 48 && pot > 36) {
                        led6.setColor(LEDColor.GREEN);
                        led5.setColor(LEDColor.GREEN);
                        led4.setColor(LEDColor.GREEN);
                        led8.setColor(LEDColor.GREEN);

                        led7.setOn();
                        led6.setOn();
                        led5.setOn();
                        led4.setOn();
                        led8.setOff();
                        Utils.sleep(50);

                    }
                    if (pot <= 36 && pot > 24) {
                        led5.setColor(LEDColor.GREEN);
                        led4.setColor(LEDColor.GREEN);
                        led3.setColor(LEDColor.GREEN);

                        led7.setOff();
                        led6.setOn();
                        led5.setOn();
                        led4.setOn();
                        led8.setOff();
                        Utils.sleep(50);
                    }
                    if (pot <= 24 && pot > 12) {
                        led4.setColor(LEDColor.GREEN);
                        led3.setColor(LEDColor.GREEN);

                        led7.setOff();
                        led6.setOff();
                        led5.setOn();
                        led4.setOn();
                        led8.setOff();
                        Utils.sleep(50);
                    }
                    if (pot <= 12 && pot > 0) {
                        led4.setColor(LEDColor.GREEN);

                        led7.setOff();
                        led6.setOff();
                        led5.setOff();
                        led4.setOn();
                        led8.setOff();
                        Utils.sleep(50);
                    }
                    if (pot <= 0 && pot > -12) {

                        led8.setColor(LEDColor.GREEN);
                        led7.setColor(LEDColor.GREEN);
                        led6.setColor(LEDColor.GREEN);
                        led5.setColor(LEDColor.GREEN);

                        led7.setOn();
                        led6.setOn();
                        led5.setOn();
                        led4.setOff();
                        led8.setOn();
                        Utils.sleep(50);

                    }
                    if (pot <= -12 && pot > -24) {
                        led7.setColor(LEDColor.GREEN);
                        led6.setColor(LEDColor.GREEN);

                        led8.setColor(LEDColor.GREEN);


                        led7.setOn();
                        led6.setOn();
                        led5.setOff();
                        led4.setOff();
                        led8.setOn();
                        Utils.sleep(50);
                    }
                    if (pot <= -24 && pot > -36) {
                        led7.setColor(LEDColor.GREEN);
                        led8.setColor(LEDColor.GREEN);
                        led5.setColor(LEDColor.GREEN);


                        led7.setOn();
                        led6.setOff();
                        led5.setOff();
                        led4.setOff();
                        led8.setOn();
                        Utils.sleep(50);
                    }
                    if (pot <= -36 && pot > -48) {
                        led8.setColor(LEDColor.GREEN);



                        led8.setOn();
                        led6.setOff();
                        led5.setOff();
                        led4.setOff();
                        Utils.sleep(50);

                    }
                    if (pot <= -48 && pot > -60) {
                        led7.setColor(LEDColor.GREEN);


                        led7.setOff();
                        led6.setOff();
                        led5.setOff();
                        led4.setOff();
                        led3.setOff();
                        Utils.sleep(50);


                    }
                    mediaint += pot;
                    if (count == 4) {
                   
                        media = mediaint / 5;
                        dg2.reset();
                        dg2.writeLong(inst);
                        dg2.writeInt(media);
                        rCon2.send(dg2);
                      
                      
                        mediaint = 0;
                        count = 0;
                    } else {
                        count++;
                    }
             //   }



                // Go to sleep to conserve battery
                Utils.sleep(SAMPLE_PERIOD);
            } catch (Exception e) {
                System.err.println("Caught " + e + " while collecting/sending sensor sample.");
            }
        }
    }

    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }
}
