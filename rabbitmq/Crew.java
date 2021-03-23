import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Crew {
    private static String crewName;
    private static ArrayList<String> equipmentArrayList = new ArrayList<>();
    private static String EXCHANGE_NAME = "exchange1";


    public static void makeAnOrder(Channel channel) throws IOException {
        for(String equipment : equipmentArrayList){
            channel.basicPublish(EXCHANGE_NAME, "equipment."+equipment, null, crewName.getBytes());
        }
    }

    public static void prepareAnOrder(Scanner scanner){
        System.out.println("Type the equipment you are interested in, type F to finish");
        String equipmentName;
        do{
            equipmentName = scanner.nextLine();
            if(!equipmentName.equals("F")) {
                equipmentArrayList.add(equipmentName);
            }
        }
        while(!equipmentName.equals("F"));
        System.out.println("Thank you");
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("CREW");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);


        Scanner scanner = new Scanner(System.in);
        System.out.print("Type your name: ");
        crewName = scanner.nextLine();

        System.out.println(crewName + " working ...");

        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "acknowledgment."+crewName);

        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                String[] message = new String(body).split(":");
                String supplierName = message[0];
                String equipment = message[1];
                System.out.println("FROM "+supplierName+" EQUIPMENT "+equipment);
            }
        };
        channel.basicConsume(queueName, true, consumer);

        String adminMessageQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(adminMessageQueueName, EXCHANGE_NAME, "admin.#.crews.#");
        Consumer adminConsumer = new DefaultConsumer(channel){
          @Override
          public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
              String message = new String(body);
              System.out.println("[ADMIN]: "+message);
          }
        };
        channel.basicConsume(adminMessageQueueName, true, adminConsumer);

        while(true){
            prepareAnOrder(scanner);
            makeAnOrder(channel);
        }
    }
}
