package com.flowmsp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.messages.MsgSender;
import com.flowmsp.controller.psap.PsapUnitCustomerModel;
import com.flowmsp.db.FcmDataDao;
import com.flowmsp.db.UserDao;
import com.flowmsp.domain.fcmData.FcmData;
import com.flowmsp.domain.fcmData.FcmResponseResult;
import com.flowmsp.service.psap.PSAPService;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FCMService {

    private static final Logger log = LoggerFactory.getLogger(FCMService.class);

    private final UserDao userDao;
    private final FcmDataDao fcmDataDao;
    private final ObjectMapper mapper;
    private final PSAPService psapService;

    private final String TYPE_DISPATCH = "dispatch";

    public FCMService(PSAPService psapService, UserDao userDao, FcmDataDao fcmDataDao, ObjectMapper mapper) {
        this.psapService = psapService;
        this.userDao = userDao;
        this.fcmDataDao = fcmDataDao;
        this.mapper = mapper;
    }

    public void sendDispatchPushNotification(String slug, String customerId, MsgSender msgSender) {
        log.info("push notifications started - " + slug);

        Boolean slugAlreadyPresent = true;
        Optional<String> slugAlready = SlugContext.getSlug();
        if (!slugAlready.isPresent()) {
            SlugContext.setSlug(slug);
            slugAlreadyPresent = false;
        }

        List<FcmData> allFcmDataList = fcmDataDao.getAllByFieldValue("customerId", customerId);
        List<FcmData> dutyFcmDataList = userDao.getDutyFcmTokens();

        List<FcmData> fcmDataList = new ArrayList<>();
        List<FcmData> fcmDataListInFilter = new ArrayList<>();
        List<FcmData> fcmDataListNotInFilter = new ArrayList<>();

        log.debug("all tokens amount = " + allFcmDataList.size());
        log.debug("onDuty token amount = " + dutyFcmDataList.size());

        log.debug("-------------- onDuty tokens before filtering -------------");
        for (FcmData fcmData : dutyFcmDataList) {

            log.debug(fcmData.id + " " + fcmData.registrationToken + " " + fcmData.platform);

            fcmDataListNotInFilter.add(fcmData);

           /* if (msgSender.units==null || msgSender.units.isEmpty()) { // if no units in message then send it to device as NotInFilter
                fcmDataList.add(fcmData);
                fcmDataListNotInFilter.add(fcmData);
            } *//*else if (fcmData.psapUnitCustomerIds == null) { // if units still not set by user then send it to device as InFilter
                fcmDataList.add(fcmData);
                fcmDataListInFilter.add(fcmData);
            }*//* else {
                List<PsapUnitCustomerModel> psapUnitCustomerResponses = psapService.getUnitsByToken(customerId, fcmData.registrationToken);

                List<String> selectedUnitIds = new ArrayList<>();
                for (PsapUnitCustomerModel psapUnitCustomerModel : psapUnitCustomerResponses) {
                    if (psapUnitCustomerModel.selected) {
                        selectedUnitIds.add(psapUnitCustomerModel.unit);
                    }
                }

                boolean isInUnitFilter = selectedUnitIds.stream()
                        .distinct().anyMatch(msgSender.units::contains);
                if (isInUnitFilter) {
                    fcmDataList.add(fcmData);
                    fcmDataListInFilter.add(fcmData);
                }
            }*/
        }

        if (!slugAlreadyPresent) {
            SlugContext.clearSlug();
        }

        log.debug("------------------ tokens after filtering -----------------");
        log.debug("in unit amount = " + fcmDataListInFilter.size());
        for (FcmData fcmData : fcmDataListInFilter) {
            log.debug("in:" + fcmData.id + " " + fcmData.registrationToken + " " + fcmData.platform);
        }

        log.debug("out unit amount = " + fcmDataListNotInFilter.size());
        for (FcmData fcmData : fcmDataListNotInFilter) {
            log.debug("out:" + fcmData.id + " " + fcmData.registrationToken + " " + fcmData.platform);
        }
        log.debug("-----------------------------------------------------------");


        msgSender.textRaw = null;
        MsgSender msgSenderInFilter = msgSender.copy();
        msgSenderInFilter.isInUnitFilter = true;
        sendPushNotification(fcmDataListInFilter, msgSenderInFilter);

        MsgSender msgSenderNotInFilter = msgSender.copy();
        msgSenderNotInFilter.isInUnitFilter = false;
        sendPushNotification(fcmDataListNotInFilter, msgSenderNotInFilter);
    }

    private void sendPushNotification(List<FcmData> fcmDataList, MsgSender msgSender){
        if (fcmDataList.size() > 0) {
            String body = "{}";
            try {
                body = mapper.writeValueAsString(msgSender);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            RegistrationTokens registrationTokens = new RegistrationTokens(fcmDataList);
            log.debug("--------------------------- body --------------------------");
            double kilobytes = body.getBytes().length / 1024.0;
            log.debug("length: " + body.length() + " size(kb): " + kilobytes);
            log.debug(body);
            log.debug("-----------------------------------------------------------");

            PushNotificationData pushNotificationData = new PushNotificationData("New alert", "Type: " + msgSender.type + "; Address: " + msgSender.address, TYPE_DISPATCH, body);
            removeFailedRegistrationTokens(sendPushNotification(registrationTokens, Platform.IOS, pushNotificationData));
            removeFailedRegistrationTokens(sendPushNotification(registrationTokens, Platform.ANDROID, pushNotificationData));
        }
    }

    private List<FcmResponseResult> sendPushNotification(
            RegistrationTokens registrationTokens, Platform platform, PushNotificationData pushNotificationData) {

        List<FcmResponseResult> results = new ArrayList<>();

        List<String> tokensPart = registrationTokens.getNextPart(platform);

        while (tokensPart.size() > 0) {
            MulticastMessage multicastMessage = getMulticastMessage(tokensPart, platform, pushNotificationData);

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendMulticastAsync(multicastMessage).get();
                log.debug(platform.getName() + " success:" + response.getSuccessCount() + ", failure:" + response.getFailureCount());

                for (int i = 0; i < response.getResponses().size(); i++) {
                    SendResponse sendResponse = response.getResponses().get(i);
                    if (!sendResponse.isSuccessful()) {
                        results.add(new FcmResponseResult(tokensPart.get(i), sendResponse.getException().getErrorCode()));
                        log.debug(tokensPart.get(i) + ": " + "FCM error: " + sendResponse.getException().getMessage() + "; " + sendResponse.getException().getErrorCode());
                    } else {
                        results.add(new FcmResponseResult(tokensPart.get(i)));
                        log.debug(tokensPart.get(i) + ": " + "success");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            tokensPart = registrationTokens.getNextPart(platform);
        }

        return results;
    }

    private void removeFailedRegistrationTokens(List<FcmResponseResult> results) {
        for (FcmResponseResult fcmResponseResult : results) {
            if (fcmResponseResult.errorCode != null &&
                    fcmResponseResult.errorCode.equals("registration-token-not-registered")) {
                fcmDataDao.deleteAllByFieldValue("registrationToken", fcmResponseResult.registrationToken);
            }
        }
    }

    private MulticastMessage getMulticastMessage(List<String> tokens, Platform platform, PushNotificationData pushNotificationData) {
        if (platform == Platform.IOS) {
            return MulticastMessage.builder()
                    .setNotification(new Notification(pushNotificationData.title, pushNotificationData.description))
                    .setApnsConfig(getApnsConfig())
                    .putData("type", pushNotificationData.type)
                    .putData("body", pushNotificationData.body)
                    .addAllTokens(tokens)
                    .build();
        } else {
            return MulticastMessage.builder()
                    .setAndroidConfig(getAndroidConfig())
                    .putData("type", pushNotificationData.type)
                    .putData("body", pushNotificationData.body)
                    .addAllTokens(tokens)
                    .build();
        }
    }

    private AndroidConfig getAndroidConfig() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder().build()).build();
    }

    private ApnsConfig getApnsConfig() {
        return ApnsConfig.builder().setAps(Aps.builder()
                .setSound(CriticalSound.builder().setCritical(true).setName("default").setVolume(1.0).build())
                .build()).build();
    }
}

enum Platform {
    IOS("ios"),
    ANDROID("android");

    private String name;

    Platform(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class PushNotificationData {
    String title;
    String description;
    String type;
    String body;

    PushNotificationData(String title, String description, String type, String body) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.body = body;
    }
}

class RegistrationTokens {
    private static final Logger log = LoggerFactory.getLogger(FCMService.class);
    private final int TOKENS_SIZE_LIMIT = 100;

    private List<String> iosTokens = new ArrayList<>();
    private List<String> androidTokens = new ArrayList<>();

    private int iosPage = 0;
    private int androidPage = 0;

    RegistrationTokens(List<FcmData> fcmDataList) {
        log.info("registrationTokens.size()=" + fcmDataList.size());

        for (FcmData fcmData : fcmDataList) {
            if (fcmData.platform.equalsIgnoreCase("ios")) {
                iosTokens.add(fcmData.registrationToken);
            } else if (fcmData.platform.equalsIgnoreCase("android")) {
                androidTokens.add(fcmData.registrationToken);
            }
        }

        log.info("iosTokens.size()=" + iosTokens.size() + " androidTokens.size()=" + androidTokens.size());
    }

    List<String> getNextPart(Platform platform) {
        List<String> result = new ArrayList<>();

        List<String> tokens = platform == Platform.IOS ? iosTokens : androidTokens;
        int page = platform == Platform.IOS ? iosPage : androidPage;
        int nextPage = page + 1;
        int startIndex = page * TOKENS_SIZE_LIMIT;
        int endIndex = Math.min(nextPage * TOKENS_SIZE_LIMIT, tokens.size());

        if (endIndex > startIndex)
            result.addAll(tokens.subList(startIndex, endIndex));

        if (platform == Platform.IOS) {
            iosPage++;
        } else {
            androidPage++;
        }

        return result;
    }
}