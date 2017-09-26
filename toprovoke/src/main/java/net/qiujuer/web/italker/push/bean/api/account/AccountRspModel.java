package net.qiujuer.web.italker.push.bean.api.account;

import com.google.gson.annotations.Expose;
import net.qiujuer.web.italker.push.bean.card.UserCard;
import net.qiujuer.web.italker.push.bean.db.User;

/**
 * Created by Administrator on 2017/8/10.
 */
public class AccountRspModel {
    // 用户的卡片信息
    @Expose
    private UserCard user;
    // 用户的账户信息
    @Expose
    private String account;
    // 登录状态信息
    // 可以根据Token获取用户所有的信息
    @Expose
    private String token;
    // 判断是否绑定
    @Expose
    private boolean isBind;

    public AccountRspModel(User user){
        this(user,false);
    }

    public AccountRspModel(User user,boolean isbind){
        this.user = new UserCard(user);
        this.account = user.getPhone();
        this.token = user.getToken();
        this.isBind = isbind;
    }

    public UserCard getUser() {
        return user;
    }

    public void setUser(UserCard user) {
        this.user = user;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isBind() {
        return isBind;
    }

    public void setBind(boolean bind) {
        isBind = bind;
    }
}
