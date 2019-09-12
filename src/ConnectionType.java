public enum ConnectionType {
    CHUNK(100), CLIENT(200), CLIENT_SEND(1000), CHUNK_SEND(2000);

    private final int value;
    private ConnectionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ConnectionType fromInteger(int x) {
        switch(x) {
            case 100:
                return CHUNK;
            case 200:
                return CLIENT;
        }
        return null;
    }
}
