package net.qiujuer.web.italker.push.bean.api.message;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import net.qiujuer.web.italker.push.bean.db.Group;
import net.qiujuer.web.italker.push.bean.db.Message;

/**
 * API 请求的model格式
 * Created by Administrator on 2017/8/29.
 */
public class MessageCreateModel {
    @Expose
    private String id;
    @Expose
    private String content;
    @Expose
    private String attach;
    // 默认为常规的消息类型
    @Expose
    private int type = Message.TYPE_STR;
    @Expose
    private String receiveId;
    // 接受者类型 群、人
    // 默认为发送给人的消息类型
    @Expose
    private int receiveType = Message.RECEIVER_TYPE_NONE;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getReceiveId() {
        return receiveId;
    }

    public void setReceiveId(String receiveId) {
        this.receiveId = receiveId;
    }

    public int getReceiveType() {
        return receiveType;
    }

    public void setReceiveType(int receiveType) {
        this.receiveType = receiveType;
    }

    public static boolean check(MessageCreateModel model) {
        // Model 不允许为null，
        // 并且只需要具有一个及其以上的参数即可
        return model!= null
                &&!(Strings.isNullOrEmpty(model.id)
                ||Strings.isNullOrEmpty(model.content)
                ||Strings.isNullOrEmpty(model.receiveId))

                &&(model.receiveType == Message.RECEIVER_TYPE_NONE
                ||model.receiveType == Message.RECEIVER_TYPE_GROUP)

                &&(model.type == Message.TYPE_STR
                ||model.type == Message.TYPE_AUDIO
                ||model.type == Message.TYPE_FILE
                ||model.type == Message.TYPE_PIC);
    }
}
