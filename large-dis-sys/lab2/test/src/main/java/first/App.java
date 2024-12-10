package first;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

public class App {

    public static class Map extends MapReduceBase implements Mapper<Object, Text, Text, Text> {

      @Override
      public void map(Object key, Text value, OutputCollector<Text, Text> output, Reporter context) throws IOException {
        output.collect(new Text(key.toString()), value);
      }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
      @Override
      public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter context) throws IOException {
        StringBuilder concatenatedValues = new StringBuilder();

        while (values.hasNext()) {
          String value = new String(values.next().getBytes(), StandardCharsets.UTF_8);
          concatenatedValues.append(value).append(",");
        }
        
        if (concatenatedValues.length() > 0) {
            concatenatedValues.setLength(concatenatedValues.length() - 1);
        }

        output.collect(key, new Text(concatenatedValues.toString()));
      }
    }

    public static void main(String[] args) throws Exception {
      JobConf conf = new JobConf(App.class);
      conf.setJobName("wordcount");

      conf.setOutputKeyClass(Text.class);
      conf.setOutputValueClass(Text.class);

      conf.setMapperClass(Map.class);
      conf.setCombinerClass(Reduce.class);
      conf.setReducerClass(Reduce.class);

      conf.setInputFormat(TextInputFormat.class);
      conf.setOutputFormat(TextOutputFormat.class);

      FileInputFormat.setInputPaths(conf, new Path(args[0]));
      FileOutputFormat.setOutputPath(conf, new Path(args[1]));

      JobClient.runJob(conf);
    }
}