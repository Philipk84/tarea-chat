package model;

import java.io.Serializable;

public class VoiceNote implements Serializable {
    private final String sender;
    private final byte[] audioData;

    public VoiceNote(String sender, byte[] audioData) {
        this.sender = sender;
        this.audioData = audioData;
    }

    public String getSender() {
        return sender;
    }

    public byte[] getAudioData() {
        return audioData;
    }
}
