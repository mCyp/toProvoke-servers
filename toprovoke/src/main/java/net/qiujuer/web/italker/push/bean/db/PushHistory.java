package net.qiujuer.web.italker.push.bean.db;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Created by Administrator on 2017/7/29.
 */
@Entity
@Table( name = "TB_PUSH_HISTORY")
public class PushHistory {
    //这是一个主键
    @Id
    @PrimaryKeyJoinColumn
    //主键生成的存储类型为UUID
    @GeneratedValue(generator = "uuid")
    //把uuid的生成器定义为uuid2,uuid2是常规的uuid tostring
    @GenericGenerator(name = "uuid" ,strategy = "uuid2")
    //不允许更改，不允许为null
    @Column(updatable = false,nullable = false)
    private String id;

    // 推送的具体实体存储都是JSON字符串
    // Blob是大字段类型
    @Lob
    @Column(nullable = false,columnDefinition = "BLOB")
    private String entity;

    // 推送的实体类型
    @Column(nullable = false)
    private int entityType;

    //接收者
    //接收者不允许为空
    //一个接收者可以接受很多消息
    //FetchType.EAGER,：加载一条推送信息的时候，加载用户信息
    @ManyToOne(optional = false,fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "receiverId")//默认是receiverId
    private User receiver;
    @Column(nullable = false,updatable = false,insertable = false)
    private String receiverId;

    //发送者
    //发送者可以为空 可能是系统消息
    //一个发送者可以发送很多消息
    //FetchType.EAGER,：加载一条推送信息的时候，加载用户信息
    @ManyToOne(fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "senderId")
    private User sender;
    @Column(updatable = false,insertable = false)
    private String senderId;

    //接收者当前状态下的设备推送Id
    //User.PushId可以为NULL
    @Column
    private String receiverPushId;

    //定义为创建时间戳,在创建的时候就已经写入
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createAt = LocalDateTime.now();

    //定义为更新时间戳,在创建的时候就已经写入
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt = LocalDateTime.now();

    //消息送达时间可以为空
    @Column
    private LocalDateTime arriveAt = LocalDateTime.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public int getEntityType() {
        return entityType;
    }

    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverPushId() {
        return receiverPushId;
    }

    public void setReceiverPushId(String receiverPushId) {
        this.receiverPushId = receiverPushId;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }

    public LocalDateTime getArriveAt() {
        return arriveAt;
    }

    public void setArriveAt(LocalDateTime arriveAt) {
        this.arriveAt = arriveAt;
    }
}
