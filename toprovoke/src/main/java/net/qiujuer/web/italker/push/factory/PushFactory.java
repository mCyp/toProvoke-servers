package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.base.PushModel;
import net.qiujuer.web.italker.push.bean.api.message.MessageCreateModel;
import net.qiujuer.web.italker.push.bean.card.GroupMemberCard;
import net.qiujuer.web.italker.push.bean.card.MessageCard;
import net.qiujuer.web.italker.push.bean.card.UserCard;
import net.qiujuer.web.italker.push.bean.db.*;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.PushDispatcher;
import net.qiujuer.web.italker.push.utils.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息推送的辅助工具类
 * Created by Administrator on 2017/8/29.
 */
public class PushFactory {
    // 发送一条消息给发送者 并存储历史记录
    public static void pushNewMessage(User sender, Message message) {
        if(sender == null || message == null)
            return;

        // 消息卡片用于发送
        MessageCard card = new MessageCard(message);
        // 要推送的字符串
        String entity = TextUtil.toJson(card);

        // 发送者
        PushDispatcher dispatcher = new PushDispatcher();

        if(message.getGroup() == null && Strings.isNullOrEmpty(message.getGroupId())){
            // 给人发送信息
            User receiver = UserFactory.findById(message.getReceiveId());
            // 判断接受者是否为空
            if(receiver == null)
                return;
            // 历史消息的记录
            PushHistory history = new PushHistory();
            // 设置为普通消息类型
            history.setEntityType(PushModel.ENTITY_TYPE_MESSAGE);
            history.setEntity(entity);
            history.setReceiver(receiver);
            // 接受者当前设备的推送id
            history.setReceiverPushId(receiver.getPushId());

            // 推送的真实Model
            PushModel pushModel = new PushModel();
            // 每一条历史记录都是独立的 可以单独发送
            pushModel.add(history.getEntityType(),history.getEntity());

            // 发送者进行真实的提交
            dispatcher.add(receiver,pushModel);
            // 将历史消息进行吧保存
            Hib.queryOnly(session -> session.save(history));
        }else {
            // 给群发送信息
            Group group = message.getGroup();
            // 因为延迟加载，可能为null ,需要通过id查询
            if(group == null){
                group = GroupFactory.findById(message.getGroupId());
            }

            // 如果群还是没有的情况下 就返回
            if(group == null)
                return;

            Set<GroupMember> members = GroupFactory.getMembers(group);
            // 群成员为空 就返回
            if(members == null || members.size() == 0)
                return;

            // 过滤我自己
            members.stream()
                    .filter(groupMember -> !groupMember.getUserId().equalsIgnoreCase(sender.getId()))
                    .collect(Collectors.toSet());

            // 可能群里只有我自己
            if(members.size() == 0)
                return;

            // 给群里所有人都发送消息记录的集合
            List<PushHistory> histories = new ArrayList<>();

            addGroupMembersPushModel(dispatcher, // 推送的发送者
                    histories,  // 数据要存储的列表
                    members,    // 数据要发送的成员
                    entity, // 要发送的数据
                    PushModel.ENTITY_TYPE_MESSAGE); //消息类型

            // 保存消息的历史记录
            Hib.queryOnly(session -> {
                for (PushHistory history : histories) {
                    session.saveOrUpdate(history);
                }
            });

        }

        // 发送者进行真实的提交
        dispatcher.submit();
    }

