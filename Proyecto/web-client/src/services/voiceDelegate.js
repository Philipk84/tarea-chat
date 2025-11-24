import IceModule from "ice";
import * as Slice from "../ice/Services.js";

const Ice = IceModule.Ice || IceModule;

// Muy parecido a IceDelegatge del repo audio_rep
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
    this._status = "idle"; // idle | connecting | connected | error
    this._statusListeners = [];
    this.retryDelayMs = 3000;
    this._adapter = null;
  }

  async init(username) {
    this.currentUser = username;

    if (this.callPrx) {
      return this.initPromise ?? Promise.resolve();
    }

    if (!this.initPromise) {
      this.initPromise = new Promise((resolve) => {
        this._resolveInit = resolve;
      });
      this._attemptInit();
    }

    return this.initPromise;
  }

  subscribe(callback) {
    this.callbacks.push(callback);
  }

  onStatusChange(listener) {
    this._statusListeners.push(listener);
    listener(this._status);
    return () => {
      this._statusListeners = this._statusListeners.filter((fn) => fn !== listener);
    };
  }

  getStatus() {
    return this._status;
  }

  _setStatus(status, detail) {
    this._status = status;
    this._statusListeners.forEach((listener) => listener(status, detail));
  }

  _cleanup() {
    if (this._adapter) {
      try {
        const result = this._adapter.destroy?.();
        if (result && typeof result.then === "function") {
          result.catch(() => {});
        }
      } catch (_) {
        // ignore cleanup failures
      }
    }
    this._adapter = null;

    if (this.communicator) {
      try {
        this.communicator.destroy();
      } catch (_) {
        // no-op
      }
    }
    this.communicator = null;
    this.callPrx = null;
  }

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
      
      // Implementar el método onVoice del servant
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

  async ensureReady() {
    if (this.callPrx) {
      return;
    }

    if (this.initPromise) {
      await this.initPromise;
    }

    if (!this.callPrx) {
      throw new Error(
        "Servicio de notas de voz no disponible. Reintentando conexión con el servidor..."
      );
    }
  }

  async sendVoiceToUser(fromUser, toUser, bytes) {
    await this.ensureReady();
    const payload = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
    await this.callPrx.sendVoiceNoteToUser(fromUser, toUser, payload);
  }

  async sendVoiceToGroup(fromUser, groupName, bytes) {
    await this.ensureReady();
    const payload = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
    await this.callPrx.sendVoiceNoteToGroup(
      fromUser,
      groupName,
      payload
    );
  }
}

const instance = new VoiceDelegate();
export default instance;
