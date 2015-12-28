package io.sipstack.example.trunking;

import java.util.Locale;

public enum EdgeType {
    PHONE, PUBLIC_SIP, CLIENT, P2P_PUBLIC_SIP;

    public static EdgeType parse(final String facility) {
        return valueOf(facility.toUpperCase(Locale.ENGLISH).replace("-", "_"));
    }
}
