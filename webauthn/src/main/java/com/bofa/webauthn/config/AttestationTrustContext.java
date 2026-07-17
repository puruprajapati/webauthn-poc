package com.bofa.webauthn.config;

public class AttestationTrustContext {
    public enum Status{
        // certificate chain validated agains t a FIDO MDS trust anchor
        TRUSTED,
        // certificate present but no MDS trust anchor found, allowed (PREFFERED mode)
        UNTRUSTED_ALLOWED,
        // certificate path trust was not evaluated (NONE or verification disabled)
        NOT_CHECKED
    }

    private static final ThreadLocal<Status> STATUS = new ThreadLocal<>();
    private AttestationTrustContext(){}

    public static void setStatus(Status status){
        STATUS.set(status);
    }

    public static Status getStatus(){
        return STATUS.get();
    }

    public static void clearStatus(){
        STATUS.remove();
    }

}
