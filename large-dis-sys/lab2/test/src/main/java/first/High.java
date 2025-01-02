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

// Compute the highest rated movieID per user
// If multiple movies have the same rating, you can pick any.
// The reduce phase needs 3 informations, the userID, and the list of (movieID, rating). You can either:
// - use a String and concatenate bother movieID and rating as an output of the mapper 
// - use a custom class which implements Writable to carry the values
public class High {

    public static class Map extends Mapper<Object, Text, Text, Text> {

        private final Text id = new Text();
        // private final Text title = new Text();
        private final Text rate = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();

            if (fileName.equals("movies.csv")) {
                // String valS = value.toString();
                // int last = valS.lastIndexOf(",");
                // String genreS = valS.substring(0, last);
                // String[] line = genreS.split(",", 2);
                // String idS = line[0];
                // if (idS.equals("movieId")) {
                //     return;
                // }
                // String titleS = line[1];
                // id.set(idS);
                // title.set("t:" + titleS);
                // context.write(id, title);
            } else {
                String[] line = value.toString().split(",");
                String userId = line[0];
                String movieId = line[1];
                if (movieId.equals("movieId")) {
                    return;
                }
                String rateS = line[2];
                id.set(userId);
                rate.set(movieId + "|" + rateS);
                context.write(id, rate);
            }
        }
    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {

        private final Text ret = new Text();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // String title = "";
            float max = 0;
            String id = "";

            for (Text t : values) {
                String[] val = t.toString().split("\\|");
                String movieId = val[0];
                String rate = val[1];
                // if (letter.equals("t") && title.equals("")) {
                //     // title = content;
                // } else {
                float rateF = Float.parseFloat(rate);
                if (max < rateF) {
                    max = rateF;
                    id = movieId;
                }
                // }
            }
            ret.set(id + " , " + max);
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
