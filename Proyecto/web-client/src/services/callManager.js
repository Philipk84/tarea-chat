import voiceDelegate from "./voiceDelegate.js";

/**
 * CallManager - Gestiona llamadas de audio en tiempo real usando Ice ZeroC
 * Similar a voiceDelegate pero para comunicación bidireccional en llamadas
 */
class CallManager {
  constructor() {
    this.currentCall = null; // { callId, type: "private"|"group", participants: [], isIncoming: bool }
    this.audioContext = null;
    this.mediaStream = null;
    this.audioProcessor = null;
    this.remoteAudioBuffers = new Map(); // username -> AudioBuffer[]
    this.onCallEventListeners = [];
    this.onIncomingCallListeners = [];
    this.remoteAudioSources = new Map(); // username -> { context, source, gainNode }
  }

  /**
   * Inicializar el CallManager y suscribirse a eventos de llamadas
   */
  init(username) {
    this.username = username;

    // Suscribirse a los eventos de llamada via voiceDelegate
    voiceDelegate.subscribe((data) => {
      // Distinguir entre CallEvent, CallChunk y VoiceEntry
      if (data.type && data.callId && data.caller !== undefined) {
        // Es un CallEvent
        this._handleCallEvent(data);
      } else if (data.callId && data.fromUser && data.audio) {
        // Es un CallChunk
        this._handleCallChunk(data);
      }
      // VoiceEntry se maneja en Chat.js, no aquí
    });

    console.log("[CallManager] Inicializado para usuario:", username);
  }

  /**
   * Manejar chunks de audio recibidos durante la llamada
   */
  _handleCallChunk(chunk) {
    if (!this.currentCall || this.currentCall.callId !== chunk.callId) {
      // No es una llamada en curso
      return;
    }
    // Reproducir el audio del otro participante
    this._playCallChunk(chunk.fromUser, chunk.audio);
  }

  /**
   * Registrar listener para eventos de llamada
   */
  onCallEvent(listener) {
    this.onCallEventListeners.push(listener);
    return () => {
      this.onCallEventListeners = this.onCallEventListeners.filter((l) => l !== listener);
    };
  }

  /**
   * Registrar listener para llamadas entrantes
   */
  onIncomingCall(listener) {
    this.onIncomingCallListeners.push(listener);
    return () => {
      this.onIncomingCallListeners = this.onIncomingCallListeners.filter((l) => l !== listener);
    };
  }

  /**
   * Iniciar una llamada privada
   */
  async startPrivateCall(callee) {
    try {
      await voiceDelegate.ensureReady();
      
      console.log("[CallManager] Iniciando llamada privada a:", callee);
      const callId = await voiceDelegate.startCall(this.username, callee);
      
      this.currentCall = {
        callId,
        type: "private",
        participants: [this.username, callee],
        isIncoming: false,
        other: callee,
      };

      console.log("[CallManager] Llamada iniciada:", callId);
      return callId;
    } catch (err) {
      console.error("[CallManager] Error iniciando llamada:", err);
      throw err;
    }
  }

  /**
   * Iniciar una llamada grupal
   */
  async startGroupCall(groupName) {
    try {
      await voiceDelegate.ensureReady();
      
      console.log("[CallManager] Iniciando llamada grupal:", groupName);
      const callId = await voiceDelegate.startGroupCall(this.username, groupName);
      
      this.currentCall = {
        callId,
        type: "group",
        group: groupName,
        participants: [], // Se llenará con los eventos
        isIncoming: false,
      };

      console.log("[CallManager] Llamada grupal iniciada:", callId);
      return callId;
    } catch (err) {
      console.error("[CallManager] Error iniciando llamada grupal:", err);
      throw err;
    }
  }

  /**
   * Aceptar una llamada entrante
   */
  async acceptCall(callId) {
    try {
      await voiceDelegate.ensureReady();
      
      console.log("[CallManager] Aceptando llamada:", callId);
      await voiceDelegate.acceptCall(callId, this.username);
      
      // Iniciar captura de audio
      await this._startAudioCapture(callId);
      
      console.log("[CallManager] Llamada aceptada:", callId);
    } catch (err) {
      console.error("[CallManager] Error aceptando llamada:", err);
      throw err;
    }
  }

  /**
   * Rechazar una llamada entrante
   */
  async rejectCall(callId) {
    try {
      await voiceDelegate.ensureReady();
      
      console.log("[CallManager] Rechazando llamada:", callId);
      await voiceDelegate.rejectCall(callId, this.username);
      
      this.currentCall = null;
      console.log("[CallManager] Llamada rechazada:", callId);
    } catch (err) {
      console.error("[CallManager] Error rechazando llamada:", err);
      throw err;
    }
  }

  /**
   * Terminar la llamada actual
   */
  async endCall() {
    if (!this.currentCall) {
      console.warn("[CallManager] No hay llamada activa para terminar");
      return;
    }

    try {
      await voiceDelegate.ensureReady();
      
      const callId = this.currentCall.callId;
      console.log("[CallManager] Terminando llamada:", callId);
      console.log("[CallManager] endCall - stack:", new Error().stack);
      
      await voiceDelegate.endCall(callId, this.username);
      
      // Detener captura de audio
      this._stopAudioCapture();
      
      this.currentCall = null;
      console.log("[CallManager] Llamada terminada:", callId);
    } catch (err) {
      console.error("[CallManager] Error terminando llamada:", err);
      throw err;
    }
  }

