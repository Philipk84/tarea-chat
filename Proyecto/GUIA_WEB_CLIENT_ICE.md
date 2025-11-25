# 📞 Guía: Llamadas en Tiempo Real desde Web-Client con ICE

## 🎯 Objetivo

Implementar llamadas de voz en tiempo real desde el web-client usando:
- **WebRTC** (nativo del navegador)
- **ICE** para NAT traversal
- **Conexión directa al servidor** (no al proxy) para señalización
- **Audio P2P** entre los dos clientes web

---

## 📋 Requisitos

- Navegador moderno con soporte WebRTC (Chrome, Firefox, Edge)
- Servidor STUN (usaremos uno público para desarrollo)
- Servidor Java con ZeroC Ice funcionando

---

## 🏗️ Arquitectura

```
┌─────────────┐                    ┌──────────────┐                    ┌─────────────┐
│  Web Client  │                    │   Servidor   │                    │  Web Client │
│     A        │                    │  (Señalizador)│                    │     B       │
└─────────────┘                    └──────────────┘                    └─────────────┘
       │                                  │                                  │
       │ 1. /call usuarioB                │                                  │
       ├──────────────────────────────────►│                                  │
       │                                  │ 2. notificar llamada             │
       │                                  ├─────────────────────────────────►│
       │                                  │                                  │
       │ 3. createOffer()                 │                                  │
       │    (WebRTC + ICE)                │                                  │
       │                                  │                                  │
       │ 4. sendIceOffer(offer)           │                                  │
       ├──────────────────────────────────►│                                  │
       │                                  │ 5. onIceOffer(offer)             │
       │                                  ├─────────────────────────────────►│
       │                                  │                                  │
       │                                  │ 6. createAnswer()                │
       │                                  │    (WebRTC + ICE)                │
       │                                  │                                  │
       │                                  │ 7. sendIceAnswer(answer)         │
       │                                  │◄─────────────────────────────────┤
       │ 8. onIceAnswer(answer)           │                                  │
       │◄─────────────────────────────────┤                                  │
       │                                  │                                  │
       │ 9. ICE Connectivity Checks       │                                  │
       │    (automático por WebRTC)       │                                  │
       │                                  │                                  │
       │ 10. ✓ Conexión P2P establecida  │                                  │
       │     Audio fluye directamente     │                                  │
       │     (NO pasa por servidor)       │                                  │
```

**Puntos clave:**
- El servidor solo actúa como **señalizador** (intercambia ofertas/respuestas)
- El audio fluye **directamente P2P** entre los clientes
- **NO pasa por el proxy** ni por el servidor

---

## 📝 Paso 1: Extender Services.ice

Primero, necesitamos agregar métodos para intercambiar ofertas/respuestas SDP.

**Modificar: `Services.ice`**

```slice
module Chat {
    
    sequence<byte> ByteSeq;
    
    struct VoiceEntry {
        string type;
        string scope;
        string sender;
        string recipient;
        string group;
        string audioFile;
    };
    
    // ============================================
    // NUEVAS ESTRUCTURAS PARA WEBRTC/ICE
    // ============================================
    
    // Oferta o Respuesta SDP (Session Description Protocol)
    struct SessionDescription {
        string type;            // "offer" o "answer"
        string sdp;             // SDP completo generado por WebRTC
    };
    
    // Candidato ICE (dirección de red potencial)
    struct IceCandidate {
        string candidate;       // Candidato en formato SDP
        string sdpMid;          // Media stream ID
        int sdpMLineIndex;      // Media line index
    };
    
    sequence<IceCandidate> IceCandidateSeq;
    
    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        
        // ============================================
        // NUEVOS MÉTODOS PARA RECIBIR EVENTOS ICE
        // ============================================
        
        // Se llama cuando se recibe una oferta WebRTC
        void onIceOffer(string fromUser, SessionDescription offer);
        
        // Se llama cuando se recibe una respuesta WebRTC
        void onIceAnswer(string fromUser, SessionDescription answer);
        
        // Se llama cuando se recibe un nuevo candidato ICE
        void onIceCandidate(string fromUser, IceCandidate candidate);
        
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
        // NUEVOS MÉTODOS PARA ENVIAR EVENTOS ICE
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
        void sendIceCandidate(string fromUser, string toUser, IceCandidate candidate);
    };
}
```

