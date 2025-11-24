import Ice from "ice";
import * as Slice from "../ice/Services.js";

// Muy parecido a IceDelegatge del repo audio_rep
class VoiceDelegate {
  constructor() {
    this.communicator = null;
    this.callPrx = null;
    this.callbacks = [];
    this.subscriber = null;
  }

  async init(username) {
    if (this.callPrx) return;

    this.communicator = Ice.initialize();

    // Debe coincidir con tu adapter Java
    const base = await this.communicator.stringToProxy(
      "Call:ws -h localhost -p 10010 -r /call"
    );

    this.callPrx = await Slice.Chat.CallPrx.checkedCast(base);
    if (!this.callPrx) {
      throw new Error("No se pudo hacer cast a CallPrx");
    }

    // Observador local (equivalente a Subscriber en audio_rep)
    const adapter = this.communicator.createObjectAdapter("");
    const servant = new Slice.Chat.VoiceObserver();

    servant.onVoice = (entry, current) => {
      // avisar a todos los callbacks registrados (ej: appendHistoryItem)
      this.callbacks.forEach((cb) => cb(entry));
    };

    const ident = Ice.Util.stringToIdentity("obs_" + username);
    adapter.add(servant, ident);
    adapter.activate();

    const obsPrx = Slice.Chat.VoiceObserverPrx.uncheckedCast(
      adapter.createProxy(ident)
    );

    await this.callPrx.subscribe(username, obsPrx);
  }

  subscribe(callback) {
    this.callbacks.push(callback);
  }

  async sendVoiceToUser(fromUser, toUser, bytes) {
    await this.callPrx.sendVoiceNoteToUser(fromUser, toUser, Array.from(bytes));
  }

  async sendVoiceToGroup(fromUser, groupName, bytes) {
    await this.callPrx.sendVoiceNoteToGroup(
      fromUser,
      groupName,
      Array.from(bytes)
    );
  }
}

const instance = new VoiceDelegate();
export default instance;
