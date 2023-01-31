package com.flowmsp.service.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.messages.MsgSender;
import com.flowmsp.db.*;
import com.flowmsp.domain.Message;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.dispatch.DispatchReadStatus;
import com.flowmsp.domain.location.Location;
import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.domain.psap.PsapUnitCustomer;
import com.flowmsp.service.FCMService;
import com.flowmsp.service.MessageParser.MsgParser;
import com.flowmsp.service.MessageParser.PSAPMsgParser;
import com.flowmsp.service.MessageParser.ParsedMessage;
import com.flowmsp.service.MessageParser.ParserFactory;
import com.flowmsp.service.debugpanel.debugPanel;
import com.flowmsp.service.psap.PSAPService;
import com.flowmsp.service.pubsub.googlemailresult;
import com.flowmsp.service.pubsub.googlepubsub;
import com.google.common.base.Strings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.*;

public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final FCMService fcmService;
    private final PSAPService psapService;
    private final CustomerDao customerDao;
    private final MessageDao messageDao;
    private final LocationDao locationDao;
    private final DebugInfoDao debugInfoDao;
    private final DispatchReadStatusDao dispatchReadStatusDao;
    private final ObjectMapper objectMapper;
    private final String mapAPIKey;
    private final HashSet<String> enhanced_parsers = new HashSet<String>();


    public MessageService(CustomerDao customerDao, MessageDao messageDao, LocationDao locationDao,
                          DebugInfoDao debugInfoDao, DispatchReadStatusDao dispatchReadStatusDao,
                          PSAPService psapService, FCMService fcmService, ObjectMapper objectMapper,
                          String mapAPIKey) {
        this.customerDao = customerDao;
        this.messageDao = messageDao;
        this.locationDao = locationDao;
        this.debugInfoDao = debugInfoDao;
        this.dispatchReadStatusDao = dispatchReadStatusDao;
        this.psapService = psapService;
        this.fcmService = fcmService;
        this.objectMapper = objectMapper;
        this.mapAPIKey = mapAPIKey;

        // these parsers will be passed original message with newlines, etc.
        enhanced_parsers.add("Savoy");
        enhanced_parsers.add("Urbana");
        enhanced_parsers.add("GlenCarbon");
        enhanced_parsers.add("Sikeston");
        enhanced_parsers.add("Jacksonville");
        enhanced_parsers.add("Mattoon");
        enhanced_parsers.add("SpringLake");
        enhanced_parsers.add("TriTownship");
        enhanced_parsers.add("Goodfield");
        enhanced_parsers.add("barringtonco");
        enhanced_parsers.add("sandbox");
        enhanced_parsers.add("sandbox_psap");
        enhanced_parsers.add("saddleriverf");
        enhanced_parsers.add("victoriafire");
        enhanced_parsers.add("norwalkfire");
        enhanced_parsers.add("tazewellfire");
        enhanced_parsers.add("emporiafired");
        enhanced_parsers.add("lakesfoursea");
        enhanced_parsers.add("piercecounty");
        //enhanced_parsers.add("ottawafirede");
        enhanced_parsers.add("paxtonfirede");
        enhanced_parsers.add("westplainsfi");
        enhanced_parsers.add("dublinfirede");
        enhanced_parsers.add("osagebeachfi");
        enhanced_parsers.add("valcom");
        enhanced_parsers.add("russellfired");
        enhanced_parsers.add("morgancounty");
        enhanced_parsers.add("marlowvolfir");
        enhanced_parsers.add("highlandvill");
        enhanced_parsers.add("southcentral");
        enhanced_parsers.add("bigriverfire");
        enhanced_parsers.add("highridgefir");
        enhanced_parsers.add("santafefirep");
        enhanced_parsers.add("whiteplainsv");
        enhanced_parsers.add("habershamga");
        enhanced_parsers.add("cencom2152");
        enhanced_parsers.add("kenai_borough");
        enhanced_parsers.add("loneoakfired");
        enhanced_parsers.add("rockcommunit");
        enhanced_parsers.add("montgomery");
        enhanced_parsers.add("psap1667");
        enhanced_parsers.add("psap3935");
        enhanced_parsers.add("somersetfire");
        enhanced_parsers.add("delhitwpfire");
        enhanced_parsers.add("rantoulfire");
        enhanced_parsers.add("clinton1926");
        enhanced_parsers.add("coconino428");
        enhanced_parsers.add("franklintn911");
        enhanced_parsers.add("rutherford4428");
        enhanced_parsers.add("ecom8131");
        enhanced_parsers.add("kirkwood3838");
    }

    public long getAmountOfUnreads(String userId, String slug) {
        long amountOfUnreads;

        Optional<DispatchReadStatus> d = dispatchReadStatusDao.getByFieldValue("userId", userId);

        Boolean slugAlreadyPresent = true;

        Optional<String> slugAlready = SlugContext.getSlug();
        if (!slugAlready.isPresent()) {
            SlugContext.setSlug(slug);
            slugAlreadyPresent = false;
        }

        if (d.isPresent()) {
            DispatchReadStatus dispatchReadStatus = d.get();
            List<Message> msgList = messageDao.getAllByFilter(Filters.and(Filters.gt("sequence", dispatchReadStatus.lastReadDispatchSequence)));
            amountOfUnreads = msgList.size();
        } else {
            List<Message> msgList = messageDao.getAllSort(1);

            if (msgList != null && msgList.size() > 0) {
                Message message = msgList.get(0);
                DispatchReadStatus dispatchReadStatus = new DispatchReadStatus(UUID.randomUUID().toString(), userId, message.id, message.sequence);
                dispatchReadStatusDao.save(dispatchReadStatus);
            } else {
                DispatchReadStatus dispatchReadStatus = new DispatchReadStatus(UUID.randomUUID().toString(), userId, null, System.currentTimeMillis());
                dispatchReadStatusDao.save(dispatchReadStatus);
            }

            amountOfUnreads = 0;
        }

        if (!slugAlreadyPresent) {
            SlugContext.clearSlug();
        }

        return amountOfUnreads;
    }

    public ArrayList<MsgSender> getMessagesOfCustomerObjAll(String slug, int limit, String offsetId, List<String> selectedUnitIds) {
        return getMessagesOfCustomerObj(slug, limit, offsetId, selectedUnitIds, false);
    }

    public ArrayList<MsgSender> getMessagesOfCustomerObjFilteredByUnits(String slug, int limit, String offsetId, List<String> selectedUnitIds) {
        return getMessagesOfCustomerObj(slug, limit, offsetId, selectedUnitIds, true);
    }

    private ArrayList<MsgSender> getMessagesOfCustomerObj(String slug, int limit, String offsetId, List<String> selectedUnitIds, boolean filterByUnits) {
        ArrayList<MsgSender> msgSenderList = null;

        msgSenderList = new ArrayList<>();
        Boolean slugAlreadyPresent = true;

        Optional<String> slugAlready = SlugContext.getSlug();
        if (!slugAlready.isPresent()) {
            SlugContext.setSlug(slug);
            slugAlreadyPresent = false;
        }

        Optional<Message> offsetMessage = messageDao.getById(offsetId);

        List<Message> msgList;

        List<Bson> filters = new ArrayList<>();
        if (offsetMessage.isPresent()) {
            filters.add(Filters.lt("sequence", offsetMessage.get().sequence));
        }
        if (filterByUnits) {
            filters.add(Filters.in("units", selectedUnitIds));
        }
        if (!filters.isEmpty()) {
            msgList = messageDao.getAllSortByFilter(Filters.and(filters), limit);
        } else {
            msgList = messageDao.getAllSort(limit);
        }

        if (!slugAlreadyPresent) {
            SlugContext.clearSlug();
        }

        for (Message msg : msgList) {
            MsgSender msgClient = new MsgSender();
            msgClient.id = msg.id;
            msgClient.sequence = msg.sequence;
            msgClient.address = msg.address;
            msgClient.text = msg.text;
            msgClient.textRaw = msg.textRaw;
            msgClient.customerID = msg.customerId;
            msgClient.type = msg.type;
            msgClient.locationID = msg.locationID;
            msgClient.latLon = msg.latLon;
            msgClient.units = msg.units;
            msgClient.incidentID = msg.incidentID;

            if (selectedUnitIds != null) {
                msgClient.isInUnitFilter = selectedUnitIds.stream()
                        .distinct().anyMatch(msgClient.units::contains);
            }
            msgSenderList.add(msgClient);
        }

        return msgSenderList;
    }

    public long getTotalAmountOfMessagesOfCustomerObjAll(String slug, List<String> selectedUnitIds) {
        return getTotalAmountOfMessagesOfCustomerObj(slug, selectedUnitIds, false);
    }

    public long getTotalAmountOfMessagesOfCustomerObjFilteredByUnits(String slug, List<String> selectedUnitIds) {
        return getTotalAmountOfMessagesOfCustomerObj(slug, selectedUnitIds, true);
    }

    private long getTotalAmountOfMessagesOfCustomerObj(String slug, List<String> selectedUnitIds, boolean filterByUnits) {
        Boolean slugAlreadyPresent = true;

        Optional<String> slugAlready = SlugContext.getSlug();
        if (!slugAlready.isPresent()) {
            SlugContext.setSlug(slug);
            slugAlreadyPresent = false;
        }

        long count;

        if (filterByUnits) {
            count = messageDao.getKount(
                    Filters.in("units", selectedUnitIds));
        } else {
            count = messageDao.getKount();
        }


        if (!slugAlreadyPresent) {
            SlugContext.clearSlug();
        }

        return count;
    }


    public ArrayList<MsgSender> getMessagesOfCustomerObj(String customerID, String slug, Long lastSequence) {
        ArrayList<MsgSender> SMS = new ArrayList<MsgSender>();
        Optional<Customer> c = customerDao.getById(customerID);
        Boolean slugAlreadyPresent = true;
        if (c.isPresent()) {
            Optional<String> slugAlready = SlugContext.getSlug();
            if (!slugAlready.isPresent()) {
                SlugContext.setSlug(slug);
                slugAlreadyPresent = false;
            }
            List<Message> msgList = messageDao.getAllSort(50);
            if (!slugAlreadyPresent) {
                SlugContext.clearSlug();
            }
            long now = System.currentTimeMillis();
            // Last 3 days sequence must be 3*24*60*60*1000 seconds
            now = now - (3 * 24 * 60 * 60 * 1000);
            for (int ii = msgList.size() - 1; ii >= 0; ii--) {
                Message msg = msgList.get(ii);
                if (msg.sequence >= now) {
                    MsgSender msgClient = new MsgSender();
                    msgClient.sequence = msg.sequence;
                    msgClient.address = msg.address;
                    msgClient.text = msg.text;
                    msgClient.textRaw = msg.textRaw;
                    msgClient.customerID = msg.customerId;
                    msgClient.type = msg.type;
                    msgClient.units = msg.units;
                    msgClient.incidentID = msg.incidentID;
                    msgClient.locationID = msg.locationID;
                    msgClient.latLon = msg.latLon;
                    SMS.add(msgClient);
                }
            }
            return SMS;
        }
        return null;
    }

    public ArrayList<String> getMessagesOfCustomer(String customerID, String slug, Long lastSequence) {
        ArrayList<String> SMSStr = new ArrayList<String>();
        ArrayList<MsgSender> SMS = getMessagesOfCustomerObj(customerID, slug, lastSequence);
        if (SMS != null) {
            for (MsgSender msg : SMS) {
                try {
                    SMSStr.add(this.objectMapper.writeValueAsString(msg));
                } catch (JsonProcessingException e) {
                    log.error("Error Processing Customer Message", e);
                }
            }
        }
        return SMSStr;
    }

    public List<MsgSender> readAndStoreUnreadMessages() {
        List<googlemailresult> unreadMails = googlepubsub.GetMyInstance().GetUnreadEmails();
        List<MsgSender> readMessageClient = new ArrayList<MsgSender>();

        for (int ii = 0; ii < unreadMails.size(); ii++) {
            debugPanel xray = new debugPanel(debugInfoDao);
            try {
                xray.SetData("Source", "DISPATCH");
                xray.SetData("Type", "EMAIL");
                xray.SetData("Synch", "PULL");

                String From = unreadMails.get(ii).From;
                String MessageID = unreadMails.get(ii).messageID;
                String To = unreadMails.get(ii).To;
                String Subject = unreadMails.get(ii).Subject;
                String Body = unreadMails.get(ii).Body;
                String BodyOrig = unreadMails.get(ii).BodyOrig;

                log.info("From: " + From);
                log.info("To: " + To);
                log.info("Subject: " + Subject);

                ArrayList<MessageResult> msgResult = new ArrayList<>();

                MessageResult newRow = new MessageResult();
                newRow.messageID = MessageID;
                newRow.emailGateway = From;
                newRow.messageRaw = Body;
                newRow.messageOrig = BodyOrig;

                if (!Strings.isNullOrEmpty(Subject)) {
                    newRow.messageRaw = "Subject-" + Subject + " Msg-" + Body;
                }
                xray.SetData("MessageID", newRow.messageID);
                xray.SetData("From", newRow.emailGateway);
                xray.SetData("RawMessage", newRow.messageRaw);

                Customer cust = getSlugfromEmailID(From, To, Subject, Body);
                PSAP psap = psapService.getPSAPfromEmailID(From);
                ParsedMessage parsedMessage = getParsedMessage(cust, psap, newRow);

                if (parsedMessage == null) {
                    newRow.errorFlag = 1;
                    newRow.errorDescription = "No Associated PSAP and Customer Found for Email:" + newRow.emailGateway;
                    log.error(newRow.errorDescription);
                    xray.SetData("ErrorFlag", newRow.errorFlag);
                    xray.SetData("ErrorDescription", newRow.errorDescription);
                    xray.commit();

                    // Mark this mail as read otherwise we will try this endlessly
                    googlepubsub.GetMyInstance().mail.MarkEmailAsNotProcessed(newRow.messageID);
                    continue;
                }

                if (cust != null) { // can be null if parsing PSAP message
                    xray.SetData("Customer", cust.id);
                    newRow.customer = cust;
                    log.debug("Email Customer Found:" + cust.slug + " For MessageID:" + newRow.messageID + " Email:"
                            + newRow.emailGateway);
                }

                newRow.messageRefined = parsedMessage.text;
                newRow.messageType = parsedMessage.Code;
                newRow.messageAddress = parsedMessage.Address;
                newRow.messageLocation = parsedMessage.location;
                newRow.messageLatLon = parsedMessage.messageLatLon;
                newRow.units = parsedMessage.units;
                newRow.incidentID = parsedMessage.incidentID;

                if (newRow.messageLocation != null) {
                    newRow.messageLocationID = newRow.messageLocation.id;
                    xray.SetData("LocationID", newRow.messageLocation.id);
                } else {
                    xray.SetData("ErrorFlag", 99);
                    xray.SetData("ErrorDescription", "Location not found");
                }

                xray.SetData("Address", newRow.messageAddress);
                if (newRow.messageLatLon != null) {
                    xray.SetData("LatLon", newRow.messageLatLon);
                } else {
                    xray.SetData("ErrorFlag", 98);
                    xray.SetData("ErrorDescription", "LatLon not extracted");
                }

                if (psap != null) {
                    for (String unit : parsedMessage.units) {
                        Optional<PsapUnitCustomer> psapUnitCustomer;
                        Optional<Customer> customerOpt = Optional.empty();

                        psapUnitCustomer = psapService.getPsapUnitCustomerFromPsapUnit(unit, psap.id);
                        if (psapUnitCustomer.isPresent())
                            customerOpt = customerDao.getById(psapUnitCustomer.get().customerId);
                        if (customerOpt.isPresent()) {
                            MessageResult newPsapRow = newRow.copy();
                            newPsapRow.customer = customerOpt.get();
                            msgResult.add(newPsapRow);
                        }
                    }
                } else {
                    msgResult.add(newRow);
                }

                for (MessageResult messageResult : msgResult) {
                    Customer customer = messageResult.customer;

                    // Set SLUG Here
                    SlugContext.setSlug(customer.slug);
                    List<Message> msgList = messageDao
                            .getAllByFilter(Filters.and(Filters.eq("messageID", MessageID), Filters.eq("source", "email")));

                    if (msgList.size() > 0) {
                        log.info("Alread saved this message:" + MessageID);
                        SlugContext.clearSlug();
                        googlepubsub.GetMyInstance().mail.MarkEmailAsProcessed(MessageID);
                        Message msg = msgList.get(0);
                        MsgSender msgClient = new MsgSender();
                        msgClient.sequence = msg.sequence;
                        msgClient.address = msg.address;
                        msgClient.text = msg.text;
                        msgClient.textRaw = msg.textRaw;
                        msgClient.customerID = msg.customerId;
                        msgClient.type = msg.type;
                        msgClient.units = msg.units;
                        msgClient.incidentID = msg.incidentID;
                        msgClient.locationID = msg.locationID;
                        msgClient.latLon = msg.latLon;
                        msgClient.status = msg.status;
                        readMessageClient.add(msgClient);
                        // readMessageId.add(MessageID);
                        continue;
                    }

                    // Add into Database
                    Message msg = new Message();
                    msg.id = UUID.randomUUID().toString();
                    msg.sequence = System.currentTimeMillis();
                    msg.textRaw = messageResult.messageRaw;
                    msg.customerId = messageResult.customer.id;
                    msg.customerSlug = messageResult.customer.slug;
                    msg.text = messageResult.messageRefined;
                    msg.address = messageResult.messageAddress;
                    msg.status = "";
                    msg.type = messageResult.messageType;
                    msg.units = messageResult.units;
                    msg.incidentID = messageResult.incidentID;
                    msg.locationID = messageResult.messageLocationID;
                    msg.latLon = messageResult.messageLatLon;
                    msg.messageID = MessageID;
                    msg.source = "email";
                    messageDao.save(msg);
                    SlugContext.clearSlug();
                    // Mark this mail as read
                    googlepubsub.GetMyInstance().mail.MarkEmailAsProcessed(MessageID);

                    MsgSender msgClient = new MsgSender();
                    msgClient.id = msg.id;
                    msgClient.sequence = msg.sequence;
                    msgClient.address = msg.address;
                    msgClient.text = msg.text;
                    msgClient.textRaw = msg.textRaw;
                    msgClient.customerID = msg.customerId;
                    msgClient.type = msg.type;
                    msgClient.units = msg.units;
                    msgClient.incidentID = msg.incidentID;
                    msgClient.locationID = msg.locationID;
                    msgClient.latLon = msg.latLon;
                    msgClient.status = msg.status;
                    readMessageClient.add(msgClient);
                    xray.commit();

                    fcmService.sendDispatchPushNotification(customer.slug, customer.id, msgClient);
                }
            } catch (Exception ex) {
                log.error("Error Reading & Storing Emails", ex);
                xray.SetData("ErrorFlag", 1);
                xray.SetData("ErrorDescription", ex.toString());
                xray.commit();
            } finally {
                SlugContext.clearSlug();
            }
        }

        return readMessageClient;
    }

	public MessageResult ParseSFTP(Request msgRequest) {
		MessageResult msgResult = new MessageResult();
		try {
			log.debug("SFTP Incoming Body" + msgRequest.body());
			msgResult.messageRaw = msgRequest.body();

			String smsBody = msgResult.messageRaw;
			Map<String, String> d = objectMapper.readValue(smsBody, new TypeReference<HashMap<String, String>>() {
			});

			msgResult.SFTP_userid = d.get("sftp_userid").toString();

			// Now extract customer slug from this
			Customer cust = getSlugByKey("SFTP_userid", msgResult.SFTP_userid);
			if (cust == null) {
				msgResult.errorFlag = 1;
				msgResult.errorDescription = "No Associated Customer Found for SFTP:" + msgResult.SFTP_userid;
				log.error(msgResult.errorDescription);
				return msgResult;
			}
			msgResult.customer = cust;
			log.debug("SFTP Customer Found:" + cust.slug);
			// Set SLUG Here
			SlugContext.setSlug(cust.slug);
			MsgParser msgParser = ParserFactory.CreateObject("email", cust.emailFormat);
			String plainMsg = d.get("text").toString();
			ParsedMessage parsedMessage = msgParser.Process(cust, plainMsg, this);
			if (parsedMessage.ErrorFlag != 0) {
				log.error("Error parsing SFTP Location-1:" + plainMsg);
			}
			msgResult.messageRefined = parsedMessage.text;
			msgResult.messageType = parsedMessage.Code;
			msgResult.messageAddress = parsedMessage.Address;
			msgResult.messageLocation = parsedMessage.location;
			msgResult.messageLatLon = parsedMessage.messageLatLon;
			msgResult.units = parsedMessage.units;
			msgResult.incidentID = parsedMessage.incidentID;

			if (msgResult.messageLocation != null) {
				msgResult.messageLocationID = msgResult.messageLocation.id;
			}
		}catch (Exception ex) {
			log.error("Error parsing SFTP Location-2:" + msgResult.messageRefined, ex);
		} finally {
			SlugContext.clearSlug();
		}
		return msgResult;
	}

