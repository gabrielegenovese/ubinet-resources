package first;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

public class App {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.setDebug(true);
        config.setNumWorkers(1);

        RandomSpout spout = new RandomSpout();
        AvgBolt bolt = new AvgBolt();

        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("random-spout", spout);
        builder.setBolt("filter-bolt", bolt).shuffleGrouping("random-spout");
        builder.setBolt("avg-bolt", bolt).shuffleGrouping("filter-bolt");

        StormSubmitter.submitTopology("test", config, builder.createTopology());
    }

    public static class RandomSpout extends BaseRichSpout {

        private SpoutOutputCollector collector;
        private Random rand;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("number"));
        }

        @Override
        public void open(Map<String, Object> config, TopologyContext context, SpoutOutputCollector _collector) {
            collector = _collector;
            rand = new Random();
        }

        @Override
        public void nextTuple() {
            Utils.sleep(100);
            Values values = new Values(rand.nextInt(100));
            collector.emit(values);
        }
    }

    public static class Filter extends BaseRichBolt {

        private OutputCollector collector;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("number"));
        }

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
            collector = _collector;
        }

        @Override
        public void execute(Tuple tuple) {
            int n = tuple.getIntegerByField("number");
            // emit only odd numbers
            if (n % 2 == 1) {
                collector.emit(new Values(n));
            }
            collector.ack(tuple);
        }

    }

    public static class AvgBolt extends BaseRichBolt {

        private int sum;
        private Queue<Integer> numbers;
        private OutputCollector collector;

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
            collector = _collector;
            sum = 0;
            numbers = new LinkedList<>();
        }

        @Override
        public void execute(Tuple tuple) {
            int n = tuple.getInteger(0);
            numbers.add(n);
            int remove = 0;
            if (numbers.size() > 10) {
                remove = numbers.remove();
            }
            sum += n - remove;
            collector.emit(new Values(sum / 10));
            collector.ack(tuple);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("avg"));
        }

    }
}
