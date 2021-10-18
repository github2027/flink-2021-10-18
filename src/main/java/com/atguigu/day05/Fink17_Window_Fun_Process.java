package com.atguigu.day05;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
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

public class Fink17_Window_Fun_Process {

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
        //开启一个基于时间的滚动窗口
        WindowedStream<WaterSensor, Tuple, TimeWindow> window = keyedStream.window(TumblingProcessingTimeWindows.of(Time.seconds(5)));


        //TODO 使用窗口函数，全窗口函数process,对数据做计数操作
        window.process(new ProcessWindowFunction<WaterSensor, Integer, Tuple, TimeWindow>() {
            private Integer count = 0;
            @Override
            public void process(Tuple tuple, Context context, Iterable<WaterSensor> elements, Collector<Integer> out) throws Exception {
                System.out.println("process...");
                for (WaterSensor element : elements) {
                    count++;
                    out.collect(count);
                }
            }
        }).print();

        env.execute();


    }

}
