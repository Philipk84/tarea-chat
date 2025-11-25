import voiceDelegate from "./voiceDelegate.js";
import * as Slice from "../ice/Services.js";

class CallService {
  constructor() {
    this.peerConnection = null;
    this.localStream = null;
    this.remoteStream = null;
    this.currentCall = null;
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

  async init(username) {
    console.log("[CallService] Inicializado para:", username);
  }

  async startCall(fromUser, toUser) {
    if (this.currentCall) {
      throw new Error("Ya hay una llamada activa");
    }

    console.log(`[CallService] Iniciando llamada: ${fromUser} → ${toUser}`);

    try {
      // Asegurar que voiceDelegate esté listo
      await voiceDelegate.ensureReady();
      
      if (!voiceDelegate.callPrx) {
        throw new Error("Servicio de llamadas no disponible");
      }

      await voiceDelegate.callPrx.initiateCall(fromUser, toUser);
      this.localStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false,
      });

      this.peerConnection = new RTCPeerConnection(this.rtcConfiguration);
      this.localStream.getTracks().forEach((track) => {
        this.peerConnection.addTrack(track, this.localStream);
      });

      this.setupPeerConnectionHandlers();

      const offer = await this.peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: false,
      });

      await this.peerConnection.setLocalDescription(offer);

      const offerSdp = new Slice.Chat.SessionDescription("offer", offer.sdp);
      await voiceDelegate.ensureReady();
      await voiceDelegate.callPrx.sendIceOffer(fromUser, toUser, offerSdp);

      this.currentCall = { fromUser, toUser, isInitiator: true };
      console.log("[CallService] Oferta enviada");
    } catch (error) {
      console.error("[CallService] Error iniciando llamada:", error);
      this.cleanup();
      throw error;
    }
  }

  async handleIncomingCall(fromUser) {
    console.log(`[CallService] Llamada entrante de: ${fromUser}`);

    if (this.currentCall) {
      await this.rejectCall(fromUser);
      return;
    }

    this.callbacks.onIncomingCall.forEach((cb) => {
      try {
        cb(fromUser);
      } catch (err) {
        console.error("[CallService] Error en callback:", err);
      }
    });
  }

  async acceptCall(fromUser, toUser) {
    console.log(`[CallService] Aceptando llamada de: ${fromUser}`);

    try {
      await voiceDelegate.ensureReady();
      await voiceDelegate.callPrx.acceptCall(toUser, fromUser);
      this.localStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false,
      });

      this.peerConnection = new RTCPeerConnection(this.rtcConfiguration);
      this.localStream.getTracks().forEach((track) => {
        this.peerConnection.addTrack(track, this.localStream);
      });

      this.setupPeerConnectionHandlers();

      this.currentCall = { fromUser, toUser, isInitiator: false };

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

  async rejectCall(fromUser) {
    console.log(`[CallService] Rechazando llamada de: ${fromUser}`);
    
    try {
      const toUser = localStorage.getItem("chat_username");
      await voiceDelegate.ensureReady();
      await voiceDelegate.callPrx.rejectCall(fromUser, toUser);
    } catch (error) {
      console.error("[CallService] Error rechazando llamada:", error);
    }
  }

  async endCall() {
    if (!this.currentCall) {
      return;
    }

    console.log("[CallService] Terminando llamada");

    try {
      const { fromUser, toUser } = this.currentCall;
      await voiceDelegate.ensureReady();
      await voiceDelegate.callPrx.endCall(fromUser, toUser);
    } catch (error) {
      console.error("[CallService] Error terminando llamada:", error);
    }

    this.cleanup();
  }

  async handleIceOffer(fromUser, offer) {
    console.log(`[CallService] Oferta recibida de: ${fromUser}`);

    if (!this.currentCall || this.currentCall.fromUser !== fromUser) {
      console.warn("[CallService] Oferta recibida pero no hay llamada activa");
      return;
    }

    try {
      await this.peerConnection.setRemoteDescription(
        new RTCSessionDescription({ type: offer.type, sdp: offer.sdp })
      );

      const answer = await this.peerConnection.createAnswer();
      await this.peerConnection.setLocalDescription(answer);

      const answerSdp = new Slice.Chat.SessionDescription("answer", answer.sdp);
      const toUser = localStorage.getItem("chat_username");
      await voiceDelegate.ensureReady();
      await voiceDelegate.callPrx.sendIceAnswer(toUser, fromUser, answerSdp);

      console.log("[CallService] Respuesta enviada");
    } catch (error) {
      console.error("[CallService] Error procesando oferta:", error);
    }
  }

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

  setupPeerConnectionHandlers() {
    this.peerConnection.ontrack = (event) => {
      console.log("[CallService] Stream remoto recibido");
      this.remoteStream = event.streams[0];
      
      this.callbacks.onRemoteStream.forEach((cb) => {
        try {
          cb(this.remoteStream);
        } catch (err) {
          console.error("[CallService] Error en callback:", err);
        }
      });
    };

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

    this.peerConnection.onicecandidate = async (event) => {
      if (!event.candidate || !this.currentCall) {
        return;
      }

      try {
        const { fromUser, toUser } = this.currentCall;
        
        // Crear Candidate con el candidato completo en formato SDP
        const candidate = new Slice.Chat.Candidate();
        candidate.candidate = event.candidate.candidate;
        candidate.sdpMid = event.candidate.sdpMid || "0";
        candidate.sdpMLineIndex = event.candidate.sdpMLineIndex || 0;

        await voiceDelegate.ensureReady();
        await voiceDelegate.callPrx.sendIceCandidate(fromUser, toUser, candidate);
        console.log("[CallService] Candidato ICE enviado");
      } catch (error) {
        console.error("[CallService] Error enviando candidato:", error);
      }
    };
  }

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

    this.callbacks.onCallEnded.forEach((cb) => {
      try {
        cb();
      } catch (err) {
        console.error("[CallService] Error en callback:", err);
      }
    });
  }

  on(event, callback) {
    if (this.callbacks[event]) {
      this.callbacks[event].push(callback);
    }
  }

  off(event, callback) {
    if (this.callbacks[event]) {
      this.callbacks[event] = this.callbacks[event].filter((cb) => cb !== callback);
    }
  }

  getRemoteStream() {
    return this.remoteStream;
  }

  hasActiveCall() {
    return this.currentCall !== null;
  }
}

const instance = new CallService();
export default instance;

