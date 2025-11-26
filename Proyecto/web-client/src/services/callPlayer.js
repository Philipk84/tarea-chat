let audioCtx = null;

function ensureCtx() {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)({
      sampleRate: 44100,
    });
  }
  return audioCtx;
}

// ByteSeq PCM16 → reproducir
export async function playPcm16(pcm16) {
  const ctx = ensureCtx();

  // pcm16 es Uint8Array, lo convertimos a Int16
  const int16 = new Int16Array(
    pcm16.buffer,
    pcm16.byteOffset,
    pcm16.byteLength / 2
  );

  const float32 = new Float32Array(int16.length);
  for (let i = 0; i < int16.length; i++) {
    float32[i] = int16[i] / 0x7fff;
  }

  const buffer = ctx.createBuffer(1, float32.length, 44100);
  buffer.copyToChannel(float32, 0);

  const src = ctx.createBufferSource();
  src.buffer = buffer;
  src.connect(ctx.destination);
  src.start();
}