**Después de modificar, regenera:**
```bash
cd web-client
npm run build
```

---

## 📝 Paso 2: Extender CallI en el Servidor

**Modificar: `server/src/main/java/rpc/CallI.java`**

Agrega estos métodos a la clase existente:

```java
// ... código existente ...

@Override
public void initiateCall(String fromUser, String toUser, Current current) {
    System.out.println("[ICE] Llamada iniciada: " + fromUser + " → " + toUser);
    
    VoiceObserverPrx observer = observers.get(toUser);
    if (observer != null) {
        try {
            observer.onCallIncoming(fromUser);
        } catch (Exception e) {
            System.err.println("[ICE] Error notificando llamada: " + e.getMessage());
        }
    }
}

@Override
public void acceptCall(String fromUser, String toUser, Current current) {
    System.out.println("[ICE] Llamada aceptada: " + toUser + " acepta llamada de " + fromUser);
    // Notificar al iniciador que la llamada fue aceptada
    VoiceObserverPrx observer = observers.get(fromUser);
    if (observer != null) {
        try {
            observer.onCallIncoming(toUser); // Reutilizamos para notificar aceptación
        } catch (Exception e) {
            System.err.println("[ICE] Error: " + e.getMessage());
        }
    }
}

@Override
public void rejectCall(String fromUser, String toUser, Current current) {
    System.out.println("[ICE] Llamada rechazada: " + toUser + " rechaza llamada de " + fromUser);
    VoiceObserverPrx observer = observers.get(fromUser);
    if (observer != null) {
        try {
            observer.onCallEnded(toUser);
        } catch (Exception e) {
            System.err.println("[ICE] Error: " + e.getMessage());
        }
    }
}

@Override
public void endCall(String fromUser, String toUser, Current current) {
    System.out.println("[ICE] Llamada terminada: " + fromUser + " termina llamada con " + toUser);
    
    // Notificar a ambos usuarios
    VoiceObserverPrx observer1 = observers.get(toUser);
    VoiceObserverPrx observer2 = observers.get(fromUser);
    
    if (observer1 != null) {
        try {
            observer1.onCallEnded(fromUser);
        } catch (Exception e) {
            System.err.println("[ICE] Error: " + e.getMessage());
        }
    }
    
    if (observer2 != null) {
        try {
            observer2.onCallEnded(toUser);
        } catch (Exception e) {
            System.err.println("[ICE] Error: " + e.getMessage());
        }
    }
}

@Override
public void sendIceOffer(String fromUser, String toUser, SessionDescription offer, Current current) {
    System.out.println("[ICE] Oferta recibida de " + fromUser + " para " + toUser);
    
    VoiceObserverPrx observer = observers.get(toUser);
    if (observer != null) {
        try {
            observer.onIceOffer(fromUser, offer);
        } catch (Exception e) {
            System.err.println("[ICE] Error enviando oferta: " + e.getMessage());
        }
    }
}

@Override
public void sendIceAnswer(String fromUser, String toUser, SessionDescription answer, Current current) {
    System.out.println("[ICE] Respuesta recibida de " + fromUser + " para " + toUser);
    
    VoiceObserverPrx observer = observers.get(toUser);
    if (observer != null) {
        try {
            observer.onIceAnswer(fromUser, answer);
        } catch (Exception e) {
            System.err.println("[ICE] Error enviando respuesta: " + e.getMessage());
        }
    }
}

@Override
public void sendIceCandidate(String fromUser, String toUser, IceCandidate candidate, Current current) {
    VoiceObserverPrx observer = observers.get(toUser);
    if (observer != null) {
        try {
            observer.onIceCandidate(fromUser, candidate);
        } catch (Exception e) {
            System.err.println("[ICE] Error enviando candidato: " + e.getMessage());
        }
    }
}
```

