package first;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class User {

    public static class Map extends Mapper<Object, Text, Text, Text> {

        private final Text user = new Text();
        private final Text rate = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] list = value.toString().split(",");
            String userid = list[0];
            String rating = list[2];
            user.set(userid);
            rate.set(rating);
            if (!userid.equals("userId")) {
                context.write(user, rate);
            }
        }
    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {

        private final Text rate = new Text();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            float sum = 0;
            int counter = 0;
            for (Text val : values) {
                sum += Float.parseFloat(val.toString());
                counter++;
            }
            String mean = Float.toString(sum / counter);
            rate.set(mean);
            context.write(key, rate);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = new Job(conf, "Genre");
        job.setJarByClass(Moovie.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
