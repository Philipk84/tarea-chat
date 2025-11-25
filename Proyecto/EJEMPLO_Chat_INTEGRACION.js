// EJEMPLO: Cómo integrar callService en Chat.js
// Modificar: web-client/src/pages/Chat.js
// Agregar estas líneas en la función Chat()

import callService from "../services/callService.js";
import voiceDelegate from "../services/voiceDelegate.js";

function Chat() {
  const username = localStorage.getItem("chat_username");
  // ... código existente ...

  // ← NUEVO: Inicializar callService
  callService.init(username);

  // ← NUEVO: Configurar callbacks de voiceDelegate para llamadas
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

  // ← NUEVO: Suscribirse a eventos de callService
  callService.on("onIncomingCall", (fromUser) => {
    const accept = confirm(`Llamada entrante de ${fromUser}. ¿Aceptar?`);
    if (accept) {
      callService.acceptCall(fromUser, username);
    } else {
      callService.rejectCall(fromUser);
    }
  });

  // ← NUEVO: Reproducir audio remoto
  let remoteAudioElement = null;

  callService.on("onRemoteStream", (stream) => {
    if (!remoteAudioElement) {
      remoteAudioElement = document.createElement("audio");
      remoteAudioElement.autoplay = true;
      remoteAudioElement.style.display = "none";
      document.body.appendChild(remoteAudioElement);
    }

    remoteAudioElement.srcObject = stream;
    remoteAudioElement.play().catch((err) => {
      console.error("Error reproduciendo audio remoto:", err);
    });
  });

  callService.on("onCallStateChange", (state) => {
    console.log("Estado de llamada:", state);
    if (state === "connected") {
      console.log("✓ Llamada conectada!");
    }
  });

  callService.on("onCallEnded", () => {
    if (remoteAudioElement) {
      remoteAudioElement.srcObject = null;
    }
  });

  // ... código existente para crear UI ...

  // ← NUEVO: Función para agregar botón de llamada
  function addCallButton(chatElement, chat) {
    if (chat.type !== "user") {
      return; // Solo para chats privados
    }

    const callBtn = document.createElement("button");
    callBtn.textContent = "📞";
    callBtn.title = "Llamar";
    callBtn.style.marginLeft = "10px";
    callBtn.style.padding = "5px 10px";
    callBtn.style.cursor = "pointer";

    callBtn.onclick = async () => {
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

  // ← NUEVO: Llamar a addCallButton cuando se selecciona un chat
  // (en la función donde manejas la selección de chat, por ejemplo:
  //  cuando se hace clic en un usuario de la lista)
  
  // Ejemplo de dónde agregarlo:
  // function selectChat(chat) {
  //   currentChat = chat;
  //   // ... código existente ...
  //   addCallButton(chatElement, chat); // ← Agregar aquí
  // }

  // ... resto del código existente ...
}

