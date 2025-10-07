package model;
import java.io.Serializable;

public class VoiceNote implements Serializable {
    private String sender;
    private String recipient;
    private byte[] audioData;

    public VoiceNote(String sender, String recipient, byte[] audioData) {
        this.sender = sender;
        this.recipient = recipient;
        this.audioData = audioData;
    }

    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public byte[] getAudioData() { return audioData; }
}