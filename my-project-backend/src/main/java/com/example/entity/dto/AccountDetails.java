package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("db_account_details")
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetails implements BaseData {
    @TableId
    Integer id;
    Integer gender;
    String phone;
    String qq;
    String wx;
    @TableField("`desc`")//。这是是 `不是单引号这里的 "`desc`" 是一个带有单引号的字符串，表示在 SQL 查询中，desc 会被当作一个字符串常量
    String desc;
}
