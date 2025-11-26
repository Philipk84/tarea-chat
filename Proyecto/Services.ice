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

    struct CallEvent {
        string type;      // "call_started" | "call_incoming" | "call_ended" | "call_rejected"
        string callId;
        string caller;
        string callee;    // vacío si es grupo
        string group;     // vacío si es privado
        string scope;     // "private" | "group"
    };

    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        void onCallChunk(CallChunk chunk);
        void onCallEvent(CallEvent event);
    };

    interface Call {

        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);

        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);

        void subscribe(string username, VoiceObserver* obs);

        void unsubscribe(string username, VoiceObserver* obs);

        void sendCallChunk(string callId, string fromUser, ByteSeq audio);

        // Gestión de llamadas
        string startCall(string caller, string callee);
        string startGroupCall(string caller, string groupName);
        void acceptCall(string callId, string user);
        void rejectCall(string callId, string user);
        void endCall(string callId, string user);
    };
}
