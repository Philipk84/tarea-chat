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

    // ============================================
    // ESTRUCTURAS PARA WEBRTC/ICE
    // ============================================
    
    // Oferta o Respuesta SDP (Session Description Protocol)
    struct SessionDescription {
        string type;            // "offer" o "answer"
        string sdp;             // SDP completo generado por WebRTC
    };
    
    // Candidato ICE (dirección de red potencial)
    // Contiene el candidato completo en formato SDP para WebRTC
    struct Candidate {
        string candidate;       // Candidato completo en formato SDP: "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host"
        string sdpMid;          // Media stream ID (normalmente "0" para audio)
        int sdpMLineIndex;      // Media line index (normalmente 0)
    }
    sequence<Candidate> CandidateSeq;


    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        
        // ============================================
        // MÉTODOS PARA RECIBIR EVENTOS ICE
        // ============================================
        
        // Se llama cuando se recibe una oferta WebRTC
        void onIceOffer(string fromUser, SessionDescription offer);
        
        // Se llama cuando se recibe una respuesta WebRTC
        void onIceAnswer(string fromUser, SessionDescription answer);
        
        // Se llama cuando se recibe un nuevo candidato ICE
        void onIceCandidate(string fromUser, Candidate candidate);
        
        // Se llama cuando se inicia una llamada
        void onCallIncoming(string fromUser);
        
        // Se llama cuando se termina una llamada
        void onCallEnded(string fromUser);
    };

    interface Call {

        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);

        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);

        void subscribe(string username, VoiceObserver* obs);

        void unsubscribe(string username, VoiceObserver* obs);
        
        // ============================================
        // MÉTODOS PARA ENVIAR EVENTOS ICE
        // ============================================
        
        // Inicia una llamada con otro usuario
        void initiateCall(string fromUser, string toUser);
        
        // Acepta una llamada entrante
        void acceptCall(string fromUser, string toUser);
        
        // Rechaza una llamada entrante
        void rejectCall(string fromUser, string toUser);
        
        // Termina una llamada activa
        void endCall(string fromUser, string toUser);
        
        // Envía una oferta WebRTC
        void sendIceOffer(string fromUser, string toUser, SessionDescription offer);
        
        // Envía una respuesta WebRTC
        void sendIceAnswer(string fromUser, string toUser, SessionDescription answer);
        
        // Envía un candidato ICE
        void sendIceCandidate(string fromUser, string toUser, Candidate candidate);
    };
}
