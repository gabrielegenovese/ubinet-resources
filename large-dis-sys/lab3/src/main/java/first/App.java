package first;

import java.util.Map;
import java.util.Random;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

public class App {

    static final String SENTENCE_SPOUT_ID = "random-spout";
    // static final String BOLT_ID = "my-bolt";
    static final String TOPOLOGY_NAME = "test-topology";

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.setNumWorkers(1);

        RandomSpout spout = new RandomSpout();

        // sync the filesystem after every 1k tuples
        // SyncPolicy syncPolicy = new CountSyncPolicy(1000);
        // rotate files when they reach 5MB
        // FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, Units.MB);
        // FileNameFormat fileNameFormat = new DefaultFileNameFormat().withPath("/tmp/source/").withExtension(".seq");
        // create sequence format instance.
        // DefaultSequenceFormat format = new DefaultSequenceFormat("timestamp", "sentence");
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("spout", spout);
        // RandomSpout --> MyBolt
        // builder.setBolt(BOLT_ID, bolt, 4).shuffleGrouping(SENTENCE_SPOUT_ID);
        StormSubmitter.submitTopology("test", config, builder.createTopology());
        StormSubmitter.submitTopologyWithProgressBar("test", config, builder.createTopology());
    }

    public static class RandomSpout extends BaseRichSpout {

        private SpoutOutputCollector collector;
        private Random rand;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("number"));
        }

        @Override
        public void open(Map<String, Object> config, TopologyContext context,
                SpoutOutputCollector collector) {
            this.collector = collector;
            this.rand = new Random();
        }

        @Override
        public void nextTuple() {
            Utils.sleep(100);
            Values values = new Values(this.rand.nextInt(50) + 1);
            this.collector.emit(values);
        }
    }

    // public static class MyBolt extends BaseRichBolt {
    //     private HashMap<String, Long> counts = null;
    //     private OutputCollector collector;
    //     @Override
    //     public void prepare(Map<String, Object> config, TopologyContext context, OutputCollector collector) {
    //         this.counts = new HashMap<>();
    //         this.collector = collector;
    //     }
    //     @Override
    //     public void execute(Tuple tuple) {
    //         collector.ack(tuple);
    //     }
    //     @Override
    //     public void declareOutputFields(OutputFieldsDeclarer declarer) {
    //         // this bolt does not emit anything
    //     }
    //     @Override
    //     public void cleanup() {
    //     }
    // }
}
