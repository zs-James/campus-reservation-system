package campus.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user")
public class User {
    /**
     * 用户ID (主键)
     * 数据库自增，唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录用户名
     * 唯一，不可重复，用于登录认证
     */
    private String username;

    /**
     * 登录密码
     * 存储的是经过 BCrypt 加密后的密文，绝对不存明文
     */
    private String password;

    /**
     * 用户昵称
     * 用于前端页面展示的称呼，允许重复
     */
    private String nickname;

    /**
     * 手机号
     * 用于联系或作为备用登录方式，有格式校验
     */
    private String phone;

    /**
     * 用户头像
     * 存储图片的 URL 地址
     */
    private String avatar;

    /**
     * 用户角色
     * 0：普通用户（学生/教职工）
     * 1：管理员（负责场地和预约管理）
     */
    private Integer role;

    /**
     * 账号状态
     * 1：正常（允许登录）
     * 0：禁用（封号，禁止登录）
     */
    private Integer status;

    /**
     * 创建时间
     * 记录账号注册的时间，插入数据时由数据库默认生成 (CURRENT_TIMESTAMP)
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 记录账号信息最后一次修改的时间，更新数据时自动更新 (ON UPDATE CURRENT_TIMESTAMP)
     */
    private LocalDateTime updateTime;
}
