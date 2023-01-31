package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.service.Message.MessageService;

public interface MsgParser {
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService);
}
