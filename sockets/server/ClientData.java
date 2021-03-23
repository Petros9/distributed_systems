import java.net.InetAddress;

public class ClientData {
    private final InetAddress address;
    private final Integer port;

    public ClientData(InetAddress address, Integer port){
        this.address = address;
        this.port = port;
    }

    public boolean isSender(InetAddress address, Integer port){
        return (this.address.equals(address) && this.port.equals(port));
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public Integer getPort() {
        return this.port;
    }
}
