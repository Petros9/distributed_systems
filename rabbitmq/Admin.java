import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Admin {

    private static String EXCHANGE_NAME = "exchange1";

    private static void sendMessage(Scanner scanner, String group, Channel channel) throws IOException {
        System.out.println("Type the message: ");
        String message = scanner.nextLine();
        String key;
        switch(group){
            case "all":
                key = "admin.crews.suppliers";
                channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes());
                break;
            case "crews":
                key = "admin.crews";
                channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes());
                break;
            case "suppliers":
                key = "admin.suppliers";
                channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes());
                break;
            default:
                System.out.println("WRONG GROUP NAME");
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("ADMIN");
        Scanner scanner = new Scanner(System.in);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        channel.queueDeclare("ADMIN_QUEUE", false, false, false, null);
        channel.queueBind("ADMIN_QUEUE", EXCHANGE_NAME, "#");

        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                String key = envelope.getRoutingKey();
                String message = new String(body);
                System.out.println("[GOT MESSAGE]: "+ message +", [FROM]: "+key);
            }
        };
        channel.basicConsume("ADMIN_QUEUE", true, consumer);

        while(true){
            System.out.println("Type the group: {all, crews, suppliers}");
            String group = scanner.nextLine();
            sendMessage(scanner, group, channel);
        }
    }
}