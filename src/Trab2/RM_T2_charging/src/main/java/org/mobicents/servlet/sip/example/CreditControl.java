/*
 * RM - segundo trabalho
 *
 *      Charging in IMS
 *
 *  Rodolfo Oliveira
 *   rado@fct.unl.pt
 *
 */
package org.mobicents.servlet.sip.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import java.awt.event.*;
import java.util.Timer;
import org.apache.log4j.Logger;

/**
 * 
 * CreditControl.java
 *
 * 
 */
public class CreditControl {

    private String user;    // identifies the user
    private Date date_off;  // date when a given user is unregistered
    private float credit;  // ammount of credit
    private boolean is_registered; // controls if user is registered
    public javax.swing.Timer timerCharge;
    long timerDate;
    int state; // o - called ; 1 - received
    String from, to;
    public static Logger logger = Logger.getLogger(CreditControl.class);

    public CreditControl(String user, Date date) {
        this.is_registered = true;
        this.credit = 1000;
        this.user = user;
        this.date_off = date;//new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss").format(date);
    }

    public void chargeTimer(int duration) {
        timerCharge = new javax.swing.Timer(duration, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (state == 0) {
                    subCredit(20);
                    if (credit == 0) {
                        DiameterOpenIMSSipServlet.sendSIPMessage(from, "Lack of Credit");
                    }

                } else {
                    subCredit(4);
                    if (credit == 0) {
                        DiameterOpenIMSSipServlet.sendSIPMessage(to, "Lack of Credit");
                    }
                }
                Date now = new Date();
                timerDate = now.getTime();
            }
        });
        Date now = new Date();
        timerDate = now.getTime();
        timerCharge.setRepeats(true);
        timerCharge.start();
    }

    public void stopChargeTimer() {
        if (timerCharge != null) {
            timerCharge.stop();
            timerCharge = null;
        }
    }

    public String getNotification() {
        return "Dear " + this.user + ", your credit is " + this.credit + ".";


    }

    @Override
    public int hashCode() {
        //return (callee + date).hashCode();
        return (user).hashCode();


    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof CreditControl) {
            CreditControl other = (CreditControl) obj;
            //return this.callee.equals(other.callee) && this.date.equals(other.date);


            return this.user.equals(other.user);


        }
        return false;


    }

    public float getCredit() {
        return credit;


    }

    public float subCredit(float value) {
        if (credit > 0) {
            credit = credit - value;


        }
        if (credit < 0) {
            credit = 0;


        }
        return credit;


    }

    public String getUser() {
        return this.user;


    }

// update credit when the user does the DEregister
    public void setDate_off(Date d) {
        this.date_off = d;


        this.is_registered = false;


    }

// update credit when the user does the register
    public void update_register() {
        if (!this.is_registered) {
            Date now = new Date();

            // difference in miliseconds
            long diff = now.getTime() - date_off.getTime();
            float price = (float) 10.0; //price per minute
            float x = (float) diff / 1000 / 60 / 60; //horas
            float toCharge = 0;

            for (float i = 1; i < (float) diff / 1000 / 60; i++) {
                if ((float) diff / 1000 / 60 / 60 <= i) {
                    toCharge += 6 * price * i * x;
                } else {
                    toCharge += 6 * price * i;//Cobrar horas inteiras
                    x--;
                }
            }
            this.subCredit(toCharge); //Cobrar 0.5 em 2.5 horas //i.e cobrar o resto
            this.is_registered = true;
        }
    }
}//class

