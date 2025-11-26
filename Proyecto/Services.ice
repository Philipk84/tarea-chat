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

    struct CallChunk {
        string callId;
        string fromUser;
        ByteSeq audio;  // trocito PCM16
    };


    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        void onCallChunk(CallChunk chunk);

    };

    interface Call {

        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);

        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);

        void subscribe(string username, VoiceObserver* obs);

        void unsubscribe(string username, VoiceObserver* obs);

        void sendCallChunk(string callId, string fromUser, ByteSeq audio);

    };
}
