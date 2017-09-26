package net.qiujuer.web.italker.push.bean.api.group;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;

/**
 * 申请群组的model
 * Created by Administrator on 2017/9/6.
 */
public class GroupApplyModel {
    // 申请加群时候的描述
    @Expose
    private String decription;
    // 附件
    @Expose
    private String attach;

    public String getDecription() {
        return decription;
    }

    public void setDecription(String decription) {
        this.decription = decription;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public static boolean check(GroupApplyModel model) {
        // Model 不允许为null，
        // 并且只需要具有一个及其以上的参数即可
        return model != null
                && !Strings.isNullOrEmpty(model.decription);
    }
}

