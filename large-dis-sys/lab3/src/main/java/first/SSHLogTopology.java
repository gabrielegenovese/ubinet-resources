package first;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt.Count;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.windowing.TupleWindow;

public class SSHLogTopology {

    public static void main(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("spout", new LogReaderSpout(), 1);
        builder.setBolt("filter", new FilteringBolt()).shuffleGrouping("spout");
        builder.setBolt("count", new LoggingAttempsBolt().withWindow(new Count(5), new Count(5))).shuffleGrouping("filter", "loggingAttempts");
        builder.setBolt("usernames", new UsernamesBolt().withWindow(new Count(5), new Count(5))).shuffleGrouping("filter", "username");
        builder.setBolt("notification", new NotificationBolt()).shuffleGrouping("filter", "notification");

        Config conf = new Config();
        conf.setDebug(true);
        conf.put("logFile", "/storm/labs/SSH.log");
        if (args != null && args.length > 0) {
            StormSubmitter.submitTopologyWithProgressBar(args[0], conf, builder.createTopology());
        }
    }

    public static class LogReaderSpout extends BaseRichSpout {

        SpoutOutputCollector _collector;
        BufferedReader fileReader;

        @Override
        public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
            _collector = collector;
            try {
                fileReader = new BufferedReader(new FileReader(conf.get("logFile").toString()));
            } catch (FileNotFoundException e) {
                // e.printStackTrace();
            }
        }

        @Override
        public void nextTuple() {
            Utils.sleep(100);
            if (fileReader != null) {
                String l;
                try {
                    l = fileReader.readLine();
                    if (l != null) {
                        _collector.emit(new Values(l));
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("line"));
        }
    }

    public static class FilteringBolt extends BaseBasicBolt {

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declareStream("loggingAttempts", new Fields("int"));
            declarer.declareStream("username", new Fields("line"));
            declarer.declareStream("ip", new Fields("line"));
            declarer.declareStream("notification", new Fields("notification"));
        }

        @Override
        public Map<String, Object> getComponentConfiguration() {
            return null;
        }

        @Override
        public void execute(Tuple tuple, BasicOutputCollector collector) {
            String line = tuple.getStringByField("line");
            if (line.contains("Failed password")) {
                // no need to send anything to the bolt countint attempts
                collector.emit("loggingAttempts", new Values(1));
                collector.emit("username", new Values(line));
                collector.emit("ip", new Values(line));
            }
            if (line.contains("Too many authentication failures")) {
                collector.emit("notification", new Values(line));
            }
        }
    }

    public static class UsernamesBolt extends BaseWindowedBolt {

        private final TreeMap<String, Integer> usernames = new TreeMap<>();

        @Override
        public void execute(TupleWindow inputWindow) {
            for (Tuple tuple : inputWindow.get()) {
                String line = tuple.getStringByField("line");
                //sanity check, should be unnecessary if topology is correct
                if (line.contains("Failed password")) {
                    String username;
                    if (line.contains("invalid ")) {
                        username = line.split(" ")[10];
                    } else {
                        username = line.split(" ")[8];
                    }
                    if (usernames.containsKey(username)) {
                        usernames.put(username, usernames.get(username) + 1);
                    } else {
                        usernames.put(username, 1);
                    }
                }

            }
            //print the top 5
            List<Entry<String, Integer>> sorted = usernames.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(5).collect(Collectors.toList());
            for (Entry<String, Integer> l : sorted) {
                System.out.println(l.getKey() + " " + l.getValue());
            }
        }
    }

    public static class NotificationBolt extends BaseBasicBolt {

        @Override
        public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
            String line = tuple.getStringByField("notification");
            //Sanity check, should be unnecessary if topology is correct
            if (line.contains("Too many authentication failures")) {
                System.out.println("Warning: " + line);
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
        }
    }

    public static class LoggingAttempsBolt extends BaseWindowedBolt {

        private int count = 0;

        @Override
        public void execute(TupleWindow inputWindow) {
            for (@SuppressWarnings("unused")Tuple tuple : inputWindow.get()) {
                count++;
            }

            System.out.println("Number of attempts: " + count);

        }
    }
}
