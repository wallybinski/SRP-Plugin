package com.srp.recovery;

import java.util.UUID;

public class RecoveryRequest {
    private final UUID targetUuid;
    private final String targetName;
    private final String targetNick;
    private final long expiresAt;

    public RecoveryRequest(UUID targetUuid, String targetName, String targetNick, long expiresAt) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetNick = targetNick;
        this.expiresAt = expiresAt;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetNick() {
        return targetNick;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