---

## 📝 Paso 3: Crear Servicio de Llamadas WebRTC

**Crear: `web-client/src/services/callService.js`**

Este servicio maneja toda la lógica de WebRTC/ICE:

```javascript
import voiceDelegate from "./voiceDelegate.js";
import * as Slice from "../ice/Services.js";

class CallService {
  constructor() {
    this.peerConnection = null;
    this.localStream = null;
    this.remoteStream = null;
    this.currentCall = null; // { fromUser, toUser, isInitiator }
    this.callbacks = {
      onIncomingCall: [],
      onCallAccepted: [],
      onCallRejected: [],
      onCallEnded: [],
      onRemoteStream: [],
      onCallStateChange: [],
    };
    
    // Configuración STUN (servidor público)
    this.rtcConfiguration = {
      iceServers: [
        { urls: "stun:stun.l.google.com:19302" },
        { urls: "stun:stun1.l.google.com:19302" },
      ],
    };
  }

  /**
   * Inicializa el servicio y suscribe a eventos del servidor
   */
  async init(username) {
    // Suscribirse a eventos ICE del servidor
    voiceDelegate.subscribe((entry) => {
      // Esto es para notas de voz, no para llamadas
    });

    // Extender VoiceObserver para recibir eventos de llamadas
    // Esto se hace en voiceDelegate.js
    console.log("[CallService] Inicializado para:", username);
  }

  /**
   * Inicia una llamada con otro usuario
   */
  async startCall(fromUser, toUser) {
    if (this.currentCall) {
      throw new Error("Ya hay una llamada activa");
    }

    console.log(`[CallService] Iniciando llamada: ${fromUser} → ${toUser}`);

    try {
      // 1. Notificar al servidor que iniciamos una llamada
      await voiceDelegate.callPrx.initiateCall(fromUser, toUser);

      // 2. Obtener stream de audio local
      this.localStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false,
      });

      // 3. Crear PeerConnection
      this.peerConnection = new RTCPeerConnection(this.rtcConfiguration);

      // 4. Agregar stream local
      this.localStream.getTracks().forEach((track) => {
        this.peerConnection.addTrack(track, this.localStream);
      });

      // 5. Configurar event handlers
      this.setupPeerConnectionHandlers();

      // 6. Crear oferta
      const offer = await this.peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: false,
      });

      await this.peerConnection.setLocalDescription(offer);

      // 7. Enviar oferta al servidor
      const offerSdp = new Slice.Chat.SessionDescription("offer", offer.sdp);
      await voiceDelegate.callPrx.sendIceOffer(fromUser, toUser, offerSdp);

      // 8. Guardar estado de llamada
      this.currentCall = {
        fromUser,
        toUser,
        isInitiator: true,
      };

      console.log("[CallService] Oferta enviada");
    } catch (error) {
      console.error("[CallService] Error iniciando llamada:", error);
      this.cleanup();
      throw error;
    }
  }

  /**
   * Maneja una llamada entrante
   */
  async handleIncomingCall(fromUser) {
    console.log(`[CallService] Llamada entrante de: ${fromUser}`);

    if (this.currentCall) {
      console.log("[CallService] Ya hay una llamada activa, rechazando");
      await this.rejectCall(fromUser);
      return;
    }

    // Notificar a los callbacks
    this.callbacks.onIncomingCall.forEach((cb) => {
      try {
        cb(fromUser);
      } catch (err) {
        console.error("[CallService] Error en callback:", err);
      }
    });
  }

  /**
   * Acepta una llamada entrante
   */
  async acceptCall(fromUser, toUser) {
    console.log(`[CallService] Aceptando llamada de: ${fromUser}`);

    try {
      // 1. Notificar al servidor
      await voiceDelegate.callPrx.acceptCall(toUser, fromUser);

      // 2. Obtener stream de audio local
      this.localStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false,
      });

      // 3. Crear PeerConnection
      this.peerConnection = new RTCPeerConnection(this.rtcConfiguration);

      // 4. Agregar stream local
      this.localStream.getTracks().forEach((track) => {
        this.peerConnection.addTrack(track, this.localStream);
      });

      // 5. Configurar event handlers
      this.setupPeerConnectionHandlers();

      // 6. Guardar estado
      this.currentCall = {
        fromUser,
        toUser,
        isInitiator: false,
      };

      // Notificar callbacks
      this.callbacks.onCallAccepted.forEach((cb) => {
        try {
          cb(fromUser);
        } catch (err) {
          console.error("[CallService] Error en callback:", err);
        }
      });
    } catch (error) {
      console.error("[CallService] Error aceptando llamada:", error);
      this.cleanup();
      throw error;
    }
  }

  /**
   * Rechaza una llamada entrante
   */
  async rejectCall(fromUser) {
    console.log(`[CallService] Rechazando llamada de: ${fromUser}`);
    
    try {
      const toUser = localStorage.getItem("chat_username");
      await voiceDelegate.callPrx.rejectCall(fromUser, toUser);
    } catch (error) {
      console.error("[CallService] Error rechazando llamada:", error);
    }
  }

  /**
   * Termina la llamada actual
   */
  async endCall() {
    if (!this.currentCall) {
      return;
    }

    console.log("[CallService] Terminando llamada");

    try {
      const { fromUser, toUser } = this.currentCall;
      await voiceDelegate.callPrx.endCall(fromUser, toUser);
    } catch (error) {
      console.error("[CallService] Error terminando llamada:", error);
    }

    this.cleanup();
  }

  /**
   * Maneja una oferta ICE recibida
   */
  async handleIceOffer(fromUser, offer) {
    console.log(`[CallService] Oferta recibida de: ${fromUser}`);

    if (!this.currentCall || this.currentCall.fromUser !== fromUser) {
      console.warn("[CallService] Oferta recibida pero no hay llamada activa");
      return;
    }

    try {
      // Si somos el receptor, ya tenemos el PeerConnection creado en acceptCall
      // Solo necesitamos establecer la descripción remota
      await this.peerConnection.setRemoteDescription(
        new RTCSessionDescription({ type: offer.type, sdp: offer.sdp })
      );

      // Crear respuesta
      const answer = await this.peerConnection.createAnswer();
      await this.peerConnection.setLocalDescription(answer);

      // Enviar respuesta
      const answerSdp = new Slice.Chat.SessionDescription("answer", answer.sdp);
      const toUser = localStorage.getItem("chat_username");
      await voiceDelegate.callPrx.sendIceAnswer(toUser, fromUser, answerSdp);

      console.log("[CallService] Respuesta enviada");
    } catch (error) {
      console.error("[CallService] Error procesando oferta:", error);
    }
  }

  /**
   * Maneja una respuesta ICE recibida
   */
  async handleIceAnswer(fromUser, answer) {
    console.log(`[CallService] Respuesta recibida de: ${fromUser}`);

    if (!this.currentCall || this.currentCall.toUser !== fromUser) {
      console.warn("[CallService] Respuesta recibida pero no hay llamada activa");
      return;
    }

    try {
      await this.peerConnection.setRemoteDescription(
        new RTCSessionDescription({ type: answer.type, sdp: answer.sdp })
      );
      console.log("[CallService] Descripción remota establecida");
    } catch (error) {
      console.error("[CallService] Error procesando respuesta:", error);
    }
  }

  /**
   * Maneja un candidato ICE recibido
   */
  async handleIceCandidate(fromUser, candidate) {
    if (!this.peerConnection) {
      return;
    }

    try {
      const iceCandidate = new RTCIceCandidate({
        candidate: candidate.candidate,
        sdpMid: candidate.sdpMid,
        sdpMLineIndex: candidate.sdpMLineIndex,
      });

      await this.peerConnection.addIceCandidate(iceCandidate);
      console.log("[CallService] Candidato ICE agregado");
    } catch (error) {
      console.error("[CallService] Error agregando candidato:", error);
    }
  }

  /**
   * Configura los event handlers del PeerConnection
   */
  setupPeerConnectionHandlers() {
    // Cuando se recibe un stream remoto
    this.peerConnection.ontrack = (event) => {
      console.log("[CallService] Stream remoto recibido");
      this.remoteStream = event.streams[0];
      
      // Notificar callbacks
      this.callbacks.onRemoteStream.forEach((cb) => {
        try {
          cb(this.remoteStream);
        } catch (err) {
          console.error("[CallService] Error en callback:", err);
        }
      });
    };

    // Cuando cambia el estado de la conexión ICE
    this.peerConnection.oniceconnectionstatechange = () => {
      const state = this.peerConnection.iceConnectionState;
      console.log(`[CallService] Estado ICE: ${state}`);

      this.callbacks.onCallStateChange.forEach((cb) => {
        try {
          cb(state);
        } catch (err) {
          console.error("[CallService] Error en callback:", err);
        }
      });

      if (state === "failed" || state === "disconnected" || state === "closed") {
        this.cleanup();
      }
    };

    // Cuando se genera un candidato ICE local
    this.peerConnection.onicecandidate = async (event) => {
      if (!event.candidate || !this.currentCall) {
        return;
      }

      try {
        const { fromUser, toUser } = this.currentCall;
        const candidate = new Slice.Chat.IceCandidate(
          event.candidate.candidate,
          event.candidate.sdpMid || "0",
          event.candidate.sdpMLineIndex || 0
        );

        await voiceDelegate.callPrx.sendIceCandidate(fromUser, toUser, candidate);
        console.log("[CallService] Candidato ICE enviado");
      } catch (error) {
        console.error("[CallService] Error enviando candidato:", error);
      }
    };
  }

  /**
   * Limpia recursos de la llamada
   */
  cleanup() {
    console.log("[CallService] Limpiando recursos");

    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => track.stop());
      this.localStream = null;
    }

    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }

    this.remoteStream = null;
    this.currentCall = null;

    // Notificar que la llamada terminó
    this.callbacks.onCallEnded.forEach((cb) => {
      try {
        cb();
      } catch (err) {
        console.error("[CallService] Error en callback:", err);
      }
    });
  }

  /**
   * Suscribirse a eventos
   */
  on(event, callback) {
    if (this.callbacks[event]) {
      this.callbacks[event].push(callback);
    }
  }

  /**
   * Desuscribirse de eventos
   */
  off(event, callback) {
    if (this.callbacks[event]) {
      this.callbacks[event] = this.callbacks[event].filter((cb) => cb !== callback);
    }
  }

  /**
   * Obtiene el stream remoto (para reproducir audio)
   */
  getRemoteStream() {
    return this.remoteStream;
  }

  /**
   * Verifica si hay una llamada activa
   */
  hasActiveCall() {
    return this.currentCall !== null;
  }
}

const instance = new CallService();
export default instance;
```

