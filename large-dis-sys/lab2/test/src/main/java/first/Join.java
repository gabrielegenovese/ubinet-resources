package first;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Join {

    public static class Map extends Mapper<Object, Text, Text, Text> {

        private final Text id = new Text();
        private final Text title = new Text();
        private final Text rate = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();

            if (fileName.equals("movies.csv")) {
                String valS = value.toString();
                int last = valS.lastIndexOf(",");
                String genreS = valS.substring(0, last);
                String[] line = genreS.split(",", 2);
                String idS = line[0];
                if (idS.equals("movieId")) {
                    return;
                }
                String titleS = line[1];
                id.set(idS);
                title.set("t:" + titleS);
                context.write(id, title);
            } else {
                String[] line = value.toString().split(",");
                String idS = line[1];
                if (idS.equals("movieId")) {
                    return;
                }
                String rateS = line[2];
                id.set(idS);
                rate.set("r:" + rateS);
                context.write(id, rate);
            }
        }
    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {

        private final Text ret = new Text();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String title = "";
            float sum = 0;
            int counter = 0;

            for (Text t : values) {
                String[] val = t.toString().split(":", 2);
                String letter = val[0];
                String content = val[1];
                if (letter.equals("t") && title.equals("")) {
                    title = content;
                } else {
                    sum += Float.parseFloat(content);
                    counter++;
                }

            }
            String mean = Float.toString(sum / counter);
            ret.set(title + " , " + mean);
            context.write(key, ret);
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
