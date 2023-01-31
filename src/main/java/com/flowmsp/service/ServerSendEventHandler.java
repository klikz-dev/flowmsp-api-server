package com.flowmsp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;

import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DispatchRegisterDao;
import com.flowmsp.db.PartnersDao;
import com.flowmsp.domain.DispatchRegister;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.partners.Partners;
import com.flowmsp.service.Message.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSendEventHandler extends EventSourceServlet{
    /**
     * The serial version UID is generated automatically otherwise a warning was persistent  
     */
    private static final long serialVersionUID = 1L;
    private static ServerSendEventHandler myInstance = null;
    private static Map<String, EventSource.Emitter> emitters = null;
    private static Map<String, String> customers = null;
    private static Map<String, String> originalJwt = null;
    private static Map<String, String> otherSessions = null;
    private static CustomerDao customerDao = null;
    private static DispatchRegisterDao dispatchDao = null;
    private static PartnersDao partnersDao = null;
    private static MessageService messageService = null;
    private static final Logger log = LoggerFactory.getLogger(ServerSendEventHandler.class);
	
	public ServerSendEventHandler() {
	    //EventSourceServletContextHandlerFactory is failing In case the constructor is kept as private
	}
	
	public static ServerSendEventHandler GetMyInstance(PartnersDao pPartnersDao, DispatchRegisterDao pDispatchDao, CustomerDao pcustomerDao, MessageService pmessageService) {
	    if (myInstance == null) {
		myInstance = new ServerSendEventHandler();
		emitters = new ConcurrentHashMap<String, EventSource.Emitter>();
		customers = new ConcurrentHashMap<String, String>();
		originalJwt = new ConcurrentHashMap<String, String>();
		otherSessions = new ConcurrentHashMap<String, String>();
		dispatchDao = pDispatchDao;
		customerDao =  pcustomerDao;
		partnersDao = pPartnersDao;
		messageService = pmessageService;
	    }		

	    return myInstance;
	}

	public static ServerSendEventHandler GetMyInstance() {		
		return GetMyInstance(null, null, null, null);
	}

	public static void ReloadRegisteredDispatch( ) {
	    otherSessions.clear();
	    List<DispatchRegister> dispatchRegister = dispatchDao.getAll();		

	    log.info("in ReloadRegisteredDispatch. dispatchRegister.size()=" + dispatchRegister.size());

	    for (int ii = 0; ii < dispatchRegister.size(); ii ++) {
		DispatchRegister masterRow = dispatchRegister.get(ii);
		List<Partners> partners = partnersDao.getAllByFieldValue("customerId", masterRow.customerId);

		log.info(ii + "customerId=" + masterRow.customerId + " partners.size()=" + partners.size());

		for (int jj = 0; jj < partners.size(); jj ++) {
		    Partners childRow = partners.get(jj);
		    Optional<Customer> part = customerDao.getById(childRow.partnerId);
		    if (!part.isPresent()) {
			continue;
		    }
		    Customer partner = part.get();
		    otherSessions.put(partner.name + "|" + masterRow.jwt + "|" + masterRow.customerId + "|" + childRow.partnerId, childRow.partnerId);

		    log.info("partner.name=" + partner.name);
		}
	    }
	}
	
	public static void SendData(String CustomerID, String data, Long msgSequence) {
	    log.info("in ServerSendEventHandler.SendData: data=" + data);

	    //Search all jwt with value CustomerID
	    Iterator<Entry<String, String>> thisCustomer = customers.entrySet()
		.stream()
		.filter(x -> x.getValue().equals(CustomerID)).iterator();

	    while(thisCustomer.hasNext()) {
		Entry<String, String> row = thisCustomer.next();
		String key = row.getKey();
		
		log.info("key=" + key);

	        if (emitters.containsKey(key)) {
		    EventSource.Emitter emitter = emitters.get(key);
		    try	{
			emitter.data(data);	        			
		    } catch (Exception e) {	        			
			try {emitter.close();} catch (Exception ex){}	        		
			emitters.remove(key);
			customers.remove(key);
			originalJwt.remove(key);
		    }
	        } else {
		    customers.remove(key);
		    originalJwt.remove(key);
	        }
	    }
		
	    //search other sessions as well
	    Iterator<Entry<String, String>> others = otherSessions.entrySet()
		.stream()
		.filter(x -> x.getValue().equals(CustomerID)).iterator();

	    while(others.hasNext()) {
		Entry<String, String> row = others.next();
		String jwt = row.getKey();
		//key has two parts to make it unique
		//First Should be TimeStamp 
		String[] arr = jwt.split(Pattern.quote("|"));
		String name = arr[0];
		String jwtKey = arr[1];
		Iterator<Entry<String, String>> othersInner = originalJwt.entrySet()
		    .stream()
		    .filter(x -> x.getValue().equals(jwtKey)).iterator();
		
		while(othersInner.hasNext()) {
		    Entry<String, String> rowInner = othersInner.next();
		    String key = rowInner.getKey();
		    if (emitters.containsKey(key)) {
			EventSource.Emitter emitter = emitters.get(key);
			try	{
			    emitter.data(data);	        			
			} catch (Exception e) {
			    try {emitter.close();} catch (Exception ex){}	        		
			    emitters.remove(key);
			    otherSessions.remove(key);
			    originalJwt.remove(key);
			}
		    } else {
			otherSessions.remove(key);
			originalJwt.remove(key);
		    }
		}
	    }
	}
    
    @Override
    protected EventSource newEventSource(HttpServletRequest request) {
        return new EventSource() {
            Emitter emmitter;
            @Override
            public void onOpen(Emitter emitter) throws IOException {
                this.emmitter = emitter;                
                String customerID = request.getParameter("CustomerID");
                String jwt = request.getParameter("jwt");
                if (jwt != null ) {
		    String origJwt = jwt;
		    if (emitters.containsKey(jwt)) {
			jwt = jwt + new Random().nextInt();
		    }
		    emitters.put(jwt, emitter);
		    customers.put(jwt, customerID);
		    originalJwt.put(jwt, origJwt);
		    
		    Optional<Customer> cust = customerDao.getById(customerID);
		    if (cust.isPresent()) {
                    	//Should've sent 10 backlog messages                    	
                    	Customer c = cust.get();
                    	ArrayList<String> msgList = messageService.getMessagesOfCustomer(customerID, c.slug, 0L);
                    	for(String msg : msgList) {
			    emitter.event("backlog", msg);
                    	}
                    	if (c.dispatchSharingConsent) {
			    //Check if there are any partners saved with him
			    //Send there data as well
			    List<Partners> partners = partnersDao.getAllByFieldValue("customerId", customerID);
			    for (int jj = 0; jj < partners.size(); jj ++) {
				Partners childRow = partners.get(jj);
				Optional<Customer> part = customerDao.getById(childRow.partnerId);
				if (!part.isPresent()) {
				    continue;
				}
				Customer partner = part.get();
                            	ArrayList<String> msgListChild = messageService.getMessagesOfCustomer(partner.id, partner.slug, 0L);
                            	for(String msg : msgListChild) {                            		
				    emitter.event("backlog", msg);
                            	}
			    }
                    	}
		    }
                }
            }
            
            @Override
            public void onClose() {
            	Iterator<Entry<String, Emitter>> thisEmitter = emitters.entrySet()
            		       .stream().iterator();
            	while(thisEmitter.hasNext()) {
            		Entry<String, Emitter> row = thisEmitter.next();
            		if (row.getValue() == this.emmitter) {
            			String key = row.getKey();
            			emitters.remove(key);
            			customers.remove(key);
            			originalJwt.remove(key);
            			break;
            		}
            	}                
                this.emmitter = null;
            }
        };
    }
}
