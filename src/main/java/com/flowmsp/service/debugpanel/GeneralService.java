package com.flowmsp.service.debugpanel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flowmsp.SlugContext;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.UserDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.debugpanel.debugUser;
import com.flowmsp.domain.user.User;
import com.google.common.base.Strings;

public class GeneralService {
	private static final Logger log = LoggerFactory.getLogger(GeneralService.class);

    private final CustomerDao  customerDao;
    private final UserDao userDao;

    public GeneralService(CustomerDao  customerDao, UserDao userDao) {
        this.customerDao  = customerDao;
        this.userDao = userDao;
    }
    
    public List<debugUser> getAllUsersInCustomer(String customerSlug) {
    	List<debugUser> userList = new ArrayList();
    	try {
    		SlugContext.setSlug(customerSlug);
    		String customerName = "";
    		String customerAddress = "";
    		List<Customer> custList = customerDao.getAllByFieldValue("slug", customerSlug);
    		if (custList.size() > 0) {
        		Customer thisCust =  custList.get(0);
        		customerName = thisCust.name;
        		if (thisCust.address != null) {
        			if (!Strings.isNullOrEmpty(thisCust.address.address1)) {
        				customerAddress = customerAddress + thisCust.address.address1 + " ";
        			}
        			if (!Strings.isNullOrEmpty(thisCust.address.address2)) {
        				customerAddress = customerAddress + thisCust.address.address2 + " ";
        			}
        			if (!Strings.isNullOrEmpty(thisCust.address.city)) {
        				customerAddress = customerAddress + thisCust.address.city + " ";
        			}
        			if (!Strings.isNullOrEmpty(thisCust.address.state)) {
        				customerAddress = customerAddress + thisCust.address.state + " ";
        			}
        			if (!Strings.isNullOrEmpty(thisCust.address.zip)) {
        				customerAddress = customerAddress + thisCust.address.zip;
        			}
        			customerAddress = customerAddress.trim();
        		}    			
    		}
    		List<User> usr = userDao.getAll();
    		for (int ii = 0; ii < usr.size(); ii ++) {
    			debugUser usrRow = new debugUser();
    			User thisRow = usr.get(ii);
    			usrRow.id = thisRow.id;
    			usrRow.email = thisRow.email;
    			usrRow.name = thisRow.firstName + " " + thisRow.lastName;
    			usrRow.customerSlug = customerSlug;
    			usrRow.customerName = customerName;
    			usrRow.customerAddress = customerAddress;
    			userList.add(usrRow);
    		}
    		
    	} catch (Exception ex) {
    		log.error("Error getAllUsers:", ex);
    	} finally {
    		SlugContext.clearSlug();
    	}
    	return userList;
    }
    
    public List<debugUser> getAllUsers() {
    	List<debugUser> userList = new ArrayList();
    	List<Customer> cust = customerDao.getAll();
    	for (int ii = 0; ii < cust.size(); ii ++) {
    		userList.addAll(getAllUsersInCustomer(cust.get(ii).slug));
    	}
    	return userList;
    }
}
