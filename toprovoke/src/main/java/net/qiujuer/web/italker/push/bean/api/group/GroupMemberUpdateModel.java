package net.qiujuer.web.italker.push.bean.api.group;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import net.qiujuer.web.italker.push.bean.api.user.UpdateInfoModel;

/**
 * 群用户信息更新model
 * Created by Administrator on 2017/9/4.
 */
public class GroupMemberUpdateModel {
    @Expose
    private String alias;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public static boolean check(GroupMemberUpdateModel model) {
        // Model 不允许为null，
        // 并且只需要具有一个及其以上的参数即可
        return model != null
                && !Strings.isNullOrEmpty(model.alias);
    }
}
