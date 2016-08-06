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

import java.io.IOException;
import java.util.Date;

import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

/**
 *
 * This is the SIP Servlet for OpenIMS Integration example.
 * 
 */
public class DiameterOpenIMSSipServlet extends SipServlet {

    private static final long serialVersionUID = 1L;
    public static Logger logger = Logger.getLogger(DiameterOpenIMSSipServlet.class);
    DiameterShClient diameterShClient = null;
    public static int reserv = 10;
    public static int reservTo = 1;
    private static SipFactory sipFactory;
    //Data structure to control the credit of each user
    public static HashMap<String, CreditControl> usersCreditDB = new HashMap<String, CreditControl>();
    long timerDuration;
    boolean gone = false;

    /**
     * Default constructor.
     */
    public DiameterOpenIMSSipServlet() {
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {


        logger.info("==============================================================================");
        logger.info("==============>                RM  v210.516                  =================");
        logger.info("==============>          Trab 2 40237, 40581, 41038          =================");
        logger.info("==============>      ap.lourenco@campus.fct.unl.pt           =================");
        logger.info("==============>      dmr.goncalves@campus.fct.unl.pt         =================");
        logger.info("==============>      jp.sousa@campus.fct.unl.pt              =================");
        logger.info("==============================================================================");


        super.init(servletConfig);

        // Get the SIP Factory
        sipFactory = (SipFactory) servletConfig.getServletContext().getAttribute(SIP_FACTORY);

        // Initialize Diameter Sh Client
        try {
            // Get our Diameter Sh Client instance
            this.diameterShClient = new DiameterShClient();

            logger.info("==============> RM T2 logger: Diameter OpenIMS SIP Servlet : Sh-Client Initialized successfuly!");
        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Diameter OpenIMS SIP Servlet : Sh-Client Failed to Initialize.", e);
        }
    }

    @Override
    protected void doInvite(SipServletRequest request) throws ServletException, IOException {
        try {
            String to = request.getTo().getDisplayName() == null ? request.getTo().getURI().toString() : request.getTo().getDisplayName() + " <" + request.getTo().getURI() + ">";
            String from = request.getFrom().getDisplayName() == null ? request.getFrom().getURI().toString() : request.getFrom().getDisplayName() + " <" + request.getFrom().getURI() + ">";
            gone = false;
            logger.info("==============> RM T2 logger: Proccessing INVITE (" + request.getFrom() + " -> " + request.getTo() + ") Request...");

            // rado's comments
            if (request.getTo().getURI().toString() == null) {
                logger.error("================================================> Error reading URI to");
            }

            if (request.getFrom().getURI().toString() == null) {
                logger.error("================================================> Error reading URI from");
            }


            // sends the Invite back - see page 42 of spec.book ("sipservlet-1.0-fcs.pdf") in in "rm_biblio" Desktop folder
            //if the user don't have credit to make a reservation
            if (usersCreditDB.get(from).getCredit() < reserv) {
                sendSIPMessage(from, "Insuficient Credit: " + usersCreditDB.get(from).getCredit() + " ! Call to " + to + " rejected!");
                sendSIPMessage(to, "Call rejected from " + from + " due to lack of credit!");
                SipServletResponse response = request.createResponse(402);
                response.send();
            } else {

                if (request.isInitial()) {
                    //take the static reserve money
                    usersCreditDB.get(from).subCredit(reserv);
                    Proxy proxy = request.getProxy();
                    if (request.getSession().getAttribute("firstInvite") == null) {
                        request.getSession().setAttribute("firstInvite", true);
                        proxy.setRecordRoute(true);
                        proxy.setSupervised(true);
                        proxy.proxyTo(request.getRequestURI());
                    } else {
                        proxy.proxyTo(request.getRequestURI());

                    }
                }
            }
        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Failure in doInvite method.", e);
        }
    }

    @Override
    protected void doAck(SipServletRequest request) throws ServletException, IOException {
        // process ACK
        try {
            logger.info("==============> RM T2 logger: Proccessing ACK (" + request.getFrom() + " -> " + request.getTo() + ") Request...");
            String to = request.getTo().getDisplayName() == null ? request.getTo().getURI().toString() : request.getTo().getDisplayName() + " <" + request.getTo().getURI() + ">";
            String from = request.getFrom().getDisplayName() == null ? request.getFrom().getURI().toString() : request.getFrom().getDisplayName() + " <" + request.getFrom().getURI() + ">";
            CreditControl userCreditFrom = usersCreditDB.get(from);
            CreditControl userCreditTo = usersCreditDB.get(to);
            userCreditFrom.from = from;
            userCreditTo.to = to;
            userCreditFrom.state = 0;
            userCreditTo.state = 1;
            userCreditFrom.chargeTimer(4 * 60 * 1000); //1 min
            userCreditTo.chargeTimer(4 * 60 * 1000); //1 min
        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Failure in doACK method.", e);
        }
    }

    @Override
    protected void doBye(SipServletRequest request) throws ServletException, IOException {
        // process Bye
        String to = request.getTo().getDisplayName() == null ? request.getTo().getURI().toString() : request.getTo().getDisplayName() + " <" + request.getTo().getURI() + ">";
        String from = request.getFrom().getDisplayName() == null ? request.getFrom().getURI().toString() : request.getFrom().getDisplayName() + " <" + request.getFrom().getURI() + ">";
        try {
            logger.info("==============> RM T2 logger: Proccessing BYE (" + request.getFrom() + " -> " + request.getTo() + ") Request...");

            CreditControl userA = usersCreditDB.get(from);
            CreditControl userB = usersCreditDB.get(to);
            userA.stopChargeTimer();
            userB.stopChargeTimer();


            Date now = new Date();
            timerDuration = 4000 * 60 - (now.getTime() - userA.timerDate); //msec
            int time2charge = (int) timerDuration / (1000 * 60);

            float credit1;
            float credit2;

            if (userA.state == 0) {
                credit1 = userA.subCredit(-(time2charge * (reserv / 2)));
                credit2 = userB.subCredit(-(time2charge) * (reservTo));
            } else {
                credit1 = userA.subCredit(-(time2charge) * (reservTo));
                credit2 = userB.subCredit(-(time2charge * (reserv / 2)));
            }

            sendSIPMessage(from, userA.getNotification() + "\n End --> Your credit is " + credit1);
            sendSIPMessage(to, userB.getNotification() + "\n End -> Your credit is " + credit2);
            gone = true;

        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Failure in doBye method.", e);
        }
    }

    @Override
    protected void doSuccessResponse(SipServletResponse response) throws ServletException, IOException {
        // process 2xx response codes
        String to = response.getTo().getDisplayName() == null ? response.getTo().getURI().toString() : response.getTo().getDisplayName() + " <" + response.getTo().getURI() + ">";
        String from = response.getFrom().getDisplayName() == null ? response.getFrom().getURI().toString() : response.getFrom().getDisplayName() + " <" + response.getFrom().getURI() + ">";
        try {

            logger.info("==============> RM T2 logger: Proccessing doSuccessResponse  STATUS(" + response.getStatus() + " from " + response.getFrom().getURI().toString() + ")...");
            //make 4minute reservation

            if (!gone) {
                usersCreditDB.get(from).subCredit(2 * reserv);
                usersCreditDB.get(to).subCredit(4 * reservTo);
            }
        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Failure in doSuccessResponse method.", e);
        }
    }

    @Override
    protected void doErrorResponse(SipServletResponse response) throws ServletException, IOException {
        try {

            logger.info("==============> RM T2 logger: Proccessing Error Response (" + response.getStatus() + ")...");
            String to = response.getTo().getDisplayName() == null ? response.getTo().getURI().toString() : response.getTo().getDisplayName() + " <" + response.getTo().getURI() + ">";
            String from = response.getFrom().getDisplayName() == null ? response.getFrom().getURI().toString() : response.getFrom().getDisplayName() + " <" + response.getFrom().getURI() + ">";

            if (response.getStatus() == 404) //404 - not found; User not found;
            {
                // Let's see from whom to whom
                String toAddress = response.getTo().getURI().toString();


            } else if (response.getStatus() == 487 || response.getStatus() == 488 || response.getStatus() == 480) { //Error establishing session
                usersCreditDB.get(from).subCredit(-(2 * reserv));
                usersCreditDB.get(to).subCredit(-(4 * reservTo));
                logger.info("==============> RM T2 logger: Got error response (" + response.getStatus() + "). Not processing further.");
            }
        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Failure in doErrorResponse method.", e);
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // sends the final message to the user - SMS
    //
    //////////////////////////////////////////////////////////////////////////////
    public static void sendSIPMessage(String toAddressString, String message) {
        try {
            logger.info("==============> RM T2 logger: Sending SIP Message [" + message + "] to [" + toAddressString + "]");

            SipApplicationSession appSession = sipFactory.createApplicationSession();
            Address from = sipFactory.createAddress("RM_T2 <sip:rm_t2@open-ims.test>");
            Address to = sipFactory.createAddress(toAddressString);
            SipServletRequest request = sipFactory.createRequest(appSession, "MESSAGE", from, to);
            request.setContent(message, "text/html");

            request.send();
        } catch (Exception e) {
            logger.error("==============> RM T2 logger: Failure creating/sending SIP Message notification.", e);
        }

    }
}
