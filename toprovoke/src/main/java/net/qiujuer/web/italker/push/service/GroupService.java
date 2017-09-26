package net.qiujuer.web.italker.push.service;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.api.group.GroupApplyModel;
import net.qiujuer.web.italker.push.bean.api.group.GroupCreateModel;
import net.qiujuer.web.italker.push.bean.api.group.GroupMemberAddModel;
import net.qiujuer.web.italker.push.bean.api.group.GroupMemberUpdateModel;
import net.qiujuer.web.italker.push.bean.card.ApplyCard;
import net.qiujuer.web.italker.push.bean.card.GroupCard;
import net.qiujuer.web.italker.push.bean.card.GroupMemberCard;
import net.qiujuer.web.italker.push.bean.db.Apply;
import net.qiujuer.web.italker.push.bean.db.Group;
import net.qiujuer.web.italker.push.bean.db.GroupMember;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.ApplyFactory;
import net.qiujuer.web.italker.push.factory.GroupFactory;
import net.qiujuer.web.italker.push.factory.PushFactory;
import net.qiujuer.web.italker.push.factory.UserFactory;
import net.qiujuer.web.italker.push.provider.LocalDateTimeConverter;

import javax.jws.soap.SOAPBinding;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群组接口的入口
 * Created by Administrator on 2017/9/4.
 */
@Path("/group")
public class GroupService extends BaseService {
    // 创建群
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> create(GroupCreateModel model){
        if(!GroupCreateModel.check(model)){
            // 如果检查没有通过 就返回一个参数错误
            return ResponseModel.buildParameterError();
        }

        User creator = getSelf();
        // 移除用户自己的信息之外
        model.getUsers().remove(creator.getId());
        if(model.getUsers() .size() == 0){
            // 说明用户群里只有当前用户一个人 所以不符合创建标准
            return ResponseModel.buildParameterError();
        }

        if(GroupFactory.findByName(model.getName()) != null){
            // 如果所有群组里面已经有一个同名的群组 就返回一个已经有当前群组名的错误
            return ResponseModel.buildHaveNameError();
        }

        // 群用户列表
        List<User> users = new ArrayList<>();
        for (String s : model.getUsers()) {
           User user = UserFactory.findById(s);
           users.add(user);
        }
        if(users.size() == 0){
            // 群model里面放的用户id对应不了用户 所以还是参数错误
            return ResponseModel.buildParameterError();
        }

        Group group = GroupFactory.create(creator,model,users);
        if(group == null){
            // 服务器异常 创建不了群
            return ResponseModel.buildServiceError();
        }

        // 拿管理员的信息 就是自己的信息
        GroupMember creatorMember = GroupFactory.getMember(creator.getId(),group.getId());

        if(creatorMember == null){
            // 如果我自己的信息都没有拿到 那么还是服务器异常
            return ResponseModel.buildServiceError();
        }

        // 获取所有人的信息 并给他们发通知信息
        Set<GroupMember> members = GroupFactory.getMembers(group);
        if(members.size() == 0){
            // 服务器异常
            return ResponseModel.buildServiceError();
        }
        members = members.stream()
                .filter(groupMember -> {
                    return !groupMember.getId().equalsIgnoreCase(creator.getId());
                }).collect(Collectors.toSet());

        // 给所有人发送添加的信息
        // 开始发起推送
        PushFactory.pushJoinGroup(members);
        return ResponseModel.buildOk(new GroupCard(creatorMember));
    }

    // 搜索群
    @GET
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> search(@PathParam("name")@DefaultValue("")String name){
        User self = getSelf();
        List<Group> groups = GroupFactory.search(name);
        if(groups != null && groups.size() != 0){
            List<GroupCard> groupCards = groups.stream()
                    .map(group -> {
                        GroupMember groupMember = GroupFactory.getMember(self.getId(),group.getId());
                        return new GroupCard(group,groupMember);
                    }).collect(Collectors.toList());
            return ResponseModel.buildOk(groupCards);
        }
        return ResponseModel.buildOk();
    }

    // 获取群列表 有时间就返回这个时间之后加入的群的列表
    @GET
    @Path("/list/{date:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> lis(@DefaultValue("")@PathParam("date")String dateStr){
        User self = getSelf();
        LocalDateTime dateTime = null;

        if(Strings.isNullOrEmpty(dateStr)){
            try {
                dateTime = LocalDateTime.parse(dateStr, LocalDateTimeConverter.FORMATTER);
            }catch (Exception e){
                dateTime = null;
            }
        }

        Set<GroupMember> members = GroupFactory.getMembers(self);
        if(members == null || members.size() == 0)
            return ResponseModel.buildOk();

        // 进行时间的过滤
        final LocalDateTime finalDateTime = dateTime;
        List<GroupCard> groupCards = members.stream()
                .filter(groupMember -> (finalDateTime == null) // 时间如果为null的情况下则不做限制
                        || (groupMember.getUpdateAt().isAfter(finalDateTime))) // 时间不为null的情况下你要在这个时间之后的群才能被获取
                .map(GroupCard::new)
                .collect(Collectors.toList());

        return ResponseModel.buildOk(groupCards);
    }

    // 获取一个群组信息 你必须是这个群的成员
    @GET
    @Path("/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> getGroup(@PathParam("groupId")String groupId){
        if(Strings.isNullOrEmpty(groupId))
            // 返回参数错误
            return ResponseModel.buildParameterError();

        User self = getSelf();
        GroupMember member = GroupFactory.getMember(self.getId(),groupId);
        if(member == null){
            // 没有找到这个群
            return ResponseModel.buildNotFoundGroupError(null);
        }
        return ResponseModel.buildOk(new GroupCard(member));
    }

