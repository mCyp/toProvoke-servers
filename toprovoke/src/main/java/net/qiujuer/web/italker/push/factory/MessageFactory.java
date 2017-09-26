package net.qiujuer.web.italker.push.factory;

import net.qiujuer.web.italker.push.bean.api.message.MessageCreateModel;
import net.qiujuer.web.italker.push.bean.db.Group;
import net.qiujuer.web.italker.push.bean.db.Message;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.utils.Hib;

/**
 * 消息的处理
 * Created by Administrator on 2017/8/29.
 */
public class MessageFactory {

    // 查询某一个消息
    public static Message findById(String id){
        return Hib.query(session -> session.get(Message.class,id));
    }

    // 添加一个跟人聊天的消息
    public static Message add(User sender, User receiver, MessageCreateModel model){
        Message message = new Message(sender,receiver,model);
        return save(message);
    }

    // 添加一个跟群聊天的消息
    public static Message add(User sender, Group group, MessageCreateModel model){
        Message message = new Message(sender,group,model);
        return save(message);
    }

    // 消息的保存操作
    private static Message save(Message message){
        return Hib.query(session -> {
            session.save(message);
            // 刷新数据库
            session.flush();
            // 重新获取最新值
            session.refresh(message);
            return message;
        });
    }
}
