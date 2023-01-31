package com.flowmsp.service.user;

/**
 * Body of the simple post to the /api/slug/user/id/password endpoint to change a password when the current password
 * is known.
 */
public class PasswordChangeRequest {
    public String currentPassword;
    public String newPassword;
}