  /**
   * Iniciar captura de audio del micrófono y envío de chunks
   */
  async _startAudioCapture(callId) {
    try {
      // Crear contexto de audio
      this.audioContext = new AudioContext({ sampleRate: 44100 });
      
      // Solicitar acceso al micrófono
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      
      const source = this.audioContext.createMediaStreamSource(this.mediaStream);
      this.audioProcessor = this.audioContext.createScriptProcessor(2048, 1, 1);
      
      this.audioProcessor.onaudioprocess = (e) => {
        const input = e.inputBuffer.getChannelData(0);
        const pcm16 = this._float32ToPCM16(input);
        
        // Enviar chunk via Ice
        this._sendCallChunk(callId, pcm16);
      };
      
      source.connect(this.audioProcessor);
      this.audioProcessor.connect(this.audioContext.destination);
      
      console.log("[CallManager] Captura de audio iniciada");
    } catch (err) {
      console.error("[CallManager] Error iniciando captura de audio:", err);
      throw err;
    }
  }

  /**
   * Detener captura de audio
   */
  _stopAudioCapture() {
    if (this.audioProcessor) {
      this.audioProcessor.disconnect();
      this.audioProcessor = null;
    }
    
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop());
      this.mediaStream = null;
    }
    
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
    
    // Limpiar audio remoto
    this.remoteAudioSources.forEach(({ context, source, gainNode }) => {
      if (source) source.disconnect();
      if (gainNode) gainNode.disconnect();
      if (context) context.close();
    });
    this.remoteAudioSources.clear();
    this.remoteAudioBuffers.clear();
    
    console.log("[CallManager] Captura de audio detenida");
  }

  /**
   * Enviar chunk de audio a través de Ice
   */
  async _sendCallChunk(callId, pcm16Data) {
    try {
      await voiceDelegate.sendCallChunk(callId, this.username, pcm16Data);
    } catch (err) {
      console.error("[CallManager] Error enviando chunk:", err);
    }
  }

  /**
   * Convertir Float32 a PCM16 Little Endian
   */
  _float32ToPCM16(float32) {
    const buffer = new ArrayBuffer(float32.length * 2);
    const view = new DataView(buffer);
    for (let i = 0; i < float32.length; i++) {
      let s = Math.max(-1, Math.min(1, float32[i]));
      view.setInt16(i * 2, s * 0x7fff, true); // little-endian
    }
    return new Uint8Array(buffer);
  }

  /**
   * Reproducir chunk de audio recibido
   */
  _playCallChunk(fromUser, audioData) {
    try {
      // Crear contexto de audio para este usuario si no existe
      if (!this.remoteAudioSources.has(fromUser)) {
        const context = new AudioContext({ sampleRate: 44100 });
        const gainNode = context.createGain();
        gainNode.connect(context.destination);
        
        this.remoteAudioSources.set(fromUser, { context, gainNode, source: null });
      }
      
      const { context, gainNode } = this.remoteAudioSources.get(fromUser);
      
      // Convertir PCM16 a Float32
      const float32 = this._pcm16ToFloat32(audioData);
      
      // Crear buffer de audio
      const audioBuffer = context.createBuffer(1, float32.length, 44100);
      audioBuffer.getChannelData(0).set(float32);
      
      // Crear source y reproducir
      const source = context.createBufferSource();
      source.buffer = audioBuffer;
      source.connect(gainNode);
      source.start(0);
      
    } catch (err) {
      console.error("[CallManager] Error reproduciendo chunk de", fromUser, ":", err);
    }
  }

  /**
   * Convertir PCM16 a Float32
   */
  _pcm16ToFloat32(uint8Array) {
    const view = new DataView(uint8Array.buffer, uint8Array.byteOffset, uint8Array.byteLength);
    const float32 = new Float32Array(uint8Array.length / 2);
    
    for (let i = 0; i < float32.length; i++) {
      const int16 = view.getInt16(i * 2, true); // little-endian
      float32[i] = int16 / 0x7fff;
    }
    
    return float32;
  }

  /**
   * Manejar eventos de llamada recibidos via Ice
   */
  _handleCallEvent(event) {
    console.log("[CallManager] Evento recibido:", event);
    
    const { type, callId, caller, callee, group, scope } = event;
    
    switch (type) {
      case "call_incoming":
        // Llamada entrante
        this.currentCall = {
          callId,
          type: scope,
          caller,
          callee: scope === "private" ? callee : undefined,
          group: scope === "group" ? group : undefined,
          participants: scope === "private" ? [caller, callee] : [],
          isIncoming: true,
        };
        
        this.onIncomingCallListeners.forEach((listener) => listener(this.currentCall));
        break;
        
      case "call_started":
        // Confirmar inicio de llamada
        if (this.currentCall && this.currentCall.callId === callId) {
          this._startAudioCapture(callId);
        }
        break;
        
      case "call_accepted":
        // Otro participante aceptó - iniciar captura si es nuestra llamada
        console.log("[CallManager] Llamada aceptada por:", caller);
        if (this.currentCall && this.currentCall.callId === callId) {
          // Si aún no estamos capturando, iniciar
          if (!this.audioProcessor && !this.audioContext) {
            this._startAudioCapture(callId);
          }
        }
        break;
        
      case "call_rejected":
        // Llamada rechazada
        this.currentCall = null;
        this._stopAudioCapture();
        break;
        
      case "call_ended":
        // Llamada terminada
        this.currentCall = null;
        this._stopAudioCapture();
        break;
    }
    
    // Notificar a listeners
    this.onCallEventListeners.forEach((listener) => listener(event));
  }

  /**
   * Obtener la llamada actual
   */
  getCurrentCall() {
    return this.currentCall;
  }
}

const instance = new CallManager();
export default instance;
