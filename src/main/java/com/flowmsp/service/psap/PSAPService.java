package com.flowmsp.service.psap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.controller.psap.PsapUnitCustomerModel;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.FcmDataDao;
import com.flowmsp.db.PSAPDao;
import com.flowmsp.db.PsapUnitCustomerDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.fcmData.FcmData;
import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.domain.psap.PsapUnitCustomer;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PSAPService {
    private static final Logger log = LoggerFactory.getLogger(PSAPService.class);

    private final CustomerDao customerDao;
    private final PSAPDao psapDao;
    private final PsapUnitCustomerDao psapUnitCustomerDao;
    private final FcmDataDao fcmDataDao;
    private final ObjectMapper objectMapper;


    public PSAPService(CustomerDao customerDao, PSAPDao psapDao, PsapUnitCustomerDao psapUnitCustomerDao,
                       FcmDataDao fcmDataDao, ObjectMapper objectMapper) {
        this.customerDao = customerDao;
        this.psapDao = psapDao;
        this.psapUnitCustomerDao = psapUnitCustomerDao;
        this.fcmDataDao = fcmDataDao;
        this.objectMapper = objectMapper;
    }

    public PSAP getPSAPfromEmailID(String from) {
        List<PSAP> psapList = psapDao.getAllByFieldValue("emailGateway", from);
        if (psapList.isEmpty()) {
            return null;
        }
        return psapList.get(0);
    }

    public Customer getCustomerFromPsapUnitCustomer(PsapUnitCustomer psapUnitCustomer) {
        String customerId = psapUnitCustomer.customerId;
        Optional<Customer> customerOpt = customerDao.getById(customerId);
        return customerOpt.orElse(null);
    }

    public Optional<PsapUnitCustomer> getPsapUnitCustomerFromPsapUnit(String unit, String psapId) {
        List<PsapUnitCustomer> psapUnitCustomerList = psapUnitCustomerDao.getAllSortByFilter(Filters.and(Filters.eq("unit", unit), Filters.eq("psapId", psapId)), 1);

        Optional<PsapUnitCustomer> psapUnitCustomerOpt;
        if (!psapUnitCustomerList.isEmpty()) {
            psapUnitCustomerOpt = Optional.of(psapUnitCustomerList.get(0));
        } else {
            psapUnitCustomerOpt = Optional.empty();
        }

        return psapUnitCustomerOpt;
    }

    public Customer getCustomerFromUnit(String unit, String psapId) {
        Optional<PsapUnitCustomer> psapUnitCustomer = getPsapUnitCustomerFromPsapUnit(unit, psapId);
        return psapUnitCustomer.map(this::getCustomerFromPsapUnitCustomer).orElse(null);
    }

    public List<PsapUnitCustomerModel> getUnitsByToken(String customerId, String registrationToken) {
        List<PsapUnitCustomerModel> psapUnitCustomerModels = new ArrayList<>();

        List<PsapUnitCustomer> psapUnitCustomerList = psapUnitCustomerDao.getAllByFieldValue("customerId", customerId);
        List<FcmData> fcmDataList = fcmDataDao.getAllByFieldValue("registrationToken", registrationToken);

        for (PsapUnitCustomer psapUnitCustomer : psapUnitCustomerList) {
            boolean isSelected = false;
            if (!fcmDataList.isEmpty()) {
                if (fcmDataList.get(0).psapUnitCustomerIds != null) {
                    for (String psapUnitCustomerId : fcmDataList.get(0).psapUnitCustomerIds) {
                        if (psapUnitCustomer.id.equals(psapUnitCustomerId)) {
                            isSelected = true;
                            break;
                        }
                    }
                } else {
                    isSelected = true;
                }
            }
            PsapUnitCustomerModel psapUnitCustomerModel = PsapUnitCustomerModel.map(psapUnitCustomer);
            psapUnitCustomerModel.selected = isSelected;
            psapUnitCustomerModels.add(psapUnitCustomerModel);
        }

        return psapUnitCustomerModels;
    }

    public void saveDispatchFilter(String registrationToken, List<String> psapUnitCustomerIds) {
        Optional<FcmData> fcmDataOpt = fcmDataDao.getByFieldValue("registrationToken", registrationToken);

        if (fcmDataOpt.isPresent()) {
            Bson newValue = new Document("psapUnitCustomerIds", psapUnitCustomerIds);
            Bson updateOperationDocument = new Document("$set", newValue);
            fcmDataDao.updateById(fcmDataOpt.get().id.toString(), updateOperationDocument);
        } else {
            //should we create new item in fcmDataDao?
        }

    }

}
