package net.qiujuer.web.italker.push.service;

import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.api.message.MessageCreateModel;
import net.qiujuer.web.italker.push.bean.card.MessageCard;
import net.qiujuer.web.italker.push.bean.db.Group;
import net.qiujuer.web.italker.push.bean.db.Message;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.GroupFactory;
import net.qiujuer.web.italker.push.factory.MessageFactory;
import net.qiujuer.web.italker.push.factory.PushFactory;
import net.qiujuer.web.italker.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
/**
 * 消息发送的入口
 * Created by Administrator on 2017/8/29.
 */
@Path("/msg")
public class MessageService extends BaseService{
    // 发送一条消息到服务器
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<MessageCard> pushMessage(MessageCreateModel model) {
        if(!MessageCreateModel.check(model))
            return ResponseModel.buildParameterError();

        User self = getSelf();

        // 首先判断是否重复请求
        Message message = MessageFactory.findById(model.getId());
        if(message != null){
            return ResponseModel.buildOk(new MessageCard(message));
        }
        if(model.getReceiveType() == Message.RECEIVER_TYPE_NONE){
            return pushToUser(self,model);
        }else {
            return pushToGroup(self,model);
        }
    }

    // 发送到人
    private ResponseModel<MessageCard> pushToUser(User sender, MessageCreateModel model) {
        User receiver = UserFactory.findById(model.getReceiveId());
        if(receiver == null){
            return ResponseModel.buildNotFoundUserError("Can't find receiver user");
        }
        if(receiver.getId().equalsIgnoreCase(sender.getId())){
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
        }

        Message message = MessageFactory.add(sender,receiver,model);
        // 通知发送者
        return buildAndPushResponse(sender,message);
    }

    // 发送到群
    private ResponseModel<MessageCard> pushToGroup(User sender, MessageCreateModel model) {
        // 找群是具有权限性质的 前提是你必须在这个群里 才可以发送信息
        Group group = GroupFactory.findById(sender,model.getReceiveId());
        if(group == null){
            // 有可能你不是群的成员
            return ResponseModel.buildNotFoundUserError("Can't find receiver group");
        }
        // 添加到数据库
        Message message = MessageFactory.add(sender,group,model);
        // 走通用的推送逻辑
        return buildAndPushResponse(sender,message);
    }

    // 推送并构建一个返回
    private ResponseModel<MessageCard> buildAndPushResponse(User sender, Message message) {
        if(message == null){
            // 存储数据库失败
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
        }
        // 进行推送
        PushFactory.pushNewMessage(sender,message);
        // 返回
        return ResponseModel.buildOk(new MessageCard(message));
    }

}
