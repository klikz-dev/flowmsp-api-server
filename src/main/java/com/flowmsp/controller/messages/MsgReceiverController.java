package com.flowmsp.controller.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.dispatch.DispatchBadgeResponse;
import com.flowmsp.controller.dispatch.DispatchResponse;
import com.flowmsp.controller.psap.PsapUnitCustomerModel;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DispatchReadStatusDao;
import com.flowmsp.db.MessageDao;
import com.flowmsp.domain.Message;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.dispatch.DispatchReadStatus;
import com.flowmsp.service.FCMService;
import com.flowmsp.service.Message.MessageResult;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.ServerSendEventHandler;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.flowmsp.service.psap.PSAPService;
import com.flowmsp.service.pubsub.googlemailresult;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MsgReceiverController {
    private static final Logger log = LoggerFactory.getLogger(MsgReceiverController.class);
    private final MessageDao msgReceiverDao;
    private final MessageService msgService;
    private final ObjectMapper objectMapper;
    private final CustomerDao customerDao;
    private final DispatchReadStatusDao dispatchReadStatusDao;
    private final FCMService fcmService;
    private final PSAPService psapService;

    public MsgReceiverController(MessageDao msgReceiverDao, CustomerDao customerDao, DispatchReadStatusDao dispatchReadStatusDao, ObjectMapper objectMapper, MessageService msgService, FCMService fcmService, PSAPService psapService) {
        this.msgReceiverDao = msgReceiverDao;
        this.customerDao = customerDao;
        this.dispatchReadStatusDao = dispatchReadStatusDao;
        this.objectMapper = objectMapper;
        this.msgService = msgService;
        this.fcmService = fcmService;
        this.psapService = psapService;
    }

    private Message save_message(MessageResult msgResult) {
        SlugContext.setSlug(msgResult.customer.slug);

        Message msg = new Message();
        msg.id = UUID.randomUUID().toString();
        msg.sequence = System.currentTimeMillis();
        msg.textRaw = msgResult.messageRaw;
        msg.customerId = msgResult.customer.id;
        msg.customerSlug = msgResult.customer.slug;
        msg.address = msgResult.messageAddress;
        msg.status = "";
        msg.text = msgResult.messageRefined;
        msg.type = msgResult.messageType;
        msg.locationID = msgResult.messageLocationID;
        msg.latLon = msgResult.messageLatLon;
        msg.incidentID = msgResult.incidentID;
        msg.units = msgResult.units;
        msgReceiverDao.save(msg);
        SlugContext.clearSlug();

        return msg;
    }

    private MsgSender send_message(Message msg) {
        //Send Message Instantly
        MsgSender msgClient = new MsgSender();
        msgClient.sequence = msg.sequence;
        msgClient.address = msg.address;
        msgClient.text = msg.text;
        msgClient.textRaw = msg.textRaw;
        msgClient.customerID = msg.customerId;
        msgClient.type = msg.type;
        msgClient.locationID = msg.locationID;
        msgClient.latLon = msg.latLon;
        msgClient.status = msg.status;
        msgClient.incidentID = msg.incidentID;
        msgClient.units = msg.units;

        return msgClient;
    }

    public MsgReceiverResponse addSMS(Request req, Response res) {
        try {
            MessageResult msgResult = msgService.ParseSMS(req);

            log.info("in MsgReceiverController.addSMS" + msgResult);

            if (msgResult.errorFlag != 0) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error saving SMS:" + msgResult.errorDescription);
                return MsgReceiverResponse.builder().apply(req, null);
            }

            var msg = save_message(msgResult);
            var msgClient = send_message(msg);

            ServerSendEventHandler.GetMyInstance().SendData(msg.customerId,
                    this.objectMapper.writeValueAsString(msgClient),
                    msg.sequence);
            fcmService.sendDispatchPushNotification(msgResult.customer.slug, msgResult.customer.id, msgClient);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error adding SMS", e);
            return MsgReceiverResponse.builder().apply(req, null);
        } finally {
            SlugContext.clearSlug();
        }
        return MsgReceiverResponse.builder().apply(req, null);
    }

    public MsgReceiverResponse addSFTP(Request req, Response res) {
        try {
            MessageResult msgResult = msgService.ParseSFTP(req);

            log.info("in MsgReceiverController.addSFTP" + msgResult);

            if (msgResult.errorFlag != 0) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error saving SFTP:" + msgResult.errorDescription);
                return MsgReceiverResponse.builder().apply(req, null);
            }

            var msg = save_message(msgResult);
            var msgClient = send_message(msg);

            ServerSendEventHandler.GetMyInstance().SendData(msg.customerId,
                    this.objectMapper.writeValueAsString(msgClient),
                    msg.sequence);
            fcmService.sendDispatchPushNotification(msgResult.customer.slug, msgResult.customer.id, msgClient);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error adding SFTP", e);
            return MsgReceiverResponse.builder().apply(req, null);
        } finally {
            SlugContext.clearSlug();
        }
        return MsgReceiverResponse.builder().apply(req, null);
    }

    public MsgReceiverResponse addMsg(Request req, Response res) {
        try {
	    MessageResult msgResult = msgService.ParseSMS(req);

	    log.info("in MsgReceiverController.addMsg" + msgResult);

	    if (msgResult.errorFlag != 0) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error saving SMS:" + msgResult.errorDescription);
                return MsgReceiverResponse.builder().apply(req, null);
	    }

	    SlugContext.setSlug(msgResult.customer.slug);

	    Message msg = new Message();
	    msg.id = UUID.randomUUID().toString();
	    msg.sequence = System.currentTimeMillis();
	    msg.textRaw = msgResult.messageRaw;
	    msg.customerId = msgResult.customer.id;
	    msg.customerSlug = msgResult.customer.slug;
	    msg.address = msgResult.messageAddress;
	    msg.status = "";
	    msg.text = msgResult.messageRefined;
	    msg.type = msgResult.messageType;
	    msg.locationID = msgResult.messageLocationID;
	    msg.latLon = msgResult.messageLatLon;
	    msgReceiverDao.save(msg);
	    SlugContext.clearSlug();

	    res.status(HttpStatus.SC_NO_CONTENT);

	    //Send Message Instantly

            MsgSender msgClient = new MsgSender();
	    msgClient.sequence = msg.sequence;
	    msgClient.address = msg.address;
	    msgClient.text = msg.text;
	    msgClient.textRaw = msg.textRaw;
	    msgClient.customerID = msg.customerId;
	    msgClient.type = msg.type;
	    msgClient.locationID = msg.locationID;
	    msgClient.latLon = msg.latLon;
	    msgClient.status = msg.status;

	    ServerSendEventHandler.GetMyInstance().SendData(msg.customerId, this.objectMapper.writeValueAsString(msgClient), msg.sequence);
	    fcmService.sendDispatchPushNotification(msgResult.customer.slug, msgResult.customer.id, msgClient);
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error adding SMS", e);
            return MsgReceiverResponse.builder().apply(req, null);
        }
        finally {
	    SlugContext.clearSlug();
        }
        return MsgReceiverResponse.builder().apply(req, null);
    }

    public DispatchResponse getDispatchFeed(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String userId = profile.getUserId();
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            int limit = 20;
            try { limit = Integer.parseInt(req.queryParams("limit")); } catch (Exception ignored) {}
            String offsetId = req.queryParams("offsetId");

            boolean filterByUnits = false;
            try { filterByUnits = Boolean.parseBoolean(req.queryParams("filterByUnits")); } catch (Exception ignored) {}

            String registrationToken = "";
            try { registrationToken = req.queryParams("registrationToken"); } catch (Exception ignored) {}


            ArrayList<MsgSender> messages;
            long totalAmount;

            List<PsapUnitCustomerModel> psapUnitCustomerResponses = psapService.getUnitsByToken(customerId, registrationToken);
            List<String> selectedUnitIds = new ArrayList<>();
            for (PsapUnitCustomerModel psapUnitCustomerModel : psapUnitCustomerResponses) {
                if(psapUnitCustomerModel.selected) {
                    selectedUnitIds.add(psapUnitCustomerModel.unit);
                }
            }

            if(filterByUnits) {
                messages = msgService.getMessagesOfCustomerObjFilteredByUnits(customer.slug, limit, offsetId, selectedUnitIds);
                totalAmount = msgService.getTotalAmountOfMessagesOfCustomerObjFilteredByUnits(customer.slug, selectedUnitIds);
            } else {
                messages = msgService.getMessagesOfCustomerObjAll(customer.slug, limit, offsetId, selectedUnitIds);
                totalAmount = msgService.getTotalAmountOfMessagesOfCustomerObjAll(customer.slug, selectedUnitIds);
            }

            if (messages != null && messages.size() > 0) {
                MsgSender message = messages.get(0);
                Optional<DispatchReadStatus> d = dispatchReadStatusDao.getByFieldValue("userId", userId);

                if (d.isPresent()) {
                    DispatchReadStatus dispatchReadStatus = d.get();
                    if (dispatchReadStatus.lastReadDispatchSequence <= message.sequence) {
                        dispatchReadStatus.lastReadDispatchId = message.id;
                        dispatchReadStatus.lastReadDispatchSequence = message.sequence;
                        dispatchReadStatusDao.replaceById(dispatchReadStatus.id, dispatchReadStatus);
                    }
                } else {
                    DispatchReadStatus dispatchReadStatus = new DispatchReadStatus(UUID.randomUUID().toString(), userId, message.id, message.sequence);
                    dispatchReadStatusDao.save(dispatchReadStatus);
                }
            }

            if (messages != null) {
                DispatchResponse dr = DispatchResponse.build(req, messages, totalAmount);
                res.status(org.eclipse.jetty.http.HttpStatus.OK_200);
                return dr;
            } else {
                res.status(org.eclipse.jetty.http.HttpStatus.UNPROCESSABLE_ENTITY_422);
                return DispatchResponse.build("Error");
            }
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in getDispatchFeed", e);
            return new DispatchResponse();
        }
    }

    public DispatchBadgeResponse getBadge(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String userId = profile.getUserId();
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            long amountOfUnreads = msgService.getAmountOfUnreads(userId, customer.slug);

            DispatchBadgeResponse dr = DispatchBadgeResponse.build(req, amountOfUnreads);
            res.status(org.eclipse.jetty.http.HttpStatus.OK_200);
            return dr;
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in getBadge", e);
            return new DispatchBadgeResponse();
        }
    }

