package com.atguigu.es.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Smexy on 2022/4/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Emp {

    private Long empid;
    private Integer age;
    private Double balance;
    private String name;
    private String gender;
    private String hobby;

}
