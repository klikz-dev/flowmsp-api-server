package com.flowmsp.domain.auth;

import com.flowmsp.domain.user.User;
import com.flowmsp.domain.customer.Customer;
import org.mindrot.jbcrypt.BCrypt;

/**
 * The Password document contains login information for all users of the system. The id of the Password
 * document is the same as the id of the corresponding user. This structure allows login processing to occur
 * without knowning a customer id while still allowing detailed user information to be stored in customer-specific
 * collections.
 */
public class Password {
    public String id;
    public String username;
    public String password;
    public String customerId;
    public String customerSlug;

    public Password() {

    }

    public Password(User user, String password, Customer customer) {
        this.id           = user.id;
        this.username     = user.email;
        this.password     = password;
        this.customerId   = customer.id;
        this.customerSlug = customer.slug;
    }

    /**
     * Given an uncrypted password, encrypt it using BCrypt.
     */
    public static String encryptPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
