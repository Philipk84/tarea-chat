package com.chatapp.server;

import java.util.*;

/**
 * Gestiona llamadas activas (solo señalización).
 * callId -> set of participants' usernames
 */
public class CallManager {

    // callId -> participants (usernames)
    private final Map<String, Set<String>> calls = new HashMap<>();

    // username -> callId (if in call)
    private final Map<String, String> userToCall = new HashMap<>();

    public synchronized String createCall(Set<String> participants) {
        String callId = UUID.randomUUID().toString();
        calls.put(callId, new HashSet<>(participants));
        for (String u : participants) userToCall.put(u, callId);
        return callId;
    }

    public synchronized void endCall(String callId) {
        Set<String> parts = calls.remove(callId);
        if (parts != null) {
            for (String u : parts) userToCall.remove(u);
        }
    }

    public synchronized boolean isInCall(String username) {
        return userToCall.containsKey(username);
    }

    public synchronized String getCallOfUser(String username) {
        return userToCall.get(username);
    }

    public synchronized Set<String> getParticipants(String callId) {
        return calls.getOrDefault(callId, Collections.emptySet());
    }
}
