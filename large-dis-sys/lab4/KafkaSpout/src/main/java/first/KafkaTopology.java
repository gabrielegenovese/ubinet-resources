package first;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import static org.apache.storm.kafka.spout.FirstPollOffsetStrategy.EARLIEST;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff;
import org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff.TimeInterval;
import org.apache.storm.kafka.spout.KafkaSpoutRetryService;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt.Count;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;

public class KafkaTopology {

    public static void main(String[] args) throws Exception {
        KafkaTopology firstTopology = new KafkaTopology();
        TopologyBuilder builder = firstTopology.KafkaSpoutTopology();
        Config conf = new Config();
        conf.setDebug(true);

        if (args != null && args.length > 0) {
            StormSubmitter.submitTopologyWithProgressBar(args[0], conf, builder.createTopology());
        } else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("word-count", conf, builder.createTopology());
            Thread.sleep(10000);
            cluster.shutdown();
        }
    }

    private TopologyBuilder KafkaSpoutTopology() {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("spout", new KafkaSpout<>(getKafkaSpoutConfig("kafka:9092")), 1);
        builder.setBolt("filter", new ParsingBolt()).shuffleGrouping("spout");
        builder.setBolt("counter", new CounterBolt().withWindow(new Count(10), new Count(10))).shuffleGrouping("filter");
        return builder;
    }

    protected KafkaSpoutConfig<String, String> getKafkaSpoutConfig(String bootstrapServers) {
        return KafkaSpoutConfig.builder(bootstrapServers, new String[]{"first"})
                .setProp(ConsumerConfig.GROUP_ID_CONFIG, "kafkaSpoutTestGroup")
                .setRetry(getRetryService())
                .setOffsetCommitPeriodMs(10000)
                .setFirstPollOffsetStrategy(EARLIEST)
                .setMaxUncommittedOffsets(250)
                .build();
    }

    protected KafkaSpoutRetryService getRetryService() {
        return new KafkaSpoutRetryExponentialBackoff(TimeInterval.microSeconds(500),
                TimeInterval.milliSeconds(2), Integer.MAX_VALUE, TimeInterval.seconds(10));
    }

    public static class ParsingBolt extends BaseRichBolt {

        private OutputCollector collector;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declareStream("word", new Fields("line"));
        }

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
            collector = _collector;
        }

        @Override
        public void execute(Tuple tuple) {

            String line = (String) tuple.getValue(5);
            String[] words = line.split(" ");
            for (String w : words) {
                collector.emit("word", new Values(w.replaceAll(",|;|:|.|-|_|\\'|\\?", "")));
            }
        }
    }

    public static class CounterBolt extends BaseWindowedBolt {

        private OutputCollector collector;
        private HashMap<String, Integer> counter;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declareStream("word", new Fields("line"));
        }

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
            collector = _collector;
        }

        @Override
        public void execute(TupleWindow inputWindow) {
            for (Tuple tuple : inputWindow.get()) {
                String word = tuple.getStringByField("word");

                if (!counter.containsKey(word)) {
                    counter.put(word, 1);
                } else {
                    int n = counter.get(word);
                    counter.put(word, n + 1);
                }
            }
        }
    }
}