    /**
     * 给群成员构建一个消息
     * 把消息存储到数据库的历史记录中，每一个人，每条消息都是一个记录
     */
    private static void addGroupMembersPushModel(PushDispatcher dispatcher,
                                                 List<PushHistory> histories,
                                                 Set<GroupMember> members,
                                                 String entity,
                                                 int entityTypeMessage) {
        for (GroupMember member : members) {
            User receiver = member.getUser();
            // 因为是急加载 如果接受不到user 就可能出现错误
            if(receiver == null)
                return;

            // 历史消息的记录
            PushHistory history = new PushHistory();
            // 设置为普通消息类型
            history.setEntityType(entityTypeMessage);
            history.setEntity(entity);
            history.setReceiver(receiver);
            // 接受者当前设备的推送id
            history.setReceiverPushId(receiver.getPushId());
            histories.add(history);

            // 构建一个消息model
            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(),history.getEntity());

            // 添加到发送者的数据集中
            dispatcher.add(receiver,pushModel);
        }
    }

    /**
     * 给新加入的成员发送你已经被添加进什么群
     * @param members 新成员
     */
    public static void pushJoinGroup(Set<GroupMember> members) {
        // 发送者
        PushDispatcher dispatcher = new PushDispatcher();
        // 一个历史记录列表
        List<PushHistory> histories = new ArrayList<>();

        for (GroupMember member : members) {
            User receiver = member.getUser();
            // 因为是急加载 如果接受不到user 就可能出现错误
            if(receiver == null)
                return;

            // 每个成员的信息卡片
            GroupMemberCard memberCard = new GroupMemberCard(member);
            String entity = TextUtil.toJson(memberCard);

            // 历史消息的记录
            PushHistory history = new PushHistory();
            // 设置为你被添加入群的消息
            history.setEntityType(PushModel.ENTITY_TYPE_ADD_GROUP);
            history.setEntity(entity);
            history.setReceiver(receiver);
            // 接受者当前设备的推送id
            history.setReceiverPushId(receiver.getPushId());
            histories.add(history);

            // 构建一个消息model
            PushModel pushModel = new PushModel()
            .add(history.getEntityType(),history.getEntity());

            // 添加到发送者的数据集中
            dispatcher.add(receiver,pushModel);
            histories.add(history);
        }

        // 保存消息的历史记录
        Hib.queryOnly(session -> {
            for (PushHistory history : histories) {
                session.saveOrUpdate(history);
            }
        });

        // 提交发送
        dispatcher.submit();
    }

    /**
     * 给群里的老成员发送成员新增信息
     * @param oldMembers 老成员
     * @param insertCards 成员新增
     */
    public static void pushGroupMemberAdd(Set<GroupMember> oldMembers, List<GroupMemberCard> insertCards) {
        // 发送者
        PushDispatcher dispatcher = new PushDispatcher();
        // 一个历史记录列表
        List<PushHistory> histories = new ArrayList<>();

        // 当前新增的用户集合的Json字符串
        String entity = TextUtil.toJson(insertCards);

        // 进行循环添加 给每一老的用户构建一个消息, 消息的内容为新增的用户的信息
        // 通知的类型为成员添加了的类型
        addGroupMembersPushModel(dispatcher, histories, oldMembers,
                entity, PushModel.ENTITY_TYPE_ADD_GROUP_MEMBERS);

        // 保存消息的历史记录
        Hib.queryOnly(session -> {
            for (PushHistory history : histories) {
                session.saveOrUpdate(history);
            }
        });

        // 提交发送
        dispatcher.submit();
    }

    public static void pushApplyGroupAdmins(List<GroupMember> admins) {
        // TODO 给群里的管理员发送申请信息
    }

    /**
     * 给退出的设备推送您的账号在新的设备登录
     * @param receiver 接收者
     * @param pushId 这个时刻推送的设备id
     */
    public static void pushLogOut(User receiver, String pushId) {
        // 历史消息的记录
        PushHistory history = new PushHistory();
        // 设置为你被添加入群的消息
        history.setEntityType(PushModel.ENTITY_TYPE_ADD_GROUP);
        history.setEntity("Account logout!!!");
        history.setReceiver(receiver);
        // 接受者当前设备的推送id
        history.setReceiverPushId(pushId);

        // 数据库保存操作
        Hib.queryOnly(session -> {
            session.save(history);
        });

        // 发送者
        PushDispatcher dispatcher = new PushDispatcher();

        PushModel pushModel = new PushModel()
                .add(history.getEntityType(),history.getEntity());
        dispatcher.add(receiver,pushModel);
        // 推送的提交
        dispatcher.submit();


    }

    /**
     * 给我关注的人发送一条我的信息
     * @param receiver 我关注得人
     * @param userCard 我的信息
     */
    public static void pushFollow(User receiver, UserCard userCard) {
        // 一定是相互关注额了
        userCard.setFollow(true);
        String entity = TextUtil.toJson(userCard);

        // 历史消息的记录
        PushHistory history = new PushHistory();
        // 设置为你被添加入群的消息
        history.setEntityType(PushModel.ENTITY_TYPE_ADD_FRIEND);
        history.setEntity(entity);
        history.setReceiver(receiver);

        history.setReceiverPushId(receiver.getPushId());
        PushDispatcher dispatcher = new PushDispatcher();

        PushModel pushModel = new PushModel()
                .add(history.getEntityType(),history.getEntity());
        dispatcher.add(receiver,pushModel);
        dispatcher.submit();

    }
}
