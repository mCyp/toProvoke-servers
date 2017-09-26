package net.qiujuer.web.italker.push.provider;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.UserFactory;
import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

/**
 * 用于所有的接口请求和过滤拦截
 * Created by Administrator on 2017/8/12.
 */
public class AuthRequestFilter implements ContainerRequestFilter{

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String relationPath = ((ContainerRequest)requestContext).getPath(false);
        if(relationPath.startsWith("account/register")||relationPath.startsWith("account/login")){
            // 直接走正常逻辑 不拦截
            return;
        }

        // 获取token
        String token = requestContext.getHeaders().getFirst("token");

        if(!Strings.isNullOrEmpty(token)){
            final User self = UserFactory.findByToken(token);

            if(self != null){
                // 给当前请求设置上下文
                requestContext.setSecurityContext(new SecurityContext() {
                    // 主体部分
                    @Override
                    public Principal getUserPrincipal() {
                        // 用户实现Principal接口
                        return self;
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        // 可以管理用户权限
                        // 比如说授予管理员的管理权限
                        return true;
                    }

                    @Override
                    public boolean isSecure() {
                        // 默认false https
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {

                        return null;
                    }
                });
                return;
            }

        }

        // 否则的话就显示账户异常
        ResponseModel model = ResponseModel.buildAccountError();
        // 构建一个返回
        Response response = Response.status(Response.Status.OK)
                                .entity(model)
                                .build();
        // 拦截 停止一个请求的继续下发 调用该方法后 返回请求
        // 在进入Service之前就停止了
        requestContext.abortWith(response);

    }
}
