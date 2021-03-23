import com.rabbitmq.client.*;
import jdk.nashorn.internal.runtime.regexp.JoniRegExp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Supplier {
    private static ArrayList<String> equipmentArrayList = new ArrayList<>();
    private static Map<String, Thread> threadMap = new HashMap<>();
    private static String supplierName;
    private static String EXCHANGE_NAME = "exchange1";

    private static void getAvailableEquipment(Scanner scanner){
        System.out.println("Declare available equipments at your store, type F to finish");
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


    public static void createEquipmentQueues(Channel channel) throws IOException {
        for(String equipment : equipmentArrayList){
            channel.queueDeclare(equipment, false, false, false, null);
            channel.queueBind(equipment, EXCHANGE_NAME, "equipment."+equipment);
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("SUPPLIER");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Type your name: ");
        supplierName = scanner.nextLine();
        Channel channel = connection.createChannel();
        String adminMessageQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(adminMessageQueueName, EXCHANGE_NAME, "admin.#.suppliers.#");

        Consumer adminConsumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                String message = new String(body);
                System.out.println("[ADMIN]: "+message);
            }
        };
        channel.basicConsume(adminMessageQueueName, true, adminConsumer);
        getAvailableEquipment(scanner);
        System.out.println(supplierName + " working ...");

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        channel.basicQos(equipmentArrayList.size() + 1);
        createEquipmentQueues(channel);


        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String crewName = new String(body);
                String crewEquipment = envelope.getRoutingKey().split("\\.")[1];
                System.out.println("ORDER FROM " + crewName + " FOR " + crewEquipment);
                String acknowledgment = supplierName + ":" + crewEquipment;
                channel.basicPublish(EXCHANGE_NAME, "acknowledgment."+crewName, null, acknowledgment.getBytes());
            }
        };


        for(String equipment : equipmentArrayList) {
            channel.basicConsume(equipment, false, consumer);
        }
    }
}
