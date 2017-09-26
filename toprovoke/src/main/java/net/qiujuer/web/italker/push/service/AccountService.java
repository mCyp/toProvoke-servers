package net.qiujuer.web.italker.push.service;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.account.AccountRspModel;
import net.qiujuer.web.italker.push.bean.api.account.LoginModel;
import net.qiujuer.web.italker.push.bean.api.account.RegisterMode;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by Administrator on 2017/7/19.
 */
//路径是127.0.0.1/api/account/...
@Path("/account")
public class AccountService extends BaseService {
    //登陆
    @POST
    @Path("/login")
    //指定请求与返回的相应体为Json
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel login(LoginModel model){

        if(!LoginModel.check(model)){
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.login(model.getAccount(),model.getPassword());
        if(user!=null){

            if(!Strings.isNullOrEmpty(model.getPushId())){
                return bind(user,model.getPushId());
            }
            // 返回当前的账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        }else{
            // token 失效 账户绑定异常
            return ResponseModel.buildAccountError();
        }
    }

    // 注册
    @POST
    @Path("/register")
    //指定请求与返回的相应体为Json
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel register(RegisterMode model){
        if(!RegisterMode.check(model)){
            return ResponseModel.buildParameterError();
        }
        // 检查是否存在相同的账号
        User user = UserFactory.findByPhone(model.getAccount().trim());
        if(user!=null){
            // 返回 已有账号
            return ResponseModel.buildHaveAccountError();
        }

        // 检查是否已有姓名的错误
        user = UserFactory.findByName(model.getName().trim());
        if(user!=null){

            if(!Strings.isNullOrEmpty(model.getPushId())){
                return bind(user,model.getPushId());
            }

            // 返回 已有姓名
            return ResponseModel.buildHaveNameError();
        }

        // 注册的逻辑处理
        user = UserFactory.register(
                model.getAccount(),
                model.getName(),
                model.getPassword()
        );

        if(user!=null){
            // 返回当前的账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        }else{
            // 注册异常
            return ResponseModel.buildRegisterError();
        }

    }

    // 绑定
    @POST
    @Path("/bind/{pushId}")
    //指定请求与返回的相应体为Json
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // 请求头中获取taken字段
    //  pushId 从uri中获取
    public ResponseModel bind(@HeaderParam("token") String token,
                              @PathParam("pushId")String pushId){
        if(Strings.isNullOrEmpty(token)||Strings.isNullOrEmpty(pushId)){
            // 返回参数异常错误
            return ResponseModel.buildParameterError();
        }

        //User user = UserFactory.findByToken(taken);
        User self = getSelf();
        if(self!=null){
           return bind(self,pushId);
        }else{
            return ResponseModel.buildLoginError();
        }
    }

    /**
     * 绑定操作
     * @param self 用户
     * @param pushId
     * @return User
     */
    private ResponseModel<AccountRspModel> bind(User self,String pushId){
        User user = UserFactory.bindUser(self,pushId);
        if(user == null){
            // 没有绑定好pushId 则是服务器异常
            return ResponseModel.buildServiceError();
        }

        // 返回当前账户
        // 返回当前的账户
        AccountRspModel rspModel = new AccountRspModel(user,true);
        return ResponseModel.buildOk(rspModel);
    }



}
