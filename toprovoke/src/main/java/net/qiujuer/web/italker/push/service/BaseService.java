package net.qiujuer.web.italker.push.service;

import net.qiujuer.web.italker.push.bean.db.User;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * Created by Administrator on 2017/8/12.
 */
public class BaseService {
    // 添加上下文的一个注解 该注解存在的时候 上下文不可能为空
    // 具体的值为拦截器返回的securityContext
    @Context
    protected SecurityContext securityContext;

    /**
     * 从上下文中获取自己
     * @return User
     */
    protected User getSelf(){
        return (User) securityContext.getUserPrincipal();
    }
}
