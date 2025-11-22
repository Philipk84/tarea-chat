module Chat {

    // Bytes de audio (PCM 16-bit, 44100 Hz, mono) o el formato
    sequence<byte> ByteSeq;

    struct VoiceEntry {
        string type;      // "voice_note" o "voice_group"
        string scope;     // "private" o "group"
        string sender;
        string recipient; // vacío si es de grupo
        string group;     // vacío si es privado
        string audioFile; // "server/data/voice/xxxxx.wav"
    };

    // Callback que el servidor usará para empujar eventos de voz
    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
    };

    // Servicio ICE SOLO para voz/notas de voz/llamadas
    interface Call {
        // Notas de voz
        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);
        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);

        // Más adelante puedes extenderlo para llamadas en tiempo real
        // (chunks, start/end, etc.)

        // Suscripción a eventos de voz (tiempo real por ws)
        void subscribe(string username, VoiceObserver* obs);
        void unsubscribe(string username, VoiceObserver* obs);
    };
}
