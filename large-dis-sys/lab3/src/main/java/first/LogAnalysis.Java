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
import java.io.File;  
import java.io.FileNotFoundException; 
import java.util.Scanner;

/*

Spout: read from file

first bolt: filter lines

second bolt: format and extract useful data (username-ip)

can use a bolt foreach information (we can scale depending on the data)
si usa il windowing per controllare che si usino 5 attempt alla volta

meglio perché sennò non si riescono a contare bene le cose quando creiamo più repliche

per l'authentication failure si manda dal filter a un altro bolt senza window così fa la notifica subito

usare il naming degli streams per mandare dopo il format

*/

public class LogAnalysis {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.setDebug(true);
        config.setNumWorkers(1);

        RandomSpout spout = new RandomSpout();
        FilterBolt filterBolt = new FilterBolt();
        FormatBolt formatBolt = new FormatBolt();

        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("read-spout", spout);
        builder.setBolt("filter-bolt", filterBolt).shuffleGrouping("random-spout");
        builder.setBolt("format-bolt", formatBolt).shuffleGrouping("format-bolt");

        StormSubmitter.submitTopology("test", config, builder.createTopology());
    }

    public static class ReadSpout extends BaseRichSpout {

        private SpoutOutputCollector collector;
        private File file;
        private Scanner reader;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("line"));

            try {
                file = new File("SSH.logs");
                Scanner reader = new Scanner(file);
            } catch (FileNotFoundException e) {
                System.out.println("File not found.");
                e.printStackTrace();
            }
        }

        @Override
        public void open(Map<String, Object> config, TopologyContext context, SpoutOutputCollector _collector) {
            collector = _collector;
        }

        @Override
        public void nextTuple() {
            Utils.sleep(1000);
            if (reader.hasNextLine()) {
                String data = reader.nextLine();
                collector.emit(data);
            } else {
                collector.emit("EOF");
            }
        }
    }

    public static class FilterBolt extends BaseRichBolt {

        private OutputCollector collector;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
        }

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
            collector = _collector;
        }

        @Override
        public void execute(Tuple tuple) {
        }
    }

    public static class AvgBolt extends BaseRichBolt {

        private OutputCollector collector;

        @Override
        public void prepare(Map conf, TopologyContext context, OutputCollector _collector) {
            collector = _collector;
        }

        @Override
        public void execute(Tuple tuple) {
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
        }
    }
}
