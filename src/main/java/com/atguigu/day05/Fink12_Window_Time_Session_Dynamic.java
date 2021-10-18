package com.atguigu.day05;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SessionWindowTimeGapExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class Fink12_Window_Time_Session_Dynamic {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<WaterSensor> wordToOneStream = streamSource.flatMap(new FlatMapFunction<String, WaterSensor>() {
            @Override
            public void flatMap(String value, Collector<WaterSensor> out) throws Exception {
                String[] split = value.split(",");

                    out.collect(new WaterSensor(split[0],Long.parseLong(split[1])*1000,Integer.parseInt(split[2])));

            }
        });

        KeyedStream<WaterSensor, Tuple> keyedStream = wordToOneStream.keyBy("id");
        //开启一个基于时间的会话窗口（选一个时间字段作为动态间隔）

        WindowedStream<WaterSensor, Tuple, TimeWindow> window = keyedStream.window(ProcessingTimeSessionWindows.withDynamicGap(new SessionWindowTimeGapExtractor<WaterSensor>() {
            @Override

            public long extract(WaterSensor element) {
                return element.getTs();
            }
        }));

        SingleOutputStreamOperator<String> processWindow = window.process(new ProcessWindowFunction<WaterSensor, String, Tuple, TimeWindow>() {
            @Override
            public void process(Tuple tuple, Context context, Iterable<WaterSensor> elements, Collector<String> out) throws
                    Exception {
                String msg =
                        "窗口: [" + context.window().getStart() / 1000 + "," + context.window().getEnd() / 1000 + ") 一共有 "
                                + elements.spliterator().estimateSize() + "条数据 ";
                out.collect(msg);

            }
        });
        processWindow.print();

        window.sum("vc").print();

        env.execute();
    }

}
