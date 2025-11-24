import voiceDelegate from "./voiceDelegate.js";

// Convierte Float32 PCM [-1,1] a PCM16 LE como en audio_rep
function float32ToPCM16(float32) {
  const buffer = new ArrayBuffer(float32.length * 2);
  const view = new DataView(buffer);
  for (let i = 0; i < float32.length; i++) {
    let s = Math.max(-1, Math.min(1, float32[i]));
    view.setInt16(i * 2, s * 0x7fff, true); // little-endian
  }
  return new Uint8Array(buffer);
}

// Creador de grabador (nota de voz completa)
export function createRecorder(username, target) {
  // target = { type: "user"|"group", id: "nombre" }

  let audioCtx = null;
  let stream = null;
  let processor = null;
  let chunks = [];

  return {
    async start() {
      audioCtx = new AudioContext({ sampleRate: 44100 });
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });

      const source = audioCtx.createMediaStreamSource(stream);
      processor = audioCtx.createScriptProcessor(2048, 1, 1);

      processor.onaudioprocess = (e) => {
        const input = e.inputBuffer.getChannelData(0);
        const pcm16 = float32ToPCM16(input);
        chunks.push(pcm16);
      };

      source.connect(processor);
      processor.connect(audioCtx.destination);
    },

    async stop() {
      if (processor) processor.disconnect();
      if (stream) stream.getTracks().forEach((t) => t.stop());
      if (audioCtx) await audioCtx.close();

      // Concatenar chunks en un solo Uint8Array
      let totalLength = chunks.reduce((acc, c) => acc + c.length, 0);
      const all = new Uint8Array(totalLength);
      let offset = 0;
      for (const c of chunks) {
        all.set(c, offset);
        offset += c.length;
      }
      chunks = [];

      if (target.type === "user") {
        await voiceDelegate.sendVoiceToUser(username, target.id, all);
      } else {
        await voiceDelegate.sendVoiceToGroup(username, target.id, all);
      }
    },
  };
}
