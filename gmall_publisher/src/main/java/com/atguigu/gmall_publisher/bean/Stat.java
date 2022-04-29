package com.atguigu.gmall_publisher.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Stat {
    String title;
    List<Option> options;
}