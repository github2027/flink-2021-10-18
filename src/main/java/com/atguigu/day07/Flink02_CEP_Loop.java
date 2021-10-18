package com.atguigu.day07;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class Flink02_CEP_Loop {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        SingleOutputStreamOperator<WaterSensor> waterSensorStream = env.readTextFile("input/sensor.txt")
                .map(new MapFunction<String, WaterSensor>() {
                    @Override
                    public WaterSensor map(String value) throws Exception {
                        String[] split = value.split(",");

                        return new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
                    }
                })
                .assignTimestampsAndWatermarks(WatermarkStrategy.<WaterSensor>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner(new SerializableTimestampAssigner<WaterSensor>() {
                            @Override
                            public long extractTimestamp(WaterSensor element, long recordTimestamp) {

                                return element.getTs();
                            }
                        })
                );
        //TODO 1.定义模式
        Pattern<WaterSensor, WaterSensor> pattern = Pattern.<WaterSensor>begin("start")
                //TODO 迭代条件
//                .where(new IterativeCondition<WaterSensor>() {
//                    @Override
//                    public boolean filter(WaterSensor waterSensor, Context<WaterSensor> context) throws Exception {
//                        return "sensor_1".equals(waterSensor.getId());
//                    }
//                })
                //TODO 简单条件
                .where(new SimpleCondition<WaterSensor>() {
                    @Override
                    public boolean filter(WaterSensor value) throws Exception {
                        return "sensor_1".equals(value.getId());
                    }
                })
                //TODO 循环模式 固定次数
//                .times(2);
                //TODO 循环模式 范围内的次数
//                .times(2,4);
                //TODO 循环模式 一次或多次
                 // .oneOrMore();
                //TODO 循环模式 多次及多次以上
                    .timesOrMore(2);
        //TODO 2.将模式作用于流上
        PatternStream<WaterSensor> patternStream = CEP.pattern(waterSensorStream, pattern);

        //TODO 3.获取符合规则的数据

        SingleOutputStreamOperator<String> select = patternStream.select(new PatternSelectFunction<WaterSensor, String>() {
            //Map中的value是过滤后的数据集,key是begin方法中取的名字。
            @Override
            public String select(Map<String, List<WaterSensor>> map) throws Exception {
                return map.toString();
            }
        });

        select.print();

        env.execute();
    }
}
