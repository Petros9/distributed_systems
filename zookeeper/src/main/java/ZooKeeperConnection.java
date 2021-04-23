import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperConnection {
    private ZooKeeper zooKeeper;
    private CountDownLatch countDownLatch = new CountDownLatch(1);


    public ZooKeeper openZookeeperConnection(String host) throws IOException, InterruptedException {
        this.zooKeeper = new ZooKeeper(host, 2000, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    countDownLatch.countDown();
                }
            }
        });
        countDownLatch.await();
        return this.zooKeeper;
    }

    public void closeZookeeperConnection() throws InterruptedException {
        this.zooKeeper.close();
    }
}
