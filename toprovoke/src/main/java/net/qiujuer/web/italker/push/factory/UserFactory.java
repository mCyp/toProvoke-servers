package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.bean.db.UserFollow;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.TextUtil;
import org.hibernate.Session;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Administrator on 2017/8/9.
 */
public class UserFactory {

    /**
     * 通过taken 查找用户信息
     * 只能用户查找自己的信息，不能获取其他人的信息
     * @param token
     * @return
     */
    public static User findByToken(String token){
        return Hib.query(session -> (User) session.createQuery("from User where token =:token")
                .setParameter("token",token)
                .uniqueResult());
    }

    /**
     * 通过账号查询用户
     * @param phone
     * @return
     */
    public static User findByPhone(String phone){
        return Hib.query(session -> (User) session.createQuery("from User where phone =:inphone")
                        .setParameter("inphone",phone)
                         .uniqueResult());
    }

    /**
     * 通过用户名查找用户
     * @param name
     * @return
     */
    public static User findByName(String name){
        return Hib.query(session -> (User) session.createQuery("from User where name =:inname")
                .setParameter("inname",name)
                .uniqueResult());
    }

    /**
     * 通过用户id查找用户
     * @param id
     * @return
     */
    public static User findById(String id){
        // 通过主键id 查询 更加方便
        return Hib.query(session ->session.get(User.class,id));
    }

    /**
     * 更新信息到数据库
     * @param user
     * @return
     */
    public static User update(User user){
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;
        });
    }

    public static User bindUser(User user,String pushId){
        // 第一步 查询该pushID是否被其他用户登陆
        // 取消绑定 并推送一条消息
        // 更新自己的状态信息
        if(Strings.isNullOrEmpty(pushId))
            return user;

        Hib.queryOnly(session -> {

            @SuppressWarnings("unchecked")
            List<User> userList = (List<User>)session
                    .createQuery("from User where lower(pushId) =:pushId and id!=:userId")
                    .setParameter("pushId",pushId.toLowerCase())
                    .setParameter("userId",user.getId())
                    .list();

            for(User u:userList){
                u.setPushId(null);
                session.saveOrUpdate(u);
            }
        });

        if(pushId.equals(user.getPushId())){
            // 要绑定的pushId是用户之前绑定过的pushId
            // 不需要再次进行绑定
            return user;
        }else {
            // 如果当前要绑定的与用户已经存在的pushID不一样
            // 那么需要单点登陆，让另外一台设备下线，
            // 设置当前设备
            if(Strings.isNullOrEmpty(user.getPushId())){
                //推送一个消息 让之前设备下线 并通知
                PushFactory.pushLogOut(user,user.getPushId());

            }
            user.setPushId(pushId);

            // 更新新的设备ID
            return update(user);

        }
    }

    /**
     * 登陆操作
     * @param phone 账户
     * @param password 密码
     * @return
     */
    public static User login(String phone,String password){
        // 去除首尾空格
         final  String  accountStr =  phone.trim();
        // 将原文进行加密
        final String  evcodePassword = encodePassword(password);

        User user = Hib.query(session -> {
            return  (User)session.createQuery("from User where phone =:inphone and password =:inpassword")
                    .setParameter("inphone",accountStr)
                    .setParameter("inpassword",evcodePassword)
                    .uniqueResult();
        });

        if(user!=null){
            // 对User进行登陆操作 更新token
            user = login(user);
        }

        return user;
    }

    public static User register(String account,String name,String password){
        // 去除空格
        account = account.trim();
        // 处理密码
        password = encodePassword(password);
        User user = createUser(account,password,name);

        if(user!=null){
            user = login(user);
        }

        return user;

    }

    /**
     * 注册部分的逻辑
     * @param account 手机号
     * @param password 密码
     * @param name 用户名
     * @return
     */
    public static User createUser(String account,String password,String name){
        User user = new User();
        // 手机号码就是account
        user.setPhone(account);
        user.setName(name);
        user.setPassword(password);
        // 数据库的存储
        return Hib.query(session -> {
            session.save(user);
            return user;
        });
    }

    /**
     * 进行一个登陆操作
     * 本质上就是对token的更新
     * @param user User
     * @return User
     */
    public static User login(User user){
        // 通过随机获取的UUID值充当taken
        String newTaken = UUID.randomUUID().toString();
        // 格式化taken
        newTaken = TextUtil.encodeBase64(newTaken);
        user.setToken(newTaken);

        // 更新信息到数据库
        return update(user);
    }


    /**
     * 加密算法MD5
     * @param password
     * @return
     */
    private static String encodePassword(String password){
        // 去除空格
        password = password.trim();
        // md5 非对称加密算法
        password = TextUtil.getMD5(password);
        // encodeBase64对称加密算法 ， 可以加盐 就是加入时间放在数据里，不过保存的时间也要保存
        return TextUtil.encodeBase64(password);
    }

    /**
     * 获取我的联系人的列表
     * @param self User
     * @return List<User>
     */
    public static List<User> contacts(User self){
        // 重新加载一次信息到用户中 原因 self 的查询在事务中 且我关注的人是懒加载 所以事务结束后 数据不会得到保存
        return Hib.query(session -> {
            session.load(self,self.getId());

            // 获取我关注的人
            Set<UserFollow> flows = self.getFollowing();

            return flows.stream()
                    .map(UserFollow::getTarget)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 关注人的操作
     * @param origin 发起人
     * @param target 被关注得人
     * @param alias 备注
     * @return 被关注的人的信息
     */
    public static User follow(final User origin,final User target,final String alias){
        UserFollow follow =getUserFollow(origin,target);
        if(follow != null){
            return follow.getTarget();
        }
        return  Hib.query(session -> {
            // 如果是懒加载 则要进行重新加载
            session.load(origin,origin.getId());
            session.load(target,target.getId());

            // 我关注别人的时候 别人也需要关注我
            // 所以需要两条UserFollow数据
            UserFollow originalFollow = new UserFollow();
            originalFollow.setOrigin(origin);
            originalFollow.setTarget(target);
            originalFollow.setAlias(alias);

            UserFollow targetFollow = new UserFollow();
            targetFollow.setOrigin(target);
            targetFollow.setTarget(origin);

            // 保存数据库
            session.save(originalFollow);
            session.save(targetFollow);

            return target;
        });
    }

    /**
     * 查询连个人的关注状态
     * @param origin 发起者
     * @param target 被关注人
     * @return 中间类 UserFollow
     */
    public static UserFollow getUserFollow(final User origin,final User target){
        return Hib.query(session -> (UserFollow) session.createQuery("from UserFollow where originId =:originId and targetId =:targetId")
                .setParameter("originId",origin.getId())
                .setParameter("targetId",target.getId())
                // 查询一条数据
                .setMaxResults(1)
                .uniqueResult());
    }

    /**
     * 搜索用户通过姓名
     * @param name 姓名
     * @return List<User>
     */
    @SuppressWarnings("unchecked")
    public static List<User> search(String name) {
        // 判断是否为空
        if(Strings.isNullOrEmpty(name))
            name = "";// 模糊匹配
        final String searchName = "%"+name+"%";

        return Hib.query(session -> (List<User>)session.createQuery("from User where lower(name)  like :name and portrait is not null and description is not null ")
                .setParameter("name",searchName)
                .setMaxResults(20) // 最多选择20条记录
                .list());
    }
}