---

## 📝 Paso 4: Extender VoiceDelegate para Llamadas

**Modificar: `web-client/src/services/voiceDelegate.js`**

Agrega los handlers para eventos de llamadas:

```javascript
// ... código existente en voiceDelegate.js ...

// En el método donde se crea el servant, agregar:

servant.onVoice = (entry, current) => {
  console.log("[VoiceDelegate] Callback onVoice recibido:", entry);
  this.callbacks.forEach((cb) => {
    try {
      cb(entry);
    } catch (err) {
      console.error("[VoiceDelegate] Error en callback:", err);
    }
  });
};

// AGREGAR ESTOS NUEVOS HANDLERS:

servant.onCallIncoming = (fromUser, current) => {
  console.log("[VoiceDelegate] Llamada entrante de:", fromUser);
  // Esto se manejará en callService
  if (this.onCallIncomingCallback) {
    this.onCallIncomingCallback(fromUser);
  }
};

servant.onCallEnded = (fromUser, current) => {
  console.log("[VoiceDelegate] Llamada terminada con:", fromUser);
  if (this.onCallEndedCallback) {
    this.onCallEndedCallback(fromUser);
  }
};

servant.onIceOffer = (fromUser, offer, current) => {
  console.log("[VoiceDelegate] Oferta ICE recibida de:", fromUser);
  if (this.onIceOfferCallback) {
    this.onIceOfferCallback(fromUser, offer);
  }
};

servant.onIceAnswer = (fromUser, answer, current) => {
  console.log("[VoiceDelegate] Respuesta ICE recibida de:", fromUser);
  if (this.onIceAnswerCallback) {
    this.onIceAnswerCallback(fromUser, answer);
  }
};

servant.onIceCandidate = (fromUser, candidate, current) => {
  console.log("[VoiceDelegate] Candidato ICE recibido de:", fromUser);
  if (this.onIceCandidateCallback) {
    this.onIceCandidateCallback(fromUser, candidate);
  }
};

// Agregar métodos para registrar callbacks:

setOnCallIncoming(callback) {
  this.onCallIncomingCallback = callback;
}

setOnCallEnded(callback) {
  this.onCallEndedCallback = callback;
}

setOnIceOffer(callback) {
  this.onIceOfferCallback = callback;
}

setOnIceAnswer(callback) {
  this.onIceAnswerCallback = callback;
}

setOnIceCandidate(callback) {
  this.onIceCandidateCallback = callback;
}
```

