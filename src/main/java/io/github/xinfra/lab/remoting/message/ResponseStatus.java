package io.github.xinfra.lab.remoting.message;

import lombok.Getter;

public enum ResponseStatus {
    SUCCESS((short) 0),

    UNKNOWN((short) 1),

    ERROR((short) 2),

    CLIENT_SEND_ERROR((short) 3),

    TIMEOUT((short) 4),

    CONNECTION_CLOSED((short) 5),

    SERVER_EXCEPTION((short) 6),

    SERVER_SERIAL_EXCEPTION((short) 7),

    SERVER_DESERIAL_EXCEPTION((short) 8),

    CLIENT_SERIAL_EXCEPTION((short) 9),

    CLIENT_DESERIAL_EXCEPTION((short) 10),
    ;

    @Getter
    private short code;

    ResponseStatus(short code) {
        this.code = code;
    }


    public static ResponseStatus valueOf(short status) {
        for (ResponseStatus s : values()) {
            if (s.getCode() == status) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status value ," + status);
    }
}
