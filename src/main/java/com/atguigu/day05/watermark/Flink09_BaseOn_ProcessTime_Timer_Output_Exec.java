package com.atguigu.day05.watermark;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class Flink09_BaseOn_ProcessTime_Timer_Output_Exec {
    public static void main(String[] args) throws Exception {
        //StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<WaterSensor> waterSensorStream = streamSource.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String value) throws Exception {
                String[] split = value.split(",");
                return new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
            }
        });
        KeyedStream<WaterSensor, String> keyedStream = waterSensorStream.keyBy(r -> r.getId());
        //TODO 使用基于处理时间的定时器
        //监控水位传感器的水位值，如果水位值在五秒钟之内连续上升，（声明一个变量，用来保存上一次水位值，在五秒钟之内来的第一条数据的时候注册（当定时器）
        // 没有注册的时候进行注册），则报警，（如果五秒钟之内没有连续上升，则删除定时器，不让其报警）并将报警信息输出到侧输出流。
        SingleOutputStreamOperator<String> result = keyedStream.process(new KeyedProcessFunction<String, WaterSensor, String>() {
            //上一次的水位值
            private Integer lastVc=Integer.MIN_VALUE;
            //定时器时间
            private Long timer = Long.MIN_VALUE;
            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<String> out) throws Exception {
                //判断当前水位与上一次水位比是否上升
                if(value.getVc()>lastVc){
                    if(timer==Long.MIN_VALUE){
                        //证明定时器没被注册过
                        //注册一个定时器
                        System.out.println("注册一个定时器"+ctx.getCurrentKey());
                        timer=ctx.timerService().currentProcessingTime()+5000;
                        ctx.timerService().registerProcessingTimeTimer(timer);
                    }

                }else {
                    System.out.println("删除一个定时器"+ctx.getCurrentKey());
                    ctx.timerService().deleteProcessingTimeTimer(timer);
                    //重置定时器时间
                    timer = Long.MIN_VALUE;
                }
                //更新水位
                lastVc = value.getVc();

            }

            @Override
            public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {

                //定时器触发之后
                ctx.output(new OutputTag<String>("output"){}, "报警！水位连续上升");

                timer =Long.MIN_VALUE;
            }
        });

        result.print();
        result.getSideOutput(new OutputTag<String>("output"){}).print("侧输出");

        env.execute();
    }


}