---

## 📝 Paso 5: Integrar en la UI (Chat.js)

**Modificar: `web-client/src/pages/Chat.js`**

Agrega botones y lógica para llamadas:

```javascript
import callService from "../services/callService.js";
import voiceDelegate from "../services/voiceDelegate.js";

// ... código existente ...

// En la función Chat(), después de inicializar voiceDelegate:

// Configurar callbacks de llamadas
voiceDelegate.setOnCallIncoming((fromUser) => {
  callService.handleIncomingCall(fromUser);
});

voiceDelegate.setOnCallEnded((fromUser) => {
  callService.endCall();
  // Mostrar notificación
  alert(`Llamada con ${fromUser} terminada`);
});

voiceDelegate.setOnIceOffer((fromUser, offer) => {
  callService.handleIceOffer(fromUser, offer);
});

voiceDelegate.setOnIceAnswer((fromUser, answer) => {
  callService.handleIceAnswer(fromUser, answer);
});

voiceDelegate.setOnIceCandidate((fromUser, candidate) => {
  callService.handleIceCandidate(fromUser, candidate);
});

// Suscribirse a eventos de callService
callService.on("onIncomingCall", (fromUser) => {
  const accept = confirm(`Llamada entrante de ${fromUser}. ¿Aceptar?`);
  if (accept) {
    const username = localStorage.getItem("chat_username");
    callService.acceptCall(fromUser, username);
  } else {
    callService.rejectCall(fromUser);
  }
});

callService.on("onRemoteStream", (stream) => {
  // Reproducir audio remoto
  const audio = new Audio();
  audio.srcObject = stream;
  audio.play().catch((err) => {
    console.error("Error reproduciendo audio:", err);
  });
});

callService.on("onCallStateChange", (state) => {
  console.log("Estado de llamada:", state);
  if (state === "connected") {
    console.log("✓ Llamada conectada!");
  }
});

// Agregar botón de llamada en la UI
function addCallButton(chatElement, chat) {
  if (chat.type !== "user") {
    return; // Solo para chats privados
  }

  const callBtn = document.createElement("button");
  callBtn.textContent = "📞";
  callBtn.title = "Llamar";
  callBtn.style.marginLeft = "10px";

  callBtn.onclick = async () => {
    const username = localStorage.getItem("chat_username");
    
    if (callService.hasActiveCall()) {
      await callService.endCall();
      callBtn.textContent = "📞";
      callBtn.title = "Llamar";
    } else {
      try {
        await callService.startCall(username, chat.id);
        callBtn.textContent = "📞⏹";
        callBtn.title = "Colgar";
      } catch (error) {
        alert("Error iniciando llamada: " + error.message);
      }
    }
  };

  // Actualizar botón cuando cambia el estado
  callService.on("onCallEnded", () => {
    callBtn.textContent = "📞";
    callBtn.title = "Llamar";
  });

  chatElement.appendChild(callBtn);
}

// Llamar a addCallButton cuando se selecciona un chat
// (en la función donde se maneja la selección de chat)
```

