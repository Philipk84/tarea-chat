# Implementación ICE - Resumen Ejecutivo

## 🎯 Objetivo
Implementar llamadas con ICE (Interactive Connectivity Establishment) para permitir conexiones P2P a través de NATs y firewalls.

## 📝 Resumen de Cambios Necesarios

### 1. **Actualizar Services.ice** (5 minutos)
Agregar estructuras y métodos para intercambio de candidatos ICE.

### 2. **Agregar Dependencia** (2 minutos)
Agregar `ice4j` al `build.gradle`.

### 3. **Implementar IceManager** (30-45 minutos)
Clase que maneja la recopilación de candidatos y establecimiento de conexiones.

### 4. **Extender CallI en Servidor** (15 minutos)
Agregar métodos para reenviar ofertas/respuestas/candidatos ICE.

### 5. **Crear IceCallManager en Cliente** (30 minutos)
Integrar ICE con el sistema de llamadas existente.

### 6. **Actualizar VoiceObserver** (10 minutos)
Manejar eventos ICE en el observer.

---

## ⚡ Inicio Rápido

### Paso 1: Actualizar Services.ice

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
    
    // NUEVO: Estructuras para ICE
    struct IceCandidate {
        string candidate;
        string sdpMid;
        int sdpMLineIndex;
    };
    
    sequence<IceCandidate> IceCandidateSeq;
    
    struct SessionDescription {
        string type;  // "offer" o "answer"
        string sdp;
    };
    
    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        
        // NUEVO: Métodos para ICE
        void onIceOffer(string fromUser, SessionDescription offer);
        void onIceAnswer(string fromUser, SessionDescription answer);
        void onIceCandidate(string fromUser, IceCandidate candidate);
    };
    
    interface Call {
        void sendVoiceNoteToUser(string fromUser, string toUser, ByteSeq audio);
        void sendVoiceNoteToGroup(string fromUser, string groupName, ByteSeq audio);
        void subscribe(string username, VoiceObserver* obs);
        void unsubscribe(string username, VoiceObserver* obs);
        
        // NUEVO: Métodos para ICE
        void sendIceOffer(string fromUser, string toUser, SessionDescription offer);
        void sendIceAnswer(string fromUser, string toUser, SessionDescription answer);
        void sendIceCandidate(string fromUser, string toUser, IceCandidate candidate);
    };
}
```

### Paso 2: Actualizar build.gradle

```gradle
dependencies {
    implementation 'com.google.code.gson:gson:2.13.2'
    implementation 'com.zeroc:ice:3.7.4'
    implementation 'org.ice4j:ice4j:3.0-24'  // ← AGREGAR ESTA LÍNEA
}
```

### Paso 3: Regenerar Código

```bash
./gradlew build
```

---

## 🔄 Flujo de Llamada con ICE

```
Usuario A                          Servidor                          Usuario B
   │                                  │                                  │
   │  /call usuarioB                 │                                  │
   ├─────────────────────────────────►│                                  │
   │                                  │  notificar llamada              │
   │                                  ├─────────────────────────────────►│
   │                                  │                                  │
   │  gather candidates               │                                  │
   │  (STUN server)                   │                                  │
   │                                  │                                  │
   │  sendIceOffer(offer)             │                                  │
   ├─────────────────────────────────►│                                  │
   │                                  │  onIceOffer(offer)               │
   │                                  ├─────────────────────────────────►│
   │                                  │                                  │
   │                                  │  gather candidates               │
   │                                  │  (STUN server)                   │
   │                                  │                                  │
   │                                  │  sendIceAnswer(answer)           │
   │                                  │◄─────────────────────────────────┤
   │  onIceAnswer(answer)             │                                  │
   │◄─────────────────────────────────┤                                  │
   │                                  │                                  │
   │  sendIceCandidate(cand1)         │                                  │
   ├─────────────────────────────────►│                                  │
   │                                  │  onIceCandidate(cand1)           │
   │                                  ├─────────────────────────────────►│
   │                                  │                                  │
   │                                  │  sendIceCandidate(cand2)         │
   │                                  │◄─────────────────────────────────┤
   │  onIceCandidate(cand2)           │                                  │
   │◄─────────────────────────────────┤                                  │
   │                                  │                                  │
   │  ICE Connectivity Checks         │                                  │
   │  (prueba cada candidato)         │                                  │
   │                                  │                                  │
   │  ✓ Conexión establecida         │                                  │
   │  Audio comienza a fluir          │                                  │
```

---

## 📦 Archivos a Crear/Modificar

### Archivos Nuevos:
1. `client/src/main/java/service/IceManager.java`
2. `client/src/main/java/service/IceCallManager.java`
3. `client/src/main/java/rpc/VoiceObserverI.java`
4. `client/src/main/resources/ice-config.json`

### Archivos a Modificar:
1. `Services.ice` ✓
2. `build.gradle` ✓
3. `server/src/main/java/rpc/CallI.java`
4. `client/src/main/java/model/ChatClient.java`

---

## 🧪 Pruebas

### Prueba Básica:
1. Iniciar servidor
2. Conectar Cliente A
3. Conectar Cliente B
4. Cliente A ejecuta: `/call usuarioB`
5. Verificar logs de ICE
6. Verificar que audio fluya

### Verificar STUN:
```bash
# Instalar stunclient (Linux)
sudo apt-get install stun-client

# Probar
stunclient stun.l.google.com 19302
```

---

## ⚠️ Consideraciones Importantes

1. **STUN Server**: Usa un servidor STUN público para desarrollo, o instala Coturn para producción.

2. **Tiempo de Gathering**: La recopilación de candidatos puede tomar 2-5 segundos.

3. **Firewalls**: Algunos firewalls bloquean UDP. En esos casos necesitarás TURN.

4. **NAT Simétrico**: NATs simétricos requieren TURN server (no solo STUN).

5. **Cifrado**: Esta implementación básica no incluye cifrado. Para producción, agrega DTLS/SRTP.

---

## 📚 Recursos

- Guía completa: `GUIA_IMPLEMENTACION_ICE.md`
- RFC 8445: https://tools.ietf.org/html/rfc8445
- ice4j: https://github.com/jitsi/ice4j

---

## 🆘 Troubleshooting

**Error: "No se pueden recopilar candidatos"**
- Verifica conectividad a STUN server
- Revisa firewall bloqueando UDP

**Error: "Conexión ICE fallida"**
- Verifica que candidatos se intercambien
- Revisa logs de connectivity checks
- Considera usar TURN server

**Audio no fluye después de conexión ICE**
- Verifica que `getConnectedSocket()` retorne socket válido
- Revisa que AudioService use el socket ICE

