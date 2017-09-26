package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.group.GroupCreateModel;
import net.qiujuer.web.italker.push.bean.db.Group;
import net.qiujuer.web.italker.push.bean.db.GroupMember;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.utils.Hib;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群组的工具类
 * Created by Administrator on 2017/8/30.
 */
public class GroupFactory {
    // 通过id拿群model
    @SuppressWarnings("WeakerAccess")
    public static Group findById(String groupId) {
        return Hib.query(session -> session.get(Group.class,groupId));
    }

    // 通过id拿GroupMember
    public static GroupMember findGroupMemberById(String memberId) {
        return Hib.query(session -> session.get(GroupMember.class,memberId));
    }

    // 查询一个群 同时该用户是群成员
    public static Group findById(User user, String groupId) {
        GroupMember member = getMember(user.getId(),groupId);
        if(member != null){
            return member.getGroup();
        }
        return null;
    }

    // 通过姓名查找群
    public static Group findByName(String name) {
        return Hib.query(session -> (Group) session.createQuery("from Group where lower(name)  =:name")
                    .setParameter("name",name.toLowerCase())
                    .uniqueResult());
    }

    // 通过群拿群成员
    @SuppressWarnings("unchecked")
    public static Set<GroupMember> getMembers(Group group) {
        return Hib.query(session -> {
            List<GroupMember> members = session.createQuery("from GroupMember where group =:group")
                    .setParameter("group",group)
                    .list();

            return new HashSet<>(members);
        });
    }

    // 通过用户拿到他的所有的群成员
    @SuppressWarnings("unchecked")
    public static Set<GroupMember> getMembers(User user) {
        return Hib.query(session -> {
            List<GroupMember> members = session.createQuery("from GroupMember where userId =:userId")
                    .setParameter("userId",user.getId())
                    .list();

            return new HashSet<>(members);
        });
    }

    public static Group create(User creator, GroupCreateModel model, List<User> users) {
        return Hib.query(session -> {
            // 创建群
            Group group = new Group(creator,model);
            session.save(group);

            GroupMember ownerMember = new GroupMember(creator,group);
            // 给管理员设置超级权限
            ownerMember.setPermissionType(GroupMember.PERMISSION_TYPE_ADMIN_SU);
            // 保存权限操作
            session.save(ownerMember);

            for (User user : users) {
                GroupMember member = new GroupMember(user,group);
                session.save(member);
            }
            return group;
        });
    }

    public static GroupMember getMember(String userId, String groupId) {
        return Hib.query(session -> (GroupMember)session.createQuery("from GroupMember where userId=:userId and groupId=:groupId")
                                    .setParameter("userId",userId)
                                    .setParameter("groupId",groupId)
                                    .setMaxResults(1)
                                    .uniqueResult());
    }

    // 根据一个名字查询群
    @SuppressWarnings("unchecked")
    public static List<Group> search(String name) {
        // 判断是否为空
        if(Strings.isNullOrEmpty(name))
            name = "";// 模糊匹配
        final String searchName = "%"+name+"%";

        return Hib.query(session -> (List<Group>)session.createQuery("from Group where lower(name)  like :name")
                .setParameter("name",searchName)
                .setMaxResults(20) // 最多选择20条记录
                .list());
    }

    /**
     *  添加群成员
     * @param group
     * @param insertUsers
     * @return
     */
    public static Set<GroupMember> addMembers(Group group, List<User> insertUsers) {
        return Hib.query(session -> {
            Set<GroupMember> members = new HashSet<>();
            for (User insertUser : insertUsers) {
                GroupMember member = new GroupMember(insertUser,group);
                // 保存 并添加到数据库
                session.save(member);
                members.add(member);
            }

            // 该方法不合适的原因是每一次遍历消耗都极高
            /*
            for (GroupMember member : members) {
                session.refresh(member);
            }
            */

            return members;
        });
    }

    /**
     * 获取一个群的管理员
     * @param groupId
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<GroupMember> getAddmins(String groupId) {
        return Hib.query(session -> {
            return (List<GroupMember>)session.createQuery("from GroupMember where groupId =:groupId and permissionType !=:permissionType")
                    .setParameter("groupId",groupId)
                    .setParameter("permissionType",GroupMember.PERMISSION_TYPE_NONE)
                    .list();
        });
    }
}
