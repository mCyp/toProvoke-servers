package net.qiujuer.web.italker.push.service;

import com.google.common.base.Strings;
import net.bytebuddy.implementation.bind.annotation.Default;
import net.qiujuer.web.italker.push.bean.api.base.PushModel;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.api.user.UpdateInfoModel;
import net.qiujuer.web.italker.push.bean.card.UserCard;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.PushFactory;
import net.qiujuer.web.italker.push.factory.UserFactory;
import net.qiujuer.web.italker.push.utils.PushDispatcher;

import javax.jws.soap.SOAPBinding;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.FactoryConfigurationError;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户更新信息的接口
 * 返回自己的信息
 * Created by Administrator on 2017/8/12.
 */
@Path("/user")

public class UserService extends BaseService {


    // 更新
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> update(UpdateInfoModel model){
        // 检测是否为空 如果为空 返回参数异常的错误
        if(!UpdateInfoModel.check(model)){
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();
        // 更新自己
        self = model.updateToUser(self);
        // 更新到数据库
        self = UserFactory.update(self);
        UserCard card = new UserCard(self, true);
        return ResponseModel.buildOk(card);
    }

    // 拉去联系人
    @GET
    @Path("/contact")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard>> contact() {
        // 获取到用户自己的信息
        User self = getSelf();

        // 获取到用户关注的信息
        List<User> users = UserFactory.contacts(self);

        // 将user 转置为 userCard
        List<UserCard> userCards = users.stream()
                .map(user -> new UserCard(user,true)).collect(Collectors.toList());

        return ResponseModel.buildOk(userCards);
    }

    // 关注别人
    // 其实是双方同时关注
    @SuppressWarnings("ConstantConditions")
    @PUT // 修改类请求用put
    @Path("/follow/{followId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> follow(@PathParam("followId") String followId) {
        User self = getSelf();
        if(self.getId().equalsIgnoreCase(followId)|| Strings.isNullOrEmpty(followId)){
            return ResponseModel.buildParameterError();
        }

        User followUser = UserFactory.findById(followId);
        if(followUser == null){
            return ResponseModel.buildNotFoundUserError(null);
        }

        followUser = UserFactory.follow(self,followUser,null);
        if(followId == null){
            // 返回一个服务器异常
            return ResponseModel.buildServiceError();
        }

        // 通知我关注得人 我关注了他
        // 给关注得人发送我的信息
        PushFactory.pushFollow(followUser,new UserCard(self));

        // 返回关注人的信息
        return ResponseModel.buildOk(new UserCard(followUser,true));
    }

    // 获取某人的信息
    @GET
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> getUser(@PathParam("id")String id){
        if(Strings.isNullOrEmpty(id)){
            // 用户id 为空
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();
        if(self.getId().equalsIgnoreCase(id)){
            // 返回自己 不必查询数据库
            return ResponseModel.buildOk(new UserCard(self,true));
        }

        User user = UserFactory.findById(id);
        // 用户没有查询到
        if(user == null){
            return ResponseModel.buildNotFoundUserError(null);
        }
        boolean isFollow = UserFactory.getUserFollow(self,user) !=null;
        return ResponseModel.buildOk(new UserCard(user,isFollow));
    }

    // 搜索人的接口实现
    @GET // 搜索人 不涉及数据更改 所以用GET
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard>> search(@DefaultValue("") @PathParam("name") String name) {
        // 获取到用户自己
        User self =getSelf();
        // 获取查询结果
        List<User> users =UserFactory.search(name);

        final List<User> contacts = UserFactory.contacts(self);
        // 将查询得人封装为userCard
        // 判断这些人是否已经是我的好友或者是我自己
        // 如果有则设置好状态
        List<UserCard> userCards = users.stream()
                .map(user -> {
                    boolean isFollow = user.getId().equalsIgnoreCase(self.getId())
                            || contacts.stream()
                            .anyMatch(contactUser->
                                contactUser.getId().equalsIgnoreCase(user.getId())
                            );
                    return new UserCard(user,isFollow);
                }).collect(Collectors.toList());
        return ResponseModel.buildOk(userCards);
    }



}
