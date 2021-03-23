import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class Client {

    private static String name;
    private static String multicastIP = "224.1.1.1";
    private static Integer multicastPort = 12344;
    private static String ASCII = ("___________                        _________                      __  .__    .__                 \n" +
            "\\__    ___/__.__.______   ____    /   _____/ ____   _____   _____/  |_|  |__ |__| ____    ____   \n" +
            "  |    | <   |  |\\____ \\_/ __ \\   \\_____  \\ /  _ \\ /     \\_/ __ \\   __\\  |  \\|  |/    \\  / ___\\  \n" +
            "  |    |  \\___  ||  |_> >  ___/   /        (  <_> )  Y Y  \\  ___/|  | |   Y  \\  |   |  \\/ /_/  > \n" +
            "  |____|  / ____||   __/ \\___  > /_______  /\\____/|__|_|  /\\___  >__| |___|  /__|___|  /\\___  /  \n" +
            "          \\/     |__|        \\/          \\/             \\/     \\/          \\/        \\//_____/  ");
    public static void main(String[] args) throws IOException {
        System.out.println("CLIENT");
        String hostName = "localhost";
        InetAddress address = InetAddress.getByName(hostName);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Your name: ");
        name = scanner.nextLine();
        int serverPortNumber = 12345;

        DatagramSocket datagramSocket = new DatagramSocket();
        MulticastSocket multicastSocket = new MulticastSocket(multicastPort);

        Socket socket = new Socket(hostName, serverPortNumber);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println(name);
        udpStartMessage(datagramSocket, address, serverPortNumber);
        name = in.readLine();

        Thread sendTCPMessageThread = sendMessage(out, datagramSocket, address, serverPortNumber);
        Thread receiveTCPMessageThread = tcpReceiveMessage(in);

        sendTCPMessageThread.start();
        receiveTCPMessageThread.start();

        Thread receiveUDPMessageThread = udpReceiveMessage(datagramSocket);
        receiveUDPMessageThread.start();
        receiveUDPMulticastMessage(multicastSocket).start();

    }

    // ####################################### TCP ####################################### //

    private static Thread sendMessage(PrintWriter out, DatagramSocket datagramSocket, InetAddress address, Integer serverPortAddress){
        return new Thread(() -> {
           Scanner scanner = new Scanner(System.in);
           String clientMessage;
           while(true){
               clientMessage = scanner.nextLine();
               if(clientMessage.equals("U")){
                   udpSendMessage(datagramSocket, address, serverPortAddress);
               }else if(clientMessage.equals("M")){
                   try {
                       udpSendMulticastMessage(datagramSocket);
                   } catch (IOException exception) {
                       exception.printStackTrace();
                   }
               }else{
                   out.println(clientMessage);
               }
           }
        });
    }

    private static Thread tcpReceiveMessage(BufferedReader in){
        return new Thread(() -> {
           try{
               String serverMessage;
               while((serverMessage = in.readLine()) != null){
                   System.out.println(serverMessage);
               }
           }catch (Exception e){
               e.printStackTrace();
           }
        });
    }

    // ####################################### UDP ####################################### //

    private static void udpStartMessage(DatagramSocket socket, InetAddress address, Integer serverPortAddress){
        String startMessage = "start";
        byte[] sendBuffer = startMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, serverPortAddress);
        try {
            socket.send(sendPacket);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
    private static void udpSendMessage(DatagramSocket socket, InetAddress address, Integer serverPortAddress){
        String clientMessage = "["+name+"]: "+ASCII;
        byte[] sendBuffer = clientMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, serverPortAddress);
        try {
            socket.send(sendPacket);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static Thread udpReceiveMessage(DatagramSocket socket){
        return new Thread(() -> {
            byte[] receiveBuffer = new byte[1024];
            while(true){
                Arrays.fill(receiveBuffer, (byte) 0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try {
                    socket.receive(receivePacket);
                    String serverMessage = new String(receivePacket.getData());
                    serverMessage = "#UDP MESSAGE# "+serverMessage;
                    System.out.println(serverMessage);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        });
    }
    // ####################################### UDP multicast ####################################### //
    private static void udpSendMulticastMessage(DatagramSocket datagramSocket) throws IOException {

        String clientMessage = "["+name+"]: "+ASCII;
        byte[] sendBuffer = clientMessage.getBytes();
        InetAddress multicastAddress = InetAddress.getByName(multicastIP);
        DatagramPacket datagramPacket = new DatagramPacket(sendBuffer, sendBuffer.length, multicastAddress, multicastPort);
        datagramSocket.send(datagramPacket);
    }

    private static Thread receiveUDPMulticastMessage(MulticastSocket multicastSocket){
        return new Thread(() ->{
            try {
                multicastSocket.joinGroup(InetAddress.getByName(multicastIP));
                while(!multicastSocket.isClosed()){
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket datagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    multicastSocket.receive(datagramPacket);
                    String multicastMessage = new String(receiveBuffer);
                    System.out.println("MULTICAST UDP "+multicastMessage);

                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }
}