---

## 📝 Paso 6: Reproducir Audio Remoto

Para reproducir el audio del otro usuario, agrega esto en Chat.js:

```javascript
let remoteAudioElement = null;

callService.on("onRemoteStream", (stream) => {
  // Crear elemento de audio si no existe
  if (!remoteAudioElement) {
    remoteAudioElement = document.createElement("audio");
    remoteAudioElement.autoplay = true;
    remoteAudioElement.style.display = "none";
    document.body.appendChild(remoteAudioElement);
  }

  // Asignar stream y reproducir
  remoteAudioElement.srcObject = stream;
  remoteAudioElement.play().catch((err) => {
    console.error("Error reproduciendo audio remoto:", err);
  });
});

callService.on("onCallEnded", () => {
  if (remoteAudioElement) {
    remoteAudioElement.srcObject = null;
  }
});
```

---

## 🧪 Paso 7: Probar

1. **Iniciar servidor Java**
2. **Iniciar web-client**: `npm start`
3. **Abrir dos navegadores** (o ventanas incógnito)
4. **Iniciar sesión** con dos usuarios diferentes
5. **Usuario A**: Hacer clic en el botón de llamada junto al nombre del Usuario B
6. **Usuario B**: Aceptar la llamada
7. **Verificar**: El audio debe fluir entre ambos

