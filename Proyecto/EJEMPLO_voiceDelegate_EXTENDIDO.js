// EJEMPLO: Cómo extender voiceDelegate.js para soportar llamadas
// Modificar: web-client/src/services/voiceDelegate.js
// Agregar las líneas marcadas con // ← NUEVO

import IceModule from "ice";
import * as Slice from "../ice/Services.js";

const Ice = IceModule.Ice || IceModule;

class VoiceDelegate {
  constructor() {
    this.communicator = null;
    this.callPrx = null;
    this.callbacks = [];
    this.subscriber = null;
    this.initPromise = null;
    this.currentUser = null;
    this._resolveInit = null;
    this._retryTimer = null;
    this._status = "idle";
    this._statusListeners = [];
    this.retryDelayMs = 3000;
    this._adapter = null;
    
    // ← NUEVO: Callbacks para eventos de llamadas
    this.onCallIncomingCallback = null;
    this.onCallEndedCallback = null;
    this.onIceOfferCallback = null;
    this.onIceAnswerCallback = null;
    this.onIceCandidateCallback = null;
  }

  // ... métodos existentes (init, subscribe, etc.) ...

  async _attemptInit() {
    if (!this.currentUser) {
      return;
    }

    if (this._retryTimer) {
      clearTimeout(this._retryTimer);
      this._retryTimer = null;
    }

    this._setStatus("connecting");

    try {
      this.communicator = Ice.initialize();

      const base = await this.communicator.stringToProxy(
        "Call:ws -h localhost -p 10010 -r /call"
      );

      const prx = await Slice.Chat.CallPrx.checkedCast(base);
      if (!prx) {
        throw new Error("No se pudo hacer cast a CallPrx");
      }

      this.callPrx = prx;

      const adapter = await this.communicator.createObjectAdapter("");
      this._adapter = adapter;
      
      // Crear el servant correctamente para Ice.js
      const servant = new Slice.Chat.VoiceObserver();
      
      // ← EXISTENTE: Método para notas de voz
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

      // ← NUEVO: Handler para llamadas entrantes
      servant.onCallIncoming = (fromUser, current) => {
        console.log("[VoiceDelegate] Llamada entrante de:", fromUser);
        if (this.onCallIncomingCallback) {
          this.onCallIncomingCallback(fromUser);
        }
      };

      // ← NUEVO: Handler para llamadas terminadas
      servant.onCallEnded = (fromUser, current) => {
        console.log("[VoiceDelegate] Llamada terminada con:", fromUser);
        if (this.onCallEndedCallback) {
          this.onCallEndedCallback(fromUser);
        }
      };

      // ← NUEVO: Handler para ofertas ICE
      servant.onIceOffer = (fromUser, offer, current) => {
        console.log("[VoiceDelegate] Oferta ICE recibida de:", fromUser);
        if (this.onIceOfferCallback) {
          this.onIceOfferCallback(fromUser, offer);
        }
      };

      // ← NUEVO: Handler para respuestas ICE
      servant.onIceAnswer = (fromUser, answer, current) => {
        console.log("[VoiceDelegate] Respuesta ICE recibida de:", fromUser);
        if (this.onIceAnswerCallback) {
          this.onIceAnswerCallback(fromUser, answer);
        }
      };

      // ← NUEVO: Handler para candidatos ICE
      servant.onIceCandidate = (fromUser, candidate, current) => {
        console.log("[VoiceDelegate] Candidato ICE recibido de:", fromUser);
        if (this.onIceCandidateCallback) {
          this.onIceCandidateCallback(fromUser, candidate);
        }
      };

      const ident = Ice.stringToIdentity("obs_" + this.currentUser);
      console.log("[VoiceDelegate] Registrando observer con identity:", ident.name);
      
      const addedPrx = adapter.add(servant, ident);
      await adapter.activate();
      console.log("[VoiceDelegate] Adapter activado");

      const connection = await this.callPrx.ice_getConnection();
      connection.setAdapter(adapter);
      console.log("[VoiceDelegate] Connection adapter configurado");

      const obsPrx = Slice.Chat.VoiceObserverPrx.uncheckedCast(addedPrx);
      console.log("[VoiceDelegate] Observer proxy creado");

      await this.callPrx.subscribe(this.currentUser, obsPrx);
      console.log("[VoiceDelegate] Subscripción completada para:", this.currentUser);

      this._setStatus("connected");

      if (this._resolveInit) {
        this._resolveInit();
        this._resolveInit = null;
      }
    } catch (err) {
      console.error("Error inicializando Ice CallPrx:", err);
      this._cleanup();
      this._setStatus("error", err);

      if (!this._retryTimer) {
        this._retryTimer = setTimeout(() => {
          this._retryTimer = null;
          this._attemptInit();
        }, this.retryDelayMs);
      }
    }
  }

  // ... métodos existentes (ensureReady, sendVoiceToUser, etc.) ...

  // ← NUEVO: Métodos para registrar callbacks de llamadas
  
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
}

const instance = new VoiceDelegate();
export default instance;

