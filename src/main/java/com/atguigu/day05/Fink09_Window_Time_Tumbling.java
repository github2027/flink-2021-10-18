package com.atguigu.day05;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import scala.annotation.meta.param;

public class Fink09_Window_Time_Tumbling {

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

        //开启一个基于时间的滚动窗口
        //TODO 窗口开启时间：timestamp - (timestamp - offset + windowSize) % windowSize
        //TODO 左闭右开：public long maxTimestamp() {return end - 1;}
        //TODO 是否会一直创建窗口？首先aggsignwindows这个方法是来一条数据调用一次，因此来一条数据new一个TimeWindow。因为用到了单例模式，所以在一个窗口大小的时间范围内不会创建大量对象

        //size The size of the generated windows.
        //offset The offset which window start would be shifted by.
        //windowStagger The utility that produces staggering offset in runtime.
        WindowedStream<Tuple2<String, Integer>, Tuple, TimeWindow> window = keyedStream.window(TumblingProcessingTimeWindows.of(Time.seconds(5)));
        //<IN> The type of the input value.
        //<OUT> The type of the output value.
        //<KEY> The type of the key.
        //<W> The type of {@code Window} that this window function can be applied on.
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