---

## 🔍 Troubleshooting

### "No se puede acceder al micrófono"
- Verificar permisos del navegador
- Usar HTTPS o localhost (requerido para getUserMedia)

### "Conexión ICE fallida"
- Verificar que el servidor STUN esté accesible
- Revisar firewall bloqueando UDP
- Considerar usar TURN server

### "Audio no se reproduce"
- Verificar que `remoteAudioElement` tenga `autoplay`
- Verificar permisos de autoplay del navegador
- Revisar consola para errores

### "Llamada no se conecta"
- Verificar logs del servidor
- Verificar que ambos clientes estén suscritos
- Revisar que las ofertas/respuestas se intercambien

---

## ✅ Checklist de Implementación

- [ ] Extender Services.ice con métodos ICE
- [ ] Regenerar código Slice
- [ ] Extender CallI.java con métodos ICE
- [ ] Crear callService.js
- [ ] Extender voiceDelegate.js con handlers ICE
- [ ] Integrar en Chat.js
- [ ] Agregar botón de llamada en UI
- [ ] Probar llamada entre dos clientes
- [ ] Verificar que audio fluya P2P

---

## 📚 Recursos

- WebRTC API: https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API
- RTCPeerConnection: https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection
- ICE: https://webrtc.org/getting-started/peer-connections-overview

---

**¡Listo! Ahora tienes llamadas en tiempo real con ICE desde el web-client.** 🎉

