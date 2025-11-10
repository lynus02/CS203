package com.lynus.cs203.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AuditResponse {
    private boolean integrityOk;

    @JsonIgnore
    private String localHash;

    @JsonIgnore
    private String onChainHash;
    private boolean error;
    private String message;

    public AuditResponse() {}

    public AuditResponse(boolean integrityOk, String localHash, String onChainHash, boolean error, String message) {
        this.integrityOk = integrityOk;
        this.localHash = localHash;
        this.onChainHash = onChainHash;
        this.error = error;
        this.message = message;
    }

    public boolean isIntegrityOk() { return integrityOk; }
    public void setIntegrityOk(boolean integrityOk) { this.integrityOk = integrityOk; }

    public String getLocalHash() { return localHash; }
    public void setLocalHash(String localHash) { this.localHash = localHash; }

    public String getOnChainHash() { return onChainHash; }
    public void setOnChainHash(String onChainHash) { this.onChainHash = onChainHash; }

    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