//    public MsgSender testPushPSAP(googlemailresult unreadMail) {
//        MsgSender readMessageClient = null;
//        MessageResult messageResult = null;
//
//        try {
//            //disassemble email
//            String From = unreadMail.From;
//            String MessageID = unreadMail.messageID;
//            String To = unreadMail.To;
//            String Subject = unreadMail.Subject;
//            String Body = unreadMail.Body;
//            String BodyOrig = unreadMail.BodyOrig;
//
//            log.info("From: " + From);
//            log.info("To: " + To);
//            log.info("Subject: " + Subject);
//
//            MessageResult newRow = new MessageResult();
//            newRow.messageID = MessageID;
//            newRow.emailGateway = From;
//            newRow.messageRaw = Body;
//            newRow.messageOrig = BodyOrig;
//            if (!Strings.isNullOrEmpty(Subject)) {
//                newRow.messageRaw = "Subject-" + Subject + " Msg-" + Body;
//            }
//
//            //parse message
//            Customer cust = getSlugfromEmailID(From, To, Subject, Body);
//            PSAP psap = psapService.getPSAPfromEmailID(From);
//            ParsedMessage parsedMessage = getParsedMessage(cust, psap, newRow);
//
//            //return if message not parsed
//            if (parsedMessage == null) {
//                newRow.errorFlag = 1;
//                newRow.errorDescription = "No Associated PSAP and Customer Found for Email:" + newRow.emailGateway;
//                return readMessageClient;
//            }
//
//            if (cust != null) { // can be null if parsing PSAP message
//                newRow.customer = cust;
//            }
//
//            newRow.messageRefined = parsedMessage.text;
//            newRow.messageType = parsedMessage.Code;
//            newRow.messageAddress = parsedMessage.Address;
//            newRow.messageLocation = parsedMessage.location;
//            newRow.messageLatLon = parsedMessage.messageLatLon;
//            newRow.units = parsedMessage.units;
//            newRow.incidentID = parsedMessage.incidentID;
//
//            if (newRow.messageLocation != null) {
//                newRow.messageLocationID = newRow.messageLocation.id;
//            }
//
//            //create messageResult
//            if (psap != null) {
//                for (String unit : parsedMessage.units) {
//                    Optional<PsapUnitCustomer> psapUnitCustomer;
//                    Optional<Customer> customerOpt = Optional.empty();
//
//                    psapUnitCustomer = psapService.getPsapUnitCustomerFromPsapUnit(unit, psap.id);
//                    if (psapUnitCustomer.isPresent())
//                        customerOpt = customerDao.getById(psapUnitCustomer.get().customerId);
//                    if (customerOpt.isPresent()) {
//                        MessageResult newPsapRow = newRow.copy();
//                        newPsapRow.customer = customerOpt.get();
//                        messageResult = newPsapRow;
//                    }
//                }
//            } else {
//                messageResult = newRow;
//            }
//
//            if (messageResult != null) {
//                Customer customer = messageResult.customer;
//
//                // Set SLUG Here
//                SlugContext.setSlug(customer.slug);
//                List<Message> msgList = messageDao
//                        .getAllByFilter(Filters.and(Filters.eq("messageID", MessageID), Filters.eq("source", "email")));
//
//                if (msgList.size() > 0) {
//                    SlugContext.clearSlug();
//
//                    Message msg = msgList.get(0);
//                    MsgSender msgClient = new MsgSender();
//                    msgClient.sequence = msg.sequence;
//                    msgClient.address = msg.address;
//                    msgClient.text = msg.text;
//                    msgClient.textRaw = msg.textRaw;
//                    msgClient.customerID = msg.customerId;
//                    msgClient.type = msg.type;
//                    msgClient.units = msg.units;
//                    msgClient.incidentID = msg.incidentID;
//                    msgClient.locationID = msg.locationID;
//                    msgClient.latLon = msg.latLon;
//                    msgClient.status = msg.status;
//                    readMessageClient = msgClient;
//                    return readMessageClient;
//                }
//
//                // Add into Database
//                Message msg = new Message();
//                msg.id = UUID.randomUUID().toString();
//                msg.sequence = System.currentTimeMillis();
//                msg.textRaw = messageResult.messageRaw;
//                msg.customerId = messageResult.customer.id;
//                msg.customerSlug = messageResult.customer.slug;
//                msg.text = messageResult.messageRefined;
//                msg.address = messageResult.messageAddress;
//                msg.status = "";
//                msg.type = messageResult.messageType;
//                msg.units = messageResult.units;
//                msg.incidentID = messageResult.incidentID;
//                msg.locationID = messageResult.messageLocationID;
//                msg.latLon = messageResult.messageLatLon;
//                msg.messageID = MessageID;
//                msg.source = "email";
//                messageDao.save(msg);
//                SlugContext.clearSlug();
//
//                MsgSender msgClient = new MsgSender();
//                msgClient.id = msg.id;
//                msgClient.sequence = msg.sequence;
//                msgClient.address = msg.address;
//                msgClient.text = msg.text;
//                msgClient.textRaw = msg.textRaw;
//                msgClient.customerID = msg.customerId;
//                msgClient.type = msg.type;
//                msgClient.units = msg.units;
//                msgClient.incidentID = msg.incidentID;
//                msgClient.locationID = msg.locationID;
//                msgClient.latLon = msg.latLon;
//                msgClient.status = msg.status;
//                readMessageClient = msgClient;
//
//                fcmService.sendDispatchPushNotification(customer.slug, customer.id, msgClient);
//            }
//        } catch (Exception ex) {
//            log.error("Error");
//        } finally {
//            SlugContext.clearSlug();
//        }
//
//        return readMessageClient;
//    }

  public MessageResult ParseSMS(Request msgRequest) {
	MessageResult msgResult = new MessageResult();
	debugPanel xray = new debugPanel(debugInfoDao);
	try {
	    xray.SetData("Source", "DISPATCH");
	    xray.SetData("Type", "SMS");
	    xray.SetData("Synch", "PUSH");
	    
	    log.debug("SMS Incoming Body" + msgRequest.body());
	    msgResult.messageRaw = msgRequest.body();
	    
	    String smsBody = msgResult.messageRaw;
	    // {"messageId":"m-6ojc2efqse46dql3z4bketa","from":"+19089308408","eventType":"sms","text":"ALARM
	    // 49 RIVERGATE DR, WILTON CT
	    // 06897","time":"2018-04-03T15:27:12Z","to":"+18153130036","state":"received","messageUri":"https://api.catapult.inetwork.com/v1/users/u-2bo3lcy3nad6imuvx5c4adq/messages/m-6ojc2efqse46dql3z4bketa","applicationId":"a-n3fv2d6wegjfzgv4hh3i6py","direction":"in"}
	    
	    Map<String, String> d = objectMapper.readValue(smsBody, new TypeReference<HashMap<String, String>>() {
		});
	    
	    msgResult.smsNumber = d.get("to").toString();
	    // Now extract customer slug from this
	    Customer cust = getSlugfromSMSNumber(msgResult.smsNumber);
	    if (cust == null) {
		msgResult.errorFlag = 1;
		msgResult.errorDescription = "No Associated Customer Found for SMS:" + msgResult.smsNumber;
		log.error(msgResult.errorDescription);
		xray.SetData("ErrorFlag", msgResult.errorFlag);
		xray.SetData("ErrorDescription", msgResult.errorDescription);
		xray.commit();
		return msgResult;
	    }
	    msgResult.customer = cust;
	    log.debug("SMS Customer Found:" + cust.slug);
	    // Set SLUG Here
	    SlugContext.setSlug(cust.slug);
	    MsgParser msgParser = ParserFactory.CreateObject("SMS", cust.smsFormat);
	    String plainMsg = d.get("text").toString();
	    ParsedMessage parsedMessage = msgParser.Process(cust, plainMsg, this);
	    if (parsedMessage.ErrorFlag != 0) {
		log.error("Error parsing SMS Location-1:" + plainMsg);
	    }
	    msgResult.messageRefined = parsedMessage.text;
	    msgResult.messageType = parsedMessage.Code;
	    msgResult.messageAddress = parsedMessage.Address;
	    msgResult.messageLocation = parsedMessage.location;
	    msgResult.messageLatLon = parsedMessage.messageLatLon;
		msgResult.units = parsedMessage.units;
		msgResult.incidentID = parsedMessage.incidentID;

	    if (msgResult.messageLocation != null) {
		msgResult.messageLocationID = msgResult.messageLocation.id;
		xray.SetData("LocationID", msgResult.messageLocationID);
	    } else {
		xray.SetData("ErrorFlag", 99);
		xray.SetData("ErrorDescription", "Location not found");
	    }
	    
	    xray.SetData("From", msgResult.smsNumber);
	    xray.SetData("RawMessage", msgResult.messageRaw);
	    xray.SetData("Address", msgResult.messageAddress);
	    if (msgResult.messageLatLon != null) {
		xray.SetData("LatLon", msgResult.messageLatLon);
	    } else {
		xray.SetData("ErrorFlag", 98);
		xray.SetData("ErrorDescription", "LatLon not extracted");
	    }
	    xray.commit();
	} catch (Exception ex) {
	    log.error("Error parsing SMS Location-2:" + msgResult.messageRefined, ex);
	    xray.SetData("ErrorFlag", 2);
	    xray.SetData("ErrorDescription", "Error parsing SMS Location:" + ex.toString());
	    xray.commit();
	    
	} finally {
	    SlugContext.clearSlug();
	}
	return msgResult;
    }

    public synchronized List<MessageResult> ParsePushNotification(Request pushRequest) {
        List<MessageResult> msgResult = new ArrayList<MessageResult>();
        try {
            String pushRequestBody = pushRequest.body();
            log.debug("ParsePushNotification " + pushRequestBody);
            JSONParser parser = new JSONParser();
            Object objRequestBody = parser.parse(pushRequestBody);
            JSONObject jbRequestBody = (JSONObject) objRequestBody;

            if (jbRequestBody == null) {
                log.error("Parser Issue In ParsePushNotification:" + pushRequestBody);
                return msgResult;
            }

            String messageDataEncoded64 = ((JSONObject) jbRequestBody.get("message")).get("data").toString();
            String messageDataDecoded64 = StringUtils.newStringUtf8(Base64.decodeBase64(messageDataEncoded64));
            // {"emailAddress":"alerts@flowmsp.com","historyId":7955}
            Object objDecodedData = parser.parse(messageDataDecoded64);
            String historyIdNow = ((JSONObject) objDecodedData).get("historyId").toString();
            String historyIdStart = googlepubsub.GetMyInstance().watcher.history.historyId;

            // These History IDS are Numeric
            log.info("History IDS:" + historyIdNow + " " + historyIdStart);
            long lnHistoryIdNow = Long.parseLong(historyIdNow);
            long lnHistoryIdStart = 0;
            if (Strings.isNullOrEmpty(historyIdStart)) {
                lnHistoryIdStart = lnHistoryIdNow - 1;
                historyIdStart = String.valueOf(lnHistoryIdStart);
            } else {
                lnHistoryIdStart = Long.parseLong(historyIdStart);
            }

            if (lnHistoryIdNow < lnHistoryIdStart) {
                log.error("HistoryID Now is Less Than HistoryID Start:" + lnHistoryIdNow + " : " + lnHistoryIdStart);
                return msgResult;
            }

            List<googlemailresult> gresult = googlepubsub.GetMyInstance().GetEmailsHistory(historyIdStart);
            for (int ii = 0; ii < gresult.size(); ii++) {
                debugPanel xray = new debugPanel(debugInfoDao);
                xray.SetData("Source", "DISPATCH");
                xray.SetData("Type", "EMAIL");
                xray.SetData("Synch", "PUSH");

                MessageResult newRow = new MessageResult();
                newRow.messageID = gresult.get(ii).messageID;
                String From = gresult.get(ii).From;
                String To = gresult.get(ii).To;
                String Subject = gresult.get(ii).Subject;
                String Body = gresult.get(ii).Body;
                String BodyOrig = gresult.get(ii).BodyOrig;

                log.info("ParsePushNotification: " + From + " " + To + " " + Subject);

                newRow.emailGateway = From;
                newRow.messageRaw = Body;
                newRow.messageOrig = BodyOrig;

                if (!Strings.isNullOrEmpty(Subject)) {
                    newRow.messageRaw = "Subject-" + Subject + " Msg-" + Body;
                }
                xray.SetData("MessageID", newRow.messageID);
                xray.SetData("From", newRow.emailGateway);
                xray.SetData("RawMessage", newRow.messageRaw);

                Customer cust = getSlugfromEmailID(From, To, Subject, Body);
                PSAP psap = psapService.getPSAPfromEmailID(From);
                ParsedMessage parsedMessage = getParsedMessage(cust, psap, newRow);

                if (parsedMessage == null) {
                    newRow.errorFlag = 1;
                    newRow.errorDescription = "No Associated PSAP and Customer Found for Email:" + newRow.emailGateway;
                    log.error(newRow.errorDescription);
                    xray.SetData("ErrorFlag", newRow.errorFlag);
                    xray.SetData("ErrorDescription", newRow.errorDescription);
                    xray.commit();

                    // Mark this mail as read otherwise we will try this endlessly
                    googlepubsub.GetMyInstance().mail.MarkEmailAsNotProcessed(newRow.messageID);
                    continue;
                }

                if (cust != null) { // can be null if parsing PSAP message
                    xray.SetData("Customer", cust.id);
                    newRow.customer = cust;
                    log.debug("Email Customer Found:" + cust.slug + " For MessageID:" + newRow.messageID + " Email:"
                            + newRow.emailGateway);
                }

                try {
                    newRow.messageRefined = parsedMessage.text;
                    newRow.messageType = parsedMessage.Code;
                    newRow.messageAddress = parsedMessage.Address;
                    newRow.messageLocation = parsedMessage.location;
                    newRow.messageLatLon = parsedMessage.messageLatLon;
                    newRow.units = parsedMessage.units;
                    newRow.incidentID = parsedMessage.incidentID;

                    if (newRow.messageLocation != null) {
                        newRow.messageLocationID = newRow.messageLocation.id;
                        xray.SetData("LocationID", newRow.messageLocationID);
                    } else {
                        xray.SetData("ErrorFlag", 99);
                        xray.SetData("ErrorDescription", "Location not found");
                    }

                    if (psap != null) {
                        for (String unit : parsedMessage.units) {
                            Optional<PsapUnitCustomer> psapUnitCustomer;
                            Optional<Customer> customerOpt = Optional.empty();

                            psapUnitCustomer = psapService.getPsapUnitCustomerFromPsapUnit(unit, psap.id);
                            if (psapUnitCustomer.isPresent())
                                customerOpt = customerDao.getById(psapUnitCustomer.get().customerId);
                            if (customerOpt.isPresent()) {
                                MessageResult newPsapRow = newRow.copy();
                                newPsapRow.customer = customerOpt.get();
                                msgResult.add(newPsapRow);
                            }
                        }
                    } else {
                        msgResult.add(newRow);
                    }


                    xray.SetData("Address", newRow.messageAddress);
                    if (newRow.messageLatLon != null) {
                        xray.SetData("LatLon", newRow.messageLatLon);
                    } else {
                        xray.SetData("ErrorFlag", 98);
                        xray.SetData("ErrorDescription", "LatLon not extracted");
                    }
                    // Mark this email as read
                    googlepubsub.GetMyInstance().mail.MarkEmailAsProcessed(gresult.get(ii).messageID);
                    xray.commit();
                } catch (Exception e) {
                    SlugContext.clearSlug();
                    log.error("Error in ParsePushNotification - 1", e);
                    xray.SetData("ErrorFlag", 1);
                    xray.SetData("ErrorDescription", "Error in Parsing or Saving");
                    xray.commit();
                }
            }
            googlepubsub.GetMyInstance().watcher.history.historyId = historyIdNow;
            googlepubsub.GetMyInstance().watcher.history.historyTimeStamp = new Date();
        } catch (Exception ex) {
            log.error("Error in ParsePushNotification - 2", ex);
        }
        return msgResult;
    }

    private ParsedMessage getParsedMessage(Customer cust, PSAP psap, MessageResult newRow) {
        String plainMsg = newRow.messageRaw;

        ParsedMessage parsedMessage = null;

        if (cust != null) {
            if (enhanced_parsers.contains(cust.emailFormat)) {
                log.info(cust.emailFormat + " will use orig messageOrig");
                plainMsg = newRow.messageOrig;
            }
            MsgParser msgParser = ParserFactory.CreateObject("email", cust.emailFormat);
            SlugContext.setSlug(cust.slug);
            parsedMessage = msgParser.Process(cust, plainMsg, this);
            if (parsedMessage.ErrorFlag != 0) {
                log.error("Error parsing Email Location-1:" + plainMsg);
            }
            SlugContext.clearSlug();
        } else if (psap != null) {
                plainMsg = newRow.messageOrig;
                MsgParser msgParser = ParserFactory.CreateObject("email", psap.emailFormat);
                if (msgParser instanceof PSAPMsgParser) {
                    parsedMessage = ((PSAPMsgParser) msgParser).Process(psap, plainMsg, this, psapService);
                }
        }

        return parsedMessage;
    }

    private boolean IsZero(double val) {
        double ZERO = 0.0;
        if (Double.compare(val, ZERO) != 0) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public Point getPointByGoogleMaps(Customer cust, String addToSearch) {
        var address = addToSearch.replace("#", "");

        String targetURL = "https://maps.googleapis.com/maps/api/geocode/json?address=" + address.replace(" ", "+")
                + "&key=" + mapAPIKey;

        if ((!IsZero(cust.boundSWLat)) && (!IsZero(cust.boundSWLon)) && (!IsZero(cust.boundNELat))
                && (!IsZero(cust.boundNELon))) {
            targetURL += ("&bounds=" + cust.boundSWLat + "," + cust.boundSWLon + "|" + cust.boundNELat + "," + cust.boundNELon);
        }

        log.info("HTTP Call:" + targetURL);
        HttpURLConnection connection = null;

        try {
            // Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/JSON");
            connection.setRequestProperty("Content-Length", "512");
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setReadTimeout(30000); // set the connection timeout value to 30 seconds (30000 milliseconds)
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                // 200 is for HTTP 200 OK
                log.error("Error timeout by google maps:" + addToSearch);
                return null;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // now parse
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(response.toString());
            JSONObject jb = (JSONObject) obj;

            // now read
            JSONArray jsonObject1 = (JSONArray) jb.get("results");
            if (jsonObject1.size() == 0) {
                // Google Map was unable to produce any result
                return null;
            }
            JSONObject jsonObject2 = (JSONObject) jsonObject1.get(0);
            JSONObject jsonObject3 = (JSONObject) jsonObject2.get("geometry");
            JSONObject location = (JSONObject) jsonObject3.get("location");

            double lat = (double) location.get("lat");
            double lon = (double) location.get("lng");

            return new Point(new Position(lon, lat));
        } catch (Exception e) {
            log.error("Error getting location by google maps", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public Location getLocationByPoint(Point p) {
        try {
            List<Location> candidateLoc = locationDao.getWithIn(p);
            if (!candidateLoc.isEmpty()) {
                return candidateLoc.get(0);
            }
        } catch (Exception e) {
            log.error("Error getting location by point", e);
        }
        return null;
    }

    public Location getLocationByAddress(String addToSearch) {
        String addToSearchDropSpl = addToSearch.toUpperCase().trim().replace(" ", "");
        List<Location> loc = locationDao.getAll();
        for (Location obj : loc) {
            if (obj.address != null) {
                String addCurrent = NullStr(obj.address.address1) + NullStr(obj.address.address2)
                        + NullStr(obj.address.city) + NullStr(obj.address.state);
                addCurrent = addCurrent.toUpperCase().trim().replace(" ", "");
                if (addCurrent.contains(addToSearchDropSpl)) {
                    String firstAddress = NullStr(obj.address.address1);
                    firstAddress = firstAddress.toUpperCase().trim().replace(" ", "");
                    // Address1 should be the starting point
                    if (addToSearchDropSpl.startsWith(firstAddress)) {
                        return obj;
                    }
                }
            }
        }
        return null;
    }

    public String removeHyperlink(String commentstr) {
        String retStr = commentstr;
        // Remove upto 5 such hyperlinks, I don't want to make this a endless task
        for (int jj = 0; jj < 5; jj++) {
            int startIdx = retStr.indexOf("<https");
            if (startIdx > 0) {
                int endIdx = retStr.indexOf(">", startIdx + 1);
                if (endIdx > startIdx) {
                    // Remove Everything in between
                    String removeStr = retStr.substring(startIdx, endIdx + 1);
                    retStr = retStr.replace(removeStr, "");
                }
            } else {
                break;
            }
        }
        return retStr;
    }

    public String NullStr(String val) {
        String ret = " ";
        if (val == null)
            return ret;
        return val;
    }

    public Customer getSlugfromSMSNumber(String smsNumber) {
        // Find Twilio Number from Customer collection
        List<Customer> customerList = customerDao.getAllByFieldValue("smsNumber", smsNumber);
        if (customerList.isEmpty()) {
            return null;
        }
        return customerList.get(0);
    }

		public Customer getSlugByKey(String key, String value) {
			// Find Twilio Number from Customer collection
			List<Customer> customerList = customerDao.getAllByFieldValue(key, value);
			if (customerList.isEmpty()) {
				return null;
			}
			return customerList.get(0);
		}

    public Customer getSlugfromEmailID(String from, String to, String subject, String body) {
        log.info("in getSlugfromEmailID 1");
        log.info("from: " + from);
        log.info("to: " + to);
        log.info("subject: " + subject);
        log.info("body: " + body);

        var now = Date.from(ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).plusDays(0).toInstant());
        log.info("now=" + now);

        Customer retCust = null;
        // 1. Get All Customers whose 'emailFormat' is defined.
        // 2. Refine those customers whose 'fromContains' contains from and
        // fromNotContains doesn't contain from
        List<Customer> haveEmailGateway = customerDao
                .getAllByFilter(Filters.and(
                        Filters.ne("emailFormat", null),
                        Filters.ne("emailFormat", ""),
                        Filters.gt("license.expirationTimestamp", now)
                        )
                );

        log.info("haveEmailGateway.size()=" + haveEmailGateway.size());
        // Now <from> contains deepakdhasmana@rediffmail.com; deepakdhasmana@gmail.com
        // Customer from Contains dhasmana@rediff,
        for (int ii = 0; ii < haveEmailGateway.size(); ii++) {
            Customer candidate = haveEmailGateway.get(ii);

            if (Strings.isNullOrEmpty(candidate.fromContains)) {
                if (Strings.isNullOrEmpty(candidate.emailGateway)) {
                    // This is very wrong, report out the error
                    continue;
                }
                candidate.fromContains = candidate.emailGateway;

                // Check if Email Signature present
                if (!Strings.isNullOrEmpty(candidate.emailSignature)) {
                    if (!Strings.isNullOrEmpty(candidate.emailSignatureLocation)) {
                        if (candidate.emailSignatureLocation.equalsIgnoreCase("subject")) {
                            // Subject
                            candidate.subjectContains = candidate.emailSignature;
                        } else {
                            // Body
                            candidate.bodyContains = candidate.emailSignature;
                        }
                    }
                }
            }

            // 1- Check <from> this is most important and required field
            String[] fromEmailIds = candidate.fromContains.split(",");
            Boolean foundFrom = false;
            for (int jj = 0; jj < fromEmailIds.length; jj++) {
                foundFrom = false;
                String testFrom = fromEmailIds[jj].trim();

                String[] testFromOr = testFrom.split("\\|");
                for (int kk = 0; kk < testFromOr.length; kk++) {
                    String testFromAny = testFromOr[kk].trim();
                    //log.info(kk + " testFromAny=" + testFromAny);

                    if (from.contains(testFromAny)) {
                        foundFrom = true;
                        log.info("foundFrom set to true");

                        break;
                    }
                }
            }

            //log.info("slug=" + candidate.slug);
            if (!foundFrom) {
                //log.info("foundFrom is false");
                // What's the point to continue further, just skip this record
                continue;
            }

            if (!Strings.isNullOrEmpty(candidate.fromNotContains)) {
                log.info("in candidate.fromNotContains");

                // Now we have to check that this shouldn't be present in from
                String[] fromEmailIdsNot = candidate.fromNotContains.split(",");
                Boolean foundFromNotContain = false;
                for (int jj = 0; jj < fromEmailIdsNot.length; jj++) {
                    String testFromNot = fromEmailIdsNot[jj].trim();
                    if (from.contains(testFromNot)) {
                        foundFromNotContain = true;
                        break;
                    }
                }
                if (foundFromNotContain) {
                    log.info("foundFromNotContain is true, continue");
                    // What's the point to continue further, just skip this record
                    continue;
                }
            }

            log.info("candidate.toContains=" + candidate.toContains);

            if (!Strings.isNullOrEmpty(candidate.toContains)) {
                // 2- Check <to> this is optional field
                String[] toEmailIds = candidate.toContains.split(",");
                Boolean foundTo = false;

                for (int jj = 0; jj < toEmailIds.length; jj++) {
                    foundTo = false;
                    String testTo = toEmailIds[jj].trim();

                    log.info(jj + " testTo=" + testTo);
                    String[] testToOr = testTo.split("\\|");
                    for (int kk = 0; kk < testToOr.length; kk++) {
                        String testToAny = testToOr[kk].trim();

                        log.info(kk + " testToAny=" + testToAny);
                        if (to.contains(testToAny)) {
                            foundTo = true;
                            log.info("foundTo set to true");

                            break;
                        }
                    }
                }
                if (!foundTo) {
                    // What's the point to continue further, just skip this record
                    continue;
                }
            }

            if (!Strings.isNullOrEmpty(candidate.toNotContains)) {
                // Now we have to check that this shouldn't be present in to
                String[] toEmailIdsNot = candidate.toNotContains.split(",");
                Boolean foundToNotContain = false;
                for (int jj = 0; jj < toEmailIdsNot.length; jj++) {
                    String testToNot = toEmailIdsNot[jj].trim();
                    if (to.contains(testToNot)) {
                        foundToNotContain = true;
                        break;
                    }
                }
                if (foundToNotContain) {
                    // What's the point to continue further, just skip this record
                    continue;
                }
            }

            if (!Strings.isNullOrEmpty(candidate.subjectContains)) {
                // 3- Check <subject> this is optional field
                String[] subjectEmailIds = candidate.subjectContains.split(",");
                Boolean foundSubject = false;
                for (int jj = 0; jj < subjectEmailIds.length; jj++) {
                    foundSubject = false;
                    String testSubject = subjectEmailIds[jj].trim();
                    String[] testSubjectOr = testSubject.split("\\|");
                    for (int kk = 0; kk < testSubjectOr.length; kk++) {
                        String testSubjectAny = testSubjectOr[kk].trim();
                        if (subject.contains(testSubjectAny)) {
                            foundSubject = true;
                            break;
                        }
                    }
                }
                if (!foundSubject) {
                    // What's the point to continue further, just skip this record
                    continue;
                }
            }

            if (!Strings.isNullOrEmpty(candidate.subjectNotContains)) {
                // Now we have to check that this shouldn't be present in to
                var subjectEmailIdsNot = candidate.subjectNotContains.trim();

                if (subject.contains(subjectEmailIdsNot))
                    continue;
            }

            if (!Strings.isNullOrEmpty(candidate.bodyContains)) {
                // 4- Check <body> this is optional field
                String[] bodyEmailIds = candidate.bodyContains.split(",");
                Boolean foundBody = false;
                for (int jj = 0; jj < bodyEmailIds.length; jj++) {
                    foundBody = false;
                    String testBody = bodyEmailIds[jj].trim();
                    String[] testBodyOr = testBody.split("\\|");
                    for (int kk = 0; kk < testBodyOr.length; kk++) {
                        String testBodyAny = testBodyOr[kk].trim();
                        if (body.contains(testBodyAny)) {
                            foundBody = true;
                            break;
                        }
                    }
                }
                if (!foundBody) {
                    // What's the point to continue further, just skip this record
                    continue;
                }
            }

            if (!Strings.isNullOrEmpty(candidate.bodyNotContains)) {
                // Now we have to check that this shouldn't be present in to
                String[] bodyEmailIdsNot = candidate.bodyNotContains.split(",");
                Boolean foundBodyNotContain = false;
                for (int jj = 0; jj < bodyEmailIdsNot.length; jj++) {
                    String testBodyNot = bodyEmailIdsNot[jj].trim();
                    if (body.contains(testBodyNot)) {
                        foundBodyNotContain = true;
                        break;
                    }
                }
                if (foundBodyNotContain) {
                    // What's the point to continue further, just skip this record
                    continue;
                }
            }
            if (retCust != null) {
                log.error("More than one customer fulfilling dispatch conditions:" + retCust.name + "," + retCust.slug);
            }
            retCust = candidate; // Atlast we found, who have fulfilled every condition
        }

        return retCust;
    }

    public Customer getSlugfromEmailID(String emailGateway, String messageWithSubject) {
        // Find email Gateway from Customer collection
        // Here the issue is that sometime emailID may contain <> sign like
        // <hellotest@gmail.com> while in our database we have "hellotest@gmail.com"
        // In addition to this it may be possible that we get this with Name like "Hello
        // Test <hellotest@gmail.com>"
        // We need to write this function very carefully to cater all the needs
        log.info("in getSlugfromEmailID 2");
        log.info("emailGateway: " + emailGateway);
        log.info("messageWithSubject: " + messageWithSubject);

        List<Customer> customerList = customerDao.getAllByFieldValue("emailGateway", emailGateway);
        if (customerList.isEmpty()) {
            // There can be occasion where the emailGateway is not defined exactly same in
            // database like
            // Deepak Dhasmana <deepak.dhasmana@gmail.com> is coming on email gateway but we
            // have defined deepak.dhasmana@gmail.com
            // The above situation can be a vice-versa as well, let us handle this
            List<Customer> allCustomer = customerDao.getAll();
            String emailGatewayLowerCase = emailGateway.toLowerCase().trim();
            for (int ii = 0; ii < allCustomer.size(); ii++) {
                String dbEmail = allCustomer.get(ii).emailGateway;
                if (Strings.isNullOrEmpty(dbEmail)) {
                    continue;
                }
                dbEmail = dbEmail.toLowerCase().trim();
                if (dbEmail.equals(emailGatewayLowerCase)) {
                    // This will ensure that casing is handled
                    customerList.add(allCustomer.get(ii));
                    continue;
                }

                if (dbEmail.contains(emailGatewayLowerCase)) {
                    // It can lead to a bug
                    // dhasmana@gmail.com is found in deepak.dhasmana@gmail.com
                    customerList.add(allCustomer.get(ii));
                    continue;
                }

                if (emailGatewayLowerCase.contains(dbEmail)) {
                    // It can lead to a bug
                    // dhasmana@gmail.com is found in deepak.dhasmana@gmail.com
                    customerList.add(allCustomer.get(ii));
                    continue;
                }
            }
            // Still if the list is empty, we can't do much
            if (customerList.isEmpty()) {
                return null;
            }
        }

        Customer retCust = null;
        if (customerList.size() == 1) {
            // If there is only one customer then check the customer shouldn't have
            // emailSignature
            // This will ensure that our previous logic is intact
            Customer thisRow = customerList.get(0);
            String emailSignature = thisRow.emailSignature;
            if (Strings.isNullOrEmpty(emailSignature)) {
                retCust = thisRow;
                return retCust;
            }
        }

        // "Subject-" + gresult.get(ii).Subject + " Msg-" + gresult.get(ii).Body;
        String messageSubject = "";
        String messageBody = messageWithSubject;
        int indexSubject = messageWithSubject.indexOf("Subject-");
        int indexBody = messageWithSubject.indexOf("Msg-");
        if (indexSubject >= 0) {
            messageSubject = messageWithSubject.substring(indexSubject, indexBody).trim();
            messageBody = messageBody.substring(indexBody).trim();
        }

        for (int ii = 0; ii < customerList.size(); ii++) {
            Customer thisRow = customerList.get(ii);
            String emailSignature = thisRow.emailSignature;
            if (!Strings.isNullOrEmpty(emailSignature)) {
                String emailSignatureLocation = thisRow.emailSignatureLocation;
                if (!Strings.isNullOrEmpty(emailSignatureLocation)) {
                    if (emailSignatureLocation.equalsIgnoreCase("subject")) {
                        // Subject Line
                        if (messageSubject.contains(emailSignature)) {
                            retCust = thisRow;
                            break;
                        }
                    } else {
                        // Body
                        if (messageBody.contains(emailSignature)) {
                            retCust = thisRow;
                            break;
                        }
                    }

                } else {
                    log.debug("Email Signature is Given but Location is Empty:" + emailGateway + ":" + emailSignature);
                }
            }
        }
        return retCust;
    }

}
