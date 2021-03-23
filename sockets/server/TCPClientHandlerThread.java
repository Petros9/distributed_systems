import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClientHandlerThread extends Thread{
    private final PrintWriter out;
    private final String clientName;
    private BufferedReader in;

    public TCPClientHandlerThread(Socket clientSocket) throws IOException {
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        clientName = in.readLine()+'#'+Server.idQueue.poll();
        Server.clientHandlerMap.put(clientName, this);
        out.println(clientName);
    }


    void sendMessage(String serverMessage){
        try{
            out.println(serverMessage);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        try {
            String clientMessage;
            while((clientMessage = in.readLine()) != null){
                    System.out.println("["+clientName+"]: "+clientMessage);
                    Server.sendTCPMessageToTheRest(clientMessage, clientName);
            }
        } catch (IOException e) { e.printStackTrace();
        }finally {
            if(out != null) out.close();
        }
    }
}

