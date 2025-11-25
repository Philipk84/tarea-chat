# Guía Paso a Paso: Implementación de Llamadas con ICE

## 📋 Índice
1. [Introducción](#introducción)
2. [Prerrequisitos](#prerrequisitos)
3. [Arquitectura General](#arquitectura-general)
4. [Paso 1: Configurar Servidor STUN/TURN](#paso-1-configurar-servidor-stunturn)
5. [Paso 2: Extender Services.ice para ICE](#paso-2-extender-servicesice-para-ice)
6. [Paso 3: Implementar Cliente ICE en Java](#paso-3-implementar-cliente-ice-en-java)
7. [Paso 4: Modificar Servidor para Manejar Señalización ICE](#paso-4-modificar-servidor-para-manejar-señalización-ice)
8. [Paso 5: Actualizar Cliente para Usar ICE](#paso-5-actualizar-cliente-para-usar-ice)
9. [Paso 6: Integrar con CallManager](#paso-6-integrar-con-callmanager)
10. [Paso 7: Pruebas y Depuración](#paso-7-pruebas-y-depuración)

---

## Introducción

**ICE (Interactive Connectivity Establishment)** es un protocolo que permite establecer conexiones P2P entre dos clientes que pueden estar detrás de NATs y firewalls. Es parte del estándar WebRTC y es esencial para llamadas en tiempo real.

### ¿Por qué ICE?
- **NAT Traversal**: Permite conectar clientes detrás de NATs
- **Firewall Friendly**: Funciona a través de firewalls corporativos
- **Mejor Conectividad**: Encuentra la mejor ruta de conexión disponible
- **Robustez**: Maneja múltiples candidatos de conexión

### Estado Actual del Proyecto
Actualmente tu proyecto usa:
- **UDP directo** para audio (requiere IPs públicas o configuración manual)
- **ZeroC Ice** para señalización RPC
- Intercambio simple de direcciones IP:puerto

### Objetivo
Implementar ICE para que las llamadas funcionen automáticamente sin necesidad de IPs públicas o configuración manual de NAT.

---

## Prerrequisitos

### 1. Dependencias Java
Necesitarás agregar una librería para ICE. Opciones:

**Opción A: ice4j (Recomendada)**
```gradle
implementation 'org.ice4j:ice4j:3.0-24'
```

**Opción B: Implementación manual** (más complejo pero más control)

### 2. Servidor STUN/TURN
Necesitas un servidor STUN/TURN. Opciones:

**Opción A: Servidor público (para pruebas)**
- Google STUN: `stun:stun.l.google.com:19302`
- Twilio STUN: `stun:global.stun.twilio.com:3478`

**Opción B: Coturn (Recomendado para producción)**
- Servidor TURN/STUN open source
- Instalación: `sudo apt-get install coturn`

### 3. Conocimientos Necesarios
- Conceptos básicos de NAT, STUN, TURN
- Protocolo ICE (candidatos, ofertas/respuestas)
- WebRTC básico (opcional pero útil)

---

## Arquitectura General

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Cliente   │◄──ICE──►│   Servidor   │◄──ICE──►│   Cliente   │
│     A       │         │  Señalización│         │     B       │
└─────────────┘         └──────────────┘         └─────────────┘
      │                        │                        │
      │                        │                        │
      └───────────STUN/TURN────┴────────STUN/TURN───────┘
                    │                        │
                    └───────────P2P──────────┘
```

### Flujo de ICE:
1. **Gathering**: Cliente recopila candidatos (host, srflx, relay)
2. **Signaling**: Intercambio de candidatos vía servidor (ZeroC Ice)
3. **Connectivity Checks**: Clientes prueban cada candidato
4. **Connection**: Se establece la mejor conexión disponible

---

## Paso 1: Configurar Servidor STUN/TURN

### Opción A: Usar Servidor STUN Público (Desarrollo)

Crea un archivo de configuración para los servidores STUN:

**Archivo: `client/src/main/resources/ice-config.json`**
```json
{
  "stunServers": [
    {
      "uri": "stun:stun.l.google.com:19302"
    }
  ],
  "turnServers": []
}
```

### Opción B: Instalar Coturn (Producción)

1. **Instalar Coturn:**
```bash
sudo apt-get update
sudo apt-get install coturn
```

2. **Configurar `/etc/turnserver.conf`:**
```
listening-port=3478
realm=tu-dominio.com
user=usuario:contraseña
```

3. **Iniciar Coturn:**
```bash
sudo systemctl start coturn
```

---

## Paso 2: Extender Services.ice para ICE

Necesitamos agregar métodos para intercambiar candidatos ICE y ofertas/respuestas SDP.

**Modificar: `Services.ice`**

```slice
module Chat {
    
    // ... código existente ...
    
    // Estructura para candidatos ICE
    struct IceCandidate {
        string candidate;      // Ej: "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host"
        string sdpMid;          // Media stream ID
        int sdpMLineIndex;      // Media line index
    };
    
    sequence<IceCandidate> IceCandidateSeq;
    
    // Estructura para ofertas/respuestas SDP
    struct SessionDescription {
        string type;            // "offer" o "answer"
        string sdp;             // SDP completo
    };
    
    // Extender interfaz Call para ICE
    interface Call {
        
        // ... métodos existentes ...
        
        // Nuevos métodos para ICE
        void sendIceOffer(string fromUser, string toUser, SessionDescription offer);
        void sendIceAnswer(string fromUser, string toUser, SessionDescription answer);
        void sendIceCandidate(string fromUser, string toUser, IceCandidate candidate);
        
        // Para llamadas grupales
        void sendIceOfferToGroup(string fromUser, string groupName, SessionDescription offer);
        void sendIceCandidateToGroup(string fromUser, string groupName, IceCandidate candidate);
    };
    
    // Extender VoiceObserver para recibir eventos ICE
    interface VoiceObserver {
        void onVoice(VoiceEntry entry);
        
        // Nuevos métodos para ICE
        void onIceOffer(string fromUser, SessionDescription offer);
        void onIceAnswer(string fromUser, SessionDescription answer);
        void onIceCandidate(string fromUser, IceCandidate candidate);
    };
}
```

**Después de modificar, regenera los archivos Java:**
```bash
./gradlew build
```

---

## Paso 3: Implementar Cliente ICE en Java

### 3.1 Agregar Dependencia

**Modificar: `build.gradle`**

```gradle
dependencies {
    implementation 'com.google.code.gson:gson:2.13.2'
    implementation 'com.zeroc:ice:3.7.4'
    implementation 'org.ice4j:ice4j:3.0-24'  // ← Agregar esta línea
}
```

### 3.2 Crear Clase ICE Manager

**Crear: `client/src/main/java/service/IceManager.java`**

```java
package service;

import org.ice4j.ice.*;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.socket.IceSocketWrapper;
import org.ice4j.StackProperties;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Gestor de ICE para establecer conexiones P2P.
 * Maneja la recopilación de candidatos, intercambio de señales y establecimiento de conexiones.
 */
public class IceManager {
    
    private Agent iceAgent;
    private MediaStream mediaStream;
    private Component component;
    private final String localUser;
    private IceConnectionState connectionState = IceConnectionState.NEW;
    
    // Callbacks
    private Runnable onConnectedCallback;
    private Runnable onDisconnectedCallback;
    
    public IceManager(String localUser) {
        this.localUser = localUser;
        initializeIce();
    }
    
    /**
     * Inicializa el agente ICE con servidores STUN/TURN.
     */
    private void initializeIce() {
        try {
            // Configurar propiedades de ICE
            System.setProperty(StackProperties.ALWAYS_SEND_RELAYED, "false");
            System.setProperty(StackProperties.HARVEST_STUN_SERVERS, "true");
            
            // Crear agente ICE
            iceAgent = new Agent();
            
            // Agregar harvester STUN
            String stunServer = "stun:stun.l.google.com:19302";
            StunCandidateHarvester stunHarvester = new StunCandidateHarvester(
                new TransportAddress(stunServer, 19302, Transport.UDP)
            );
            iceAgent.addCandidateHarvester(stunHarvester);
            
            // Crear media stream
            mediaStream = iceAgent.createMediaStream("audio");
            
            // Crear componente RTP
            component = iceAgent.createComponent(mediaStream, Transport.UDP);
            
            System.out.println("[ICE] Agente ICE inicializado para: " + localUser);
            
        } catch (Exception e) {
            System.err.println("[ICE] Error inicializando ICE: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicia la recopilación de candidatos ICE.
     */
    public CompletableFuture<String> gatherCandidates() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Iniciar gathering
            iceAgent.setControlling(true);
            
            // Agregar listener para cuando termine el gathering
            iceAgent.addStateChangeListener(new AgentStateChangeListener() {
                @Override
                public void stateChanged(AgentStateChangeEvent event) {
                    if (event.getNewState() == AgentState.GATHERING) {
                        System.out.println("[ICE] Recopilando candidatos...");
                    } else if (event.getNewState() == AgentState.GATHERED) {
                        System.out.println("[ICE] Candidatos recopilados");
                        String localSdp = generateLocalSdp();
                        future.complete(localSdp);
                    }
                }
            });
            
            iceAgent.start();
            
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Genera el SDP local con los candidatos recopilados.
     */
    private String generateLocalSdp() {
        StringBuilder sdp = new StringBuilder();
        sdp.append("v=0\r\n");
        sdp.append("o=- ").append(System.currentTimeMillis()).append(" 1 IN IP4 127.0.0.1\r\n");
        sdp.append("s=-\r\n");
        sdp.append("t=0 0\r\n");
        sdp.append("m=audio 9 UDP/TLS/RTP/SAVPF 0\r\n");
        sdp.append("c=IN IP4 0.0.0.0\r\n");
        
        // Agregar candidatos
        List<LocalCandidate> candidates = component.getLocalCandidates();
        for (LocalCandidate candidate : candidates) {
            sdp.append("a=candidate:").append(candidate.toSdp()).append("\r\n");
        }
        
        return sdp.toString();
    }
    
    /**
     * Procesa una oferta SDP remota y genera una respuesta.
     */
    public CompletableFuture<String> processOffer(String remoteSdp) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Parsear candidatos remotos del SDP
            List<RemoteCandidate> remoteCandidates = parseRemoteCandidates(remoteSdp);
            
            // Agregar candidatos remotos
            for (RemoteCandidate candidate : remoteCandidates) {
                component.addRemoteCandidate(candidate);
            }
            
            // Iniciar checks de conectividad
            iceAgent.setControlling(false); // Somos el que responde
            iceAgent.start();
            
            // Agregar listener para cambios de estado
            iceAgent.addStateChangeListener(new AgentStateChangeListener() {
                @Override
                public void stateChanged(AgentStateChangeEvent event) {
                    if (event.getNewState() == AgentState.COMPLETED) {
                        connectionState = IceConnectionState.COMPLETED;
                        System.out.println("[ICE] Conexión establecida!");
                        if (onConnectedCallback != null) {
                            onConnectedCallback.run();
                        }
                        String answerSdp = generateLocalSdp();
                        future.complete(answerSdp);
                    }
                }
            });
            
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Procesa una respuesta SDP remota.
     */
    public void processAnswer(String remoteSdp) {
        try {
            List<RemoteCandidate> remoteCandidates = parseRemoteCandidates(remoteSdp);
            
            for (RemoteCandidate candidate : remoteCandidates) {
                component.addRemoteCandidate(candidate);
            }
            
            // Continuar con checks de conectividad
            iceAgent.start();
            
        } catch (Exception e) {
            System.err.println("[ICE] Error procesando respuesta: " + e.getMessage());
        }
    }
    
    /**
     * Agrega un candidato ICE remoto.
     */
    public void addRemoteCandidate(String candidateSdp) {
        try {
            RemoteCandidate candidate = parseCandidate(candidateSdp);
            component.addRemoteCandidate(candidate);
        } catch (Exception e) {
            System.err.println("[ICE] Error agregando candidato: " + e.getMessage());
        }
    }
    
    /**
     * Parsea candidatos remotos del SDP.
     */
    private List<RemoteCandidate> parseRemoteCandidates(String sdp) {
        List<RemoteCandidate> candidates = new ArrayList<>();
        String[] lines = sdp.split("\r\n");
        
        for (String line : lines) {
            if (line.startsWith("a=candidate:")) {
                String candidateStr = line.substring(12);
                RemoteCandidate candidate = parseCandidate(candidateStr);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Parsea un candidato individual.
     */
    private RemoteCandidate parseCandidate(String candidateStr) {
        // Formato: "1 1 UDP 2130706431 192.168.1.100 54321 typ host"
        String[] parts = candidateStr.split(" ");
        if (parts.length < 8) return null;
        
        try {
            String foundation = parts[0];
            int componentId = Integer.parseInt(parts[1]);
            String transport = parts[2];
            long priority = Long.parseLong(parts[3]);
            String ip = parts[4];
            int port = Integer.parseInt(parts[5]);
            String type = parts[7];
            
            TransportAddress address = new TransportAddress(ip, port, Transport.UDP);
            return new RemoteCandidate(address, component, type, foundation, priority);
            
        } catch (Exception e) {
            System.err.println("[ICE] Error parseando candidato: " + candidateStr);
            return null;
        }
    }
    
    /**
     * Obtiene el socket UDP establecido después de la conexión ICE.
     */
    public DatagramSocket getConnectedSocket() {
        if (connectionState != IceConnectionState.COMPLETED) {
            return null;
        }
        
        try {
            IceSocketWrapper socketWrapper = component.getSocket();
            if (socketWrapper != null) {
                return socketWrapper.getUDPSocket();
            }
        } catch (Exception e) {
            System.err.println("[ICE] Error obteniendo socket: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtiene los candidatos locales recopilados.
     */
    public List<String> getLocalCandidates() {
        List<String> candidates = new ArrayList<>();
        List<LocalCandidate> localCandidates = component.getLocalCandidates();
        
        for (LocalCandidate candidate : localCandidates) {
            candidates.add(candidate.toSdp());
        }
        
        return candidates;
    }
    
    /**
     * Cierra la conexión ICE y libera recursos.
     */
    public void close() {
        if (iceAgent != null) {
            iceAgent.free();
        }
        connectionState = IceConnectionState.CLOSED;
    }
    
    // Setters para callbacks
    public void setOnConnectedCallback(Runnable callback) {
        this.onConnectedCallback = callback;
    }
    
    public void setOnDisconnectedCallback(Runnable callback) {
        this.onDisconnectedCallback = callback;
    }
    
    public IceConnectionState getConnectionState() {
        return connectionState;
    }
    
    enum IceConnectionState {
        NEW, GATHERING, GATHERED, CHECKING, COMPLETED, FAILED, CLOSED
    }
}
```

---

## Paso 4: Modificar Servidor para Manejar Señalización ICE

### 4.1 Extender CallI

**Modificar: `server/src/main/java/rpc/CallI.java`**

Agregar los nuevos métodos para manejar ofertas, respuestas y candidatos ICE:

```java
// ... código existente ...

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
    } else {
        System.err.println("[ICE] Observer no encontrado para: " + toUser);
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

@Override
public void sendIceOfferToGroup(String fromUser, String groupName, SessionDescription offer, Current current) {
    // Implementar para grupos
    System.out.println("[ICE] Oferta grupal de " + fromUser + " para grupo " + groupName);
}

@Override
public void sendIceCandidateToGroup(String fromUser, String groupName, IceCandidate candidate, Current current) {
    // Implementar para grupos
    System.out.println("[ICE] Candidato grupal de " + fromUser + " para grupo " + groupName);
}
```

---

## Paso 5: Actualizar Cliente para Usar ICE

### 5.1 Crear IceCallManager

**Crear: `client/src/main/java/service/IceCallManager.java`**

```java
package service;

import Chat.*;
import com.zeroc.Ice.Current;
import interfaces.CallManager;
import interfaces.AudioService;
import model.CallAudio;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestor de llamadas que usa ICE para establecer conexiones P2P.
 */
public class IceCallManager implements CallManager {
    
    private IceManager iceManager;
    private CallPrx callPrx;
    private VoiceObserverPrx voiceObserverPrx;
    private AudioService audioService;
    private String activeCallId;
    private String remoteUser;
    private boolean isInitiator;
    
    public IceCallManager(String localUser, CallPrx callPrx, VoiceObserverPrx voiceObserverPrx) {
        this.callPrx = callPrx;
        this.voiceObserverPrx = voiceObserverPrx;
        this.iceManager = new IceManager(localUser);
        
        // Configurar callbacks ICE
        iceManager.setOnConnectedCallback(() -> {
            System.out.println("[ICE] Conexión establecida, iniciando audio...");
            startAudioAfterIce();
        });
    }
    
    /**
     * Inicia una llamada usando ICE.
     */
    @Override
    public void startCall(String callId, List<InetSocketAddress> peers) {
        // Para ICE, no necesitamos las direcciones directas
        // El proceso es: gather -> offer -> answer -> connect
        this.activeCallId = callId;
        this.isInitiator = true;
        
        // Iniciar recopilación de candidatos
        iceManager.gatherCandidates().thenAccept(localSdp -> {
            // Crear oferta SDP
            SessionDescription offer = new SessionDescription("offer", localSdp);
            
            // Enviar oferta al otro usuario (necesitas obtener el nombre del usuario remoto)
            // Esto debería venir como parámetro o del servidor
            try {
                callPrx.sendIceOffer(/* fromUser */, remoteUser, offer);
            } catch (Exception e) {
                System.err.println("[ICE] Error enviando oferta: " + e.getMessage());
            }
        });
    }
    
    /**
     * Maneja una oferta ICE recibida.
     */
    public void handleIceOffer(String fromUser, SessionDescription offer) {
        this.remoteUser = fromUser;
        this.isInitiator = false;
        
        // Recopilar nuestros candidatos
        iceManager.gatherCandidates().thenAccept(localSdp -> {
            // Procesar oferta remota
            iceManager.processOffer(offer.sdp).thenAccept(answerSdp -> {
                // Enviar respuesta
                SessionDescription answer = new SessionDescription("answer", answerSdp);
                try {
                    callPrx.sendIceAnswer(/* localUser */, fromUser, answer);
                } catch (Exception e) {
                    System.err.println("[ICE] Error enviando respuesta: " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * Maneja una respuesta ICE recibida.
     */
    public void handleIceAnswer(String fromUser, SessionDescription answer) {
        iceManager.processAnswer(answer.sdp);
    }
    
    /**
     * Maneja un candidato ICE recibido.
     */
    public void handleIceCandidate(String fromUser, IceCandidate candidate) {
        iceManager.addRemoteCandidate(candidate.candidate);
    }
    
    /**
     * Inicia el audio después de que ICE establezca la conexión.
     */
    private void startAudioAfterIce() {
        DatagramSocket iceSocket = iceManager.getConnectedSocket();
        if (iceSocket != null && audioService != null) {
            audioService.setUdpSocket(iceSocket);
            // Iniciar envío y recepción de audio
            // Nota: Para P2P, solo necesitas el socket, no una lista de peers
            audioService.startReceiving();
            // El envío se iniciará cuando haya audio del micrófono
        }
    }
    
    @Override
    public void endCall() {
        iceManager.close();
        if (audioService != null) {
            audioService.stopAudio();
        }
        activeCallId = null;
        remoteUser = null;
    }
    
    @Override
    public boolean hasActiveCall() {
        return activeCallId != null;
    }
    
    @Override
    public void setAudioService(AudioService audioService) {
        this.audioService = audioService;
    }
}
```

### 5.2 Implementar VoiceObserver en Cliente

**Crear: `client/src/main/java/rpc/VoiceObserverI.java`**

```java
package rpc;

import Chat.*;
import com.zeroc.Ice.Current;
import service.IceCallManager;

public class VoiceObserverI implements VoiceObserver {
    
    private IceCallManager callManager;
    
    public VoiceObserverI(IceCallManager callManager) {
        this.callManager = callManager;
    }
    
    @Override
    public void onVoice(VoiceEntry entry, Current current) {
        // Manejar notas de voz (código existente)
    }
    
    @Override
    public void onIceOffer(String fromUser, SessionDescription offer, Current current) {
        callManager.handleIceOffer(fromUser, offer);
    }
    
    @Override
    public void onIceAnswer(String fromUser, SessionDescription answer, Current current) {
        callManager.handleIceAnswer(fromUser, answer);
    }
    
    @Override
    public void onIceCandidate(String fromUser, IceCandidate candidate, Current current) {
        callManager.handleIceCandidate(fromUser, candidate);
    }
}
```

---

## Paso 6: Integrar con CallManager

### 6.1 Modificar ChatClient

**Modificar: `client/src/main/java/model/ChatClient.java`**

Actualizar para usar `IceCallManager` en lugar de `CallManagerImpl`:

```java
// Reemplazar:
// CallManager callManager = new CallManagerImpl();

// Con:
IceCallManager callManager = new IceCallManager(
    username, 
    callPrx, 
    voiceObserverPrx
);
```

### 6.2 Actualizar Flujo de Llamadas

Modificar el comando `/call` para usar el nuevo flujo ICE:

1. Usuario A ejecuta `/call usuarioB`
2. Servidor notifica a Usuario B
3. Usuario A inicia gathering y envía oferta
4. Usuario B recibe oferta, hace gathering y envía respuesta
5. Ambos intercambian candidatos
6. ICE establece conexión
7. Audio comienza a fluir

---

## Paso 7: Pruebas y Depuración

### 7.1 Verificar STUN

```bash
# Probar servidor STUN
stunclient stun.l.google.com 19302
```

### 7.2 Logs de Depuración

Agregar logs detallados en cada paso:
- Gathering de candidatos
- Envío/recepción de ofertas
- Intercambio de candidatos
- Estado de conexión ICE

### 7.3 Escenarios de Prueba

1. **Misma red**: Ambos clientes en la misma LAN
2. **Diferentes redes**: Clientes en redes diferentes
3. **NAT simétrico**: Cliente detrás de NAT
4. **Firewall**: Cliente detrás de firewall

### 7.4 Troubleshooting Común

**Problema: No se recopilan candidatos**
- Verificar conectividad a servidor STUN
- Revisar firewall bloqueando UDP

**Problema: Conexión no se establece**
- Verificar que candidatos se intercambien correctamente
- Revisar logs de connectivity checks

**Problema: Audio no fluye**
- Verificar que socket ICE esté activo
- Revisar que AudioService use el socket correcto

---

## Resumen de Archivos Modificados/Creados

### Nuevos Archivos:
1. `client/src/main/java/service/IceManager.java`
2. `client/src/main/java/service/IceCallManager.java`
3. `client/src/main/java/rpc/VoiceObserverI.java`
4. `client/src/main/resources/ice-config.json`

### Archivos Modificados:
1. `Services.ice` - Agregar estructuras y métodos ICE
2. `build.gradle` - Agregar dependencia ice4j
3. `server/src/main/java/rpc/CallI.java` - Implementar métodos ICE
4. `client/src/main/java/model/ChatClient.java` - Usar IceCallManager

---

## Próximos Pasos (Opcional)

1. **TURN Server**: Implementar servidor TURN para casos extremos
2. **ICE Restart**: Manejar reinicios de ICE durante llamadas
3. **DTLS/SRTP**: Agregar cifrado para audio
4. **Video**: Extender para soportar video
5. **Llamadas Grupales**: Implementar ICE para múltiples participantes

---

## Recursos Adicionales

- [RFC 8445 - ICE](https://tools.ietf.org/html/rfc8445)
- [ice4j Documentation](https://github.com/jitsi/ice4j)
- [WebRTC Best Practices](https://webrtc.org/getting-started/overview)
- [Coturn Documentation](https://github.com/coturn/coturn)

---

**Nota**: Esta implementación es una guía básica. Para producción, considera:
- Manejo de errores más robusto
- Timeouts y reintentos
- Cifrado de audio (DTLS/SRTP)
- Servidor TURN propio
- Monitoreo y métricas