    // 获取群成员
    @GET
    @Path("/{groupId}/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupMemberCard>> members(@PathParam("groupId") String groupId){
        User self = getSelf();
        // 查找群是否存在
        Group group = GroupFactory.findById(groupId);
        if(group == null){
            // 群不存在的情况下返回参数错误
            return  ResponseModel.buildNotFoundGroupError(null);
        }
        GroupMember selfMember = GroupFactory.getMember(self.getId(),groupId);
        if(selfMember == null){
            // 返回没有查到群
            return ResponseModel.buildNoPermissionError();
        }
        Set<GroupMember> members = GroupFactory.getMembers(group);
        if(members == null){
            // 返回服务器异常
            return ResponseModel.buildServiceError();
        }

        List<GroupMemberCard> memberCards = members.stream()
                .map(GroupMemberCard::new)
                .collect(Collectors.toList());

        return ResponseModel.buildOk(memberCards);
    }

    // 添加一个成员进你已经建好的群
    @POST
    @Path("/{groupId}/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupMemberCard>> addMember(@PathParam("groupId") String groupId, GroupMemberAddModel model){
        // 没有检查通过就返回参数错误
        if(Strings.isNullOrEmpty(groupId)||!GroupMemberAddModel.check(model))
            return ResponseModel.buildParameterError();

        Group group = GroupFactory.findById(groupId);
        if(group == null){
            // 群不存在的情况下返回参数错误
            return  ResponseModel.buildNotFoundGroupError(null);
        }

        User self = getSelf();
        // 校验群里是否只有自己
        model.getUsers().remove(self);
        if(model.getUsers().size() == 0)
            return ResponseModel.buildParameterError();

        // 校验权限
        GroupMember selfMember = GroupFactory.getMember(self.getId(),groupId);
        // 我必须是管理员或者及其以上的级别
        if(selfMember == null || selfMember.getPermissionType() == GroupMember.PERMISSION_TYPE_NONE)
            return ResponseModel.buildNoPermissionError();
        // 得到以前群里面的成员
        Set<GroupMember> oldMembers = GroupFactory.getMembers(group);
        Set<String> oldMembersId = oldMembers.stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toSet());;

        // 群用户列表
        List<User> insertUsers = new ArrayList<>();
        for (String s : model.getUsers()) {
            User user = UserFactory.findById(s);
            // 如果用户为空
            if(user == null)
                continue;

            // 群列表当中已经有该用户
            if(oldMembersId.contains(user.getId()))
                continue;

            insertUsers.add(user);
        }

        // 没有一个新增的成员
        if(insertUsers.size() == 0)
            return ResponseModel.buildParameterError();

        Set<GroupMember> insertMembers = GroupFactory.addMembers(group,insertUsers);
        // 如果没有就代表服务器错误
        if(insertMembers == null)
            return ResponseModel.buildServiceError();

        List<GroupMemberCard> insertCards = insertMembers.stream()
                .map(GroupMemberCard::new)
                .collect(Collectors.toList());

        // 通知两部曲
        // 通知新用户 你加入了xxx群
        PushFactory.pushJoinGroup(insertMembers);

        // 通知老用户 xxx用户加入了xxx群
        PushFactory.pushGroupMemberAdd(oldMembers,insertCards);

        return ResponseModel.buildOk(insertCards);
    }

    // 如果你是群的管理员 可以修改成员的备注名的信息
    // 修改成员信息
    @PUT
    @Path("/member/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupMemberCard> modifyMember(@PathParam("memberId") String memberId, GroupMemberUpdateModel model){
        if(Strings.isNullOrEmpty(memberId) || model == null)
            return ResponseModel.buildParameterError();
        User self = getSelf();
        GroupMember member = GroupFactory.findGroupMemberById(memberId);
        // 如果member找不到 就返回参数异常
        if(member == null)
            return ResponseModel.buildParameterError();
        // 获得自己在这个群中的成员
        GroupMember selfMember = GroupFactory.getMember(self.getId(),member.getGroupId());
        // 如果是修改自己的群名片或者你是这个群管理员
        if(selfMember.getPermissionType()!= GroupMember.PERMISSION_TYPE_NONE
                || selfMember.getId().equals(memberId)){
            GroupMemberCard card = new GroupMemberCard(member);
            card.setAlias(model.getAlias());
            return ResponseModel.buildOk(card);
        }
        // 没有权限操作
        return ResponseModel.buildNoPermissionError();
    }

    // 加入一个群
    @POST
    @Path("/apply/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<ApplyCard> join(@PathParam("groupId") String groupId,GroupApplyModel model){
        // groupId为空或者GroupApplyModel为null 或者 检查不通过都返回参数错误
        if(Strings.isNullOrEmpty(groupId) || model == null || GroupApplyModel.check(model))
            return ResponseModel.buildParameterError();

        // 查找群
        Group group = GroupFactory.findById(groupId);
        if(group == null)
            return ResponseModel.buildNotFoundGroupError(null);

        User self = getSelf();
        GroupMember member = GroupFactory.getMember(self.getId(),groupId);
        // 已经是群成员无需添加
        if(member != null)
            return ResponseModel.buildOk();

        Apply apply = ApplyFactory.createGroupApply(self.getId(),groupId,model);

        // 如果没有创建成功 就返回服务器错误
        if(apply == null)
            return ResponseModel.buildServiceError();

        ApplyCard applyCard = new ApplyCard(apply);

        // 通知群的管理员
        List<GroupMember> admins = GroupFactory.getAddmins(groupId);
        //noinspection ConstantConditions
        if(admins.size() == 0 || admins == null)
            return ResponseModel.buildServiceError();

        // 发送通知给管理员
        PushFactory.pushApplyGroupAdmins(admins);
        return ResponseModel.buildOk(applyCard);
    }






}
