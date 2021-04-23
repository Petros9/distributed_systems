import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    static Process process;
    private static ZooKeeper zooKeeper;
    private static final String rootNode = "/z";
    static CountDownLatch countDownLatch = new CountDownLatch(1);
    static AtomicBoolean stillRunning = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Zookeeper App");

        String graphicsProgrammePath;
        String paintPath = "C:\\windows\\system32\\mspaint.exe";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Type path to the graphical program/type ENTER (default Paint) to continue: ");
        graphicsProgrammePath = reader.readLine();
        if(graphicsProgrammePath.equals("")){
            graphicsProgrammePath = paintPath;
        }

        ZooKeeperConnection zooKeeperConnection = new ZooKeeperConnection();
        zooKeeper = zooKeeperConnection.openZookeeperConnection("localhost:2181");

        Watcher childrenNodeWatcher = new Watcher() { // when child is created
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() != Event.EventType.None) {
                    try {
                        zooKeeper.addWatch(rootNode, this, AddWatchMode.PERSISTENT_RECURSIVE);
                        System.out.println("Children nodes: " + zooKeeper.getAllChildrenNumber(rootNode));
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        try {
            zooKeeper.addWatch(rootNode, childrenNodeWatcher, AddWatchMode.PERSISTENT_RECURSIVE);
        } catch (KeeperException keeperException) {
            keeperException.printStackTrace();
            System.exit(1);
        }

        String finalGraphicsProgrammePath = graphicsProgrammePath;
        Watcher rootNodeWatcher = new Watcher() { // checks whether /z node exists
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() != Event.EventType.None) {
                    try {
                        Stat stat = zooKeeper.exists(event.getPath(), this);
                        System.out.println("Exists: " + (stat != null) + " Path: " + event.getPath() + " EventType: "
                                + event.getType());
                        switch (event.getType()) {
                            case NodeCreated:
                                System.out.println("/z node created ");
                                zooKeeper.getChildren(rootNode, childrenNodeWatcher);
                                process = Runtime.getRuntime().exec(finalGraphicsProgrammePath);
                                break;
                            case NodeDeleted:
                                System.out.println("/z node deleted ");
                                if (process != null)
                                    process.destroy();
                                break;
                        }
                    } catch (KeeperException | InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        try {
            Stat stat = zooKeeper.exists(rootNode, rootNodeWatcher);
            System.out.println("/z exists: " + (stat != null));
            if (stat != null) {
                printChildrenUsingWatcher(rootNode, childrenNodeWatcher);
            }
            System.out.println();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        
        Thread consoleThread = new Thread(() -> {
            while (stillRunning.get()) {
                try {
                    System.out.println("Type \'tree\' or \'quit\':");
                    String cmd = reader.readLine();
                    if ("tree".equals(cmd)) {
                        printTreeStructure(rootNode);
                    } else if (cmd.equals("quit")) {
                        stillRunning.set(false);
                    } else {
                        System.out.println("Wrong command");
                    }
                } catch (IOException | KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        consoleThread.start();
        countDownLatch.await();
        stillRunning.set(false);
        consoleThread.interrupt();
    }

    public static void printTreeStructure(String rootPath) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(rootPath, null) != null) {
            System.out.println(rootPath);
            printChildren(rootPath);
        } else {
            System.out.println("Znode '/z' does not exist\n");
        }
    }

    private static void printChildren(String parent) {
        List<String> childrenList;
        String parentString;
        if(parent.equals("/")){
            parentString = "/";
        } else {
            parentString = parent + "/";
        }
        try {
            childrenList = zooKeeper.getChildren(parent, null);
            for (String child : childrenList) {
                System.out.println(parentString + child);
                printChildren(parentString + child);
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
    static void printChildrenUsingWatcher(String parent, Watcher watcher) throws KeeperException, InterruptedException {
        List<String> childrenList = zooKeeper.getChildren(parent, watcher);
        for (String child : childrenList) {
            System.out.println("child: /z/" + child);
        }
    }
}