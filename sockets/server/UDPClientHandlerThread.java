import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class UDPClientHandlerThread extends Thread{

    private final DatagramSocket socket;
    public UDPClientHandlerThread(DatagramSocket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        byte[] receiveBuffer = new byte[1024];

        while(true){
            try{
                Arrays.fill(receiveBuffer, (byte) 0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);

                String clientMessage = new String(receivePacket.getData());

                InetAddress address = receivePacket.getAddress();
                int port = receivePacket.getPort();
                if(!clientMessage.contains("#")){
                    Server.clientDataList.add(new ClientData(address, port));
                }else{
                    System.out.println("UDP MESSAGE: " + clientMessage);
                    Server.sendUDPMessageToTheRest(clientMessage, port, address, socket);
                }
            }catch(Exception exception){
                exception.printStackTrace();
            }
        }
    }
}
