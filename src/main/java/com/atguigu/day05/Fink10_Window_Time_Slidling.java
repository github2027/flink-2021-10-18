package com.atguigu.day05;

import com.sun.java.swing.plaf.windows.resources.windows;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class Fink10_Window_Time_Slidling {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<Tuple2<String, Integer>> wordToOneStream = streamSource.flatMap(new FlatMapFunction<String, Tuple2<String, Integer>>() {
            @Override
            public void flatMap(String value, Collector<Tuple2<String, Integer>> out) throws Exception {
                String[] words = value.split(" ");
                for (String word : words) {
                    out.collect(Tuple2.of(word, 1));
                }
            }
        });

        KeyedStream<Tuple2<String, Integer>, Tuple> keyedStream = wordToOneStream.keyBy(0);
        //开启一个基于时间的滑动窗口
        //假设第一秒来了一条数据 窗口大小是6 滑动步长是2
        //timestamp - (timestamp - offset + windowSize) % windowSize 1-(1-0+2)%2=0 -> lastStart=0
        //for (long start = lastStart;start > timestamp - size;start -= slide){
        //          windows.add(new TimeWindow(start, start + size));
        //      (0,6) (-2,4)(-4,2)
        //		}
        WindowedStream<Tuple2<String, Integer>, Tuple, TimeWindow> window = keyedStream.window(SlidingProcessingTimeWindows.of(Time.seconds(6), Time.seconds(2)));
        SingleOutputStreamOperator<String> processWindow = window.process(new ProcessWindowFunction<Tuple2<String, Integer>, String, Tuple, TimeWindow>() {
            @Override
            public void process(Tuple tuple, Context context, Iterable<Tuple2<String, Integer>> elements, Collector<String> out) throws
                    Exception {
                String msg =
                        "窗口: [" + context.window().getStart() / 1000 + "," + context.window().getEnd() / 1000 + ") 一共有 "
                                + elements.spliterator().estimateSize() + "条数据 ";
                out.collect(msg);

            }
        });
        processWindow.print();

        window.sum(1).print();

        env.execute();
    }

}
