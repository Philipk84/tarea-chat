package model;

import java.io.Serializable;

public class VoiceNote implements Serializable {
    private final String fromUser;
    private final String target;
    private final byte[] audioData;
    private final boolean isGroup;

    public VoiceNote(String fromUser, String target, byte[] audioData, boolean isGroup) {
        this.fromUser = fromUser;
        this.target = target;
        this.audioData = audioData;
        this.isGroup = isGroup;
    }

    public String getFromUser() {
        return fromUser;
    }

    public String getTarget() {
        return target;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public boolean isGroup() {
        return isGroup;
    }
}
