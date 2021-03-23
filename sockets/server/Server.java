import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    static Map<String, TCPClientHandlerThread> clientHandlerMap = new ConcurrentHashMap<>();
    static Queue<Integer> idQueue = new ConcurrentLinkedQueue<>();
    static ArrayList<ClientData> clientDataList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("CHAT SERVER");
        int portNumber = 12345;
        ServerSocket serverSocket = new ServerSocket(portNumber);
        DatagramSocket datagramSocket = new DatagramSocket(portNumber);
        for(int i = 1000; i < 9999; i++){
            idQueue.add(i);
        }
        new UDPClientHandlerThread(datagramSocket).start();
        while(true){
            new TCPClientHandlerThread(serverSocket.accept()).start();
        }
    }
    static void sendTCPMessageToTheRest(String clientMessage, String clientName){
        for(TCPClientHandlerThread TCPClientHandlerThread : clientHandlerMap.values()){
            if(!clientHandlerMap.get(clientName).equals(TCPClientHandlerThread)){
                TCPClientHandlerThread.sendMessage("["+clientName+"]: "+clientMessage);
            }
        }
    }

    static void sendUDPMessageToTheRest(String clientMessage, int clientPort, InetAddress address,DatagramSocket socket){
        byte[] sendBuffer = clientMessage.getBytes();
        for(ClientData clientData : clientDataList){
            if(!clientData.isSender(address, clientPort)){
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientData.getAddress(), clientData.getPort());
                try{
                    socket.send(sendPacket);
                } catch (Exception exception){
                    exception.printStackTrace();
                }
            }
        }
    }
}
