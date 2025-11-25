// EJEMPLO: Services.ice con extensiones ICE
// Copia este contenido a Services.ice después de hacer backup

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

    // ============================================
    // NUEVAS ESTRUCTURAS PARA ICE
    // ============================================
    
    // Candidato ICE (dirección de red potencial)
    struct IceCandidate {
        string candidate;      // Ej: "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host"
        string sdpMid;          // Media stream ID (normalmente "0" para audio)
        int sdpMLineIndex;      // Media line index (normalmente 0)
    };
    
    sequence<IceCandidate> IceCandidateSeq;
    
    // Oferta o Respuesta SDP (Session Description Protocol)
    struct SessionDescription {
        string type;            // "offer" o "answer"
        string sdp;             // SDP completo con candidatos
    };

    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        
        // ============================================
        // NUEVOS MÉTODOS PARA RECIBIR EVENTOS ICE
        // ============================================
        
        // Se llama cuando se recibe una oferta ICE del otro usuario
        void onIceOffer(string fromUser, SessionDescription offer);
        
        // Se llama cuando se recibe una respuesta ICE del otro usuario
        void onIceAnswer(string fromUser, SessionDescription answer);
        
        // Se llama cuando se recibe un nuevo candidato ICE del otro usuario
        void onIceCandidate(string fromUser, IceCandidate candidate);
    };

    interface Call {

        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);

        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);

        void subscribe(string username, VoiceObserver* obs);

        void unsubscribe(string username, VoiceObserver* obs);
        
        // ============================================
        // NUEVOS MÉTODOS PARA ENVIAR EVENTOS ICE
        // ============================================
        
        // Envía una oferta ICE a otro usuario
        void sendIceOffer(string fromUser, string toUser, SessionDescription offer);
        
        // Envía una respuesta ICE a otro usuario
        void sendIceAnswer(string fromUser, string toUser, SessionDescription answer);
        
        // Envía un candidato ICE a otro usuario
        void sendIceCandidate(string fromUser, string toUser, IceCandidate candidate);
        
        // Para llamadas grupales (opcional, implementar después)
        void sendIceOfferToGroup(string fromUser, string groupName, SessionDescription offer);
        void sendIceCandidateToGroup(string fromUser, string groupName, IceCandidate candidate);
    };
}

