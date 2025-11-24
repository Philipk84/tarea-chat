// Proyecto/Services.ice

module Chat {

    sequence<byte> ByteSeq;

    struct VoiceEntry {
        string type;      // "voice_note" | "voice_group"
        string scope;     // "private" | "group"
        string sender;
        string recipient; // vacío si es group
        string group;     // vacío si es private
        string audioFile; // ruta relativa del .wav, ej: "voice/nota-123.wav"
    };

    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
    };

    interface Call {

        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);

        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);

        void subscribe(string username, VoiceObserver* obs);

        void unsubscribe(string username, VoiceObserver* obs);
    };
}