//    public DispatchResponse testPush(Request req, Response res) {
//        //get email
//        String testId = UUID.randomUUID().toString();
//        googlemailresult unreadMail = new googlemailresult();
//        unreadMail.From = "dulyaba@volpis.com";
//        unreadMail.To = "alerts+aaaaaa@flowmsp.com";
//        unreadMail.Subject = "test";
//        unreadMail.messageID = "test" + testId;
//        unreadMail.Body = "CALL: ambulance test" + testId + "\n" +
//                "INFO: test" + testId + "\n" +
//                "ADDR: test\n" +
//                "CITY: test\n" +
//                "DST: test\n" +
//                "ZIP: 01234\n" +
//                "GPS: 41.100019,-74.146818\n";
//        unreadMail.BodyOrig = unreadMail.Body;
//
//        try {
//            msgService.testPushPSAP(unreadMail);
//            return null;
//        } catch (Exception e) {
//            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
//            log.error("Error in testPush", e);
//            return new DispatchResponse();
//        }
//    }
//
//    public DispatchResponse testPushPSAP(Request req, Response res) {
//        //get email
//        String testId = UUID.randomUUID().toString();
//        googlemailresult unreadMail = new googlemailresult();
//        unreadMail.From = "Oleg Dulyaba <dulyaba@volpis.com>";
//        unreadMail.To = "devalerts@flowmsp.com";
//        unreadMail.Subject = "test";
//        unreadMail.messageID = "test" + testId;
//        unreadMail.Body = "CALL: ambulance test" + testId + "\n" +
//                "INFO: test" + testId + "\n" +
//                "ADDR: test\n" +
//                "CITY: test\n" +
//                "DST: test\n" +
//                "ZIP: 01234\n" +
//                "GPS: 41.100019,-74.146818\n" +
//                "UNITS: U2";
//        unreadMail.BodyOrig = unreadMail.Body;
//
//        try {
//            msgService.testPushPSAP(unreadMail);
//            return null;
//        } catch (Exception e) {
//            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
//            log.error("Error in testPush", e);
//            return new DispatchResponse();
//        }
//    }

}
